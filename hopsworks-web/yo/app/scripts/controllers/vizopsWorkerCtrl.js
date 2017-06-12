'use strict';

angular.module('hopsWorksApp')
         .controller('VizopsWorkerCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'VizopsService',

            function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;

                self.appId;
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now; // is the application running now?
                // array of dictionaries: self.executorInfo.entry[executor].value[0: container, 1: hostname, 2: nm vcores]
                self.executorInfo;
                self.nbExecutors;
                // a list of objects holding as a key the hostname and value the number of executors
                self.hostnames = {};

                // UI
                /* It can be a specific host or all, e.g. ['dn0'](one) or ['dn0', 'dn1'](all)
                   It can either be set to hostsList or chosenHostToFilter
                */
                self.hostsToQuery;
                self.hostsList; // List of all the hosts
                self.nbOfHosts; // hostsList.length
                self.chosenHostToFilter; // two way binded with the chosen host to filter

                $scope.optionsPhysicalCPUUsage = vizopsWorkerPhysicalCpuOptions();
                $scope.optionsMemoryUsage = vizopsWorkerMemoryUsageOptions();
                $scope.optionsNetworkTraffic = vizopsWorkerNetworkTrafficOptions();

                $scope.templatePhysicalCPUUsage = [];
                $scope.templateMemoryUsage = [];
                $scope.templateNetworkTraffic = [];

                self.startTimeMap = {
                    'physicalCpuUsage': -1,
                    'ramUsage': -1,
                    'networkUsage': -1
                };

                self.hasLoadedOnce = {
                    'physicalCpuUsage': false,
                    'ramUsage': false,
                    'networkUsage': false
                };

                var updatePCpuUsage = function() {
                    if (!self.now && self.hasLoadedOnce['physicalCpuUsage'])
                        return; // offline mode + we have loaded the information

                    var tags = 'cpu = \'cpu-total\' and ' + _getTimestampLimits('physicalCpuUsage') +
                               ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'mean(usage_user), mean(usage_iowait), mean(usage_idle)', 'cpu',
                                             tags, 'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['physicalCpuUsage'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templatePhysicalCPUUsage[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templatePhysicalCPUUsage[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                    $scope.templatePhysicalCPUUsage[2].values.push({'x': +splitEntry[0], 'y': +splitEntry[3]});
                                }

                                self.hasLoadedOnce['physicalCpuUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching physicalCpuUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateRAMUsage = function() {
                    if (!self.now && self.hasLoadedOnce['ramUsage'])
                        return; // offline mode + we have loaded the information

                    var tags = _getTimestampLimits('ramUsage') + ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'mean(used), mean(available)',
                                             'mem', tags, 'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['ramUsage'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateMemoryUsage[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templateMemoryUsage[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                }

                                self.hasLoadedOnce['ramUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching ramUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateNetworkUsage = function() {
                    if (!self.now && self.hasLoadedOnce['networkUsage'])
                        return; // offline mode + we have loaded the information

                    var tags = _getTimestampLimits('networkUsage') + ' and host =~ /' + self.hostsToQuery.join('|') + '/';

                    VizopsService.getMetrics('telegraf', 'derivative(first(bytes_recv)), derivative(first(bytes_sent))',
                                             'net', tags, 'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['networkUsage'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateNetworkTraffic[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templateNetworkTraffic[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                }

                                self.hasLoadedOnce['networkUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching networkUsage metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updatePCpuUsage();
                    updateRAMUsage();
                    updateNetworkUsage();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                      if (self.startTimeMap.hasOwnProperty(key)) {
                        self.startTimeMap[key] = self.startTime;
                        self.hasLoadedOnce[key] = false;
                      }
                    }

                    $scope.templatePhysicalCPUUsage = vizopsWorkerPhysicalCpuTemplate();
                    $scope.templateMemoryUsage = vizopsWorkerMemoryUsageTemplate();
                    $scope.templateNetworkTraffic = vizopsWorkerNetworkTrafficTemplate();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didn't use groupBy calls then it would be enough to upper limit the time with now()
                    var limits = 'time > ' + self.startTimeMap[graphName] + 'ms';

                    if (!self.now) {
                        limits += ' and time < ' + self.endTime + 'ms';
                    } else {
                        limits += ' and time < now()';
                    }

                    return limits;
                };

                var _extractHostnameInfoFromResponse = function(response) {
                    // get the unique host names
                    var hosts = [...new Set(response.entry.map(item => item.value[1]))];

                    var result = {};
                    for(var i = 0; i < hosts.length; i++) {
                        result[hosts[i]] = [];
                    }

                    // and add the executors running on them
                    for(var i = 0; i < response.entry.length; i++) {
                        result[response.entry[i].value[1]].push(response.entry[i].key);
                    }

                    return result;
                };

                var init = function() {
                    self.appId = VizopsService.getAppId();

                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                        function(success) {
                            var info = success.data;

                            self.nbExecutors = info.nbExecutors;
                            self.executorInfo = info.executorInfo;
                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                            self.hostsList = Object.keys(self.hostnames);
                            self.nbOfHosts = self.hostsList.length;
                            self.hostsToQuery = self.hostsList;

                            resetGraphs();
                            updateMetrics();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                }

                init();

                self.onHostFilterChange = function() {
                    if (self.hostsList.length === 1) { // only one host, filtering doesn't change anything
                        return;
                    } else if (self.chosenHostToFilter === null) { // filter was emptied, reset to original
                        self.hostsToQuery = self.hostsList;
                    } else if (_.isEqual([self.chosenHostToFilter], self.hostsToQuery)) { // same choice, skip
                        return;
                    } else {
                        self.hostsToQuery = [self.chosenHostToFilter];
                    }

                    resetGraphs();
                    updateMetrics();
                };

                self.poller = $interval(function () {
                    //updateMetrics();
                }, vizopsUpdateInterval());

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
            }
        ]
    );