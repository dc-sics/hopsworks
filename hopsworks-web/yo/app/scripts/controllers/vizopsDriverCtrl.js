'use strict';

angular.module('hopsWorksApp')
         .controller('VizopsDriverCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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

                self.nbExecutorsOnDriverHost = 0;
                self.maxUsedDriverMem = "0.0";
                self.maxAvailableDriverMem = "0.0";

                $scope.optionsMemorySpace = vizopsMemorySpaceDriverOptions();
                $scope.optionsVCPU = vizopsVCPUDriverOptions();
                $scope.optionsRDDCacheDiskSpill = vizopsRDDCacheDiskSpillOptions();
                $scope.optionsGCTime = vizopsGCTimeOptions();

                $scope.templateMemorySpace = vizopsMemorySpaceDriverTemplate();
                $scope.templateVCPU = vizopsVCPUDriverTemplate();
                $scope.templateRDDCacheDiskSpill = vizopsRDDCacheDiskSpillTemplate();
                $scope.templateGCTime = vizopsGCTimeTemplate();

                self.startTimeMap = {
                    'vcpuUsage': -1,
                    'memorySpace': -1,
                    'maxMemory': -1,
                    'rddCacheDiskSpill': -1,
                    'gcTime': -1
                };

                self.hasLoadedOnce = {
                    'vcpuUsage': false,
                    'memorySpace': false,
                    'maxMemory': false,
                    'rddCacheDiskSpill': false,
                    'gcTime': false
                };

                var updateMemorySpace = function() {
                    if (!self.now && self.hasLoadedOnce['memorySpace'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('memorySpace')
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'mean(heap_used), max(heap_used)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['memorySpace'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length - 1; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateMemorySpace[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templateMemorySpace[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                }

                                self.hasLoadedOnce['memorySpace'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching memorySpaceDriver metrics.', ttl: 10000});
                        }
                    );
                };

                var updateGraphVCPU = function() {
                    if (!self.now && self.hasLoadedOnce['vcpuUsage'])
                        return; // offline mode + we have loaded the information

                    var tags = 'source =~ /' + self.executorInfo.entry[0].value[0] + '/' + ' and ' + _getTimestampLimits('vcpuUsage')
                               + ' and MilliVcoreUsageIMinMilliVcores <= ' + (+self.executorInfo.entry[0].value[2]*1000);

                    VizopsService.getMetrics('graphite',
                                               'mean(MilliVcoreUsageIMinMilliVcores)/' + (+self.executorInfo.entry[0].value[2]*1000),
                                               'nodemanager', tags, 'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['vcpuUsage'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length - 1; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateVCPU[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                }

                                self.hasLoadedOnce['vcpuUsage'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching VcpuUsageDriver metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMaxMemory = function() {
                    if (!self.now && self.hasLoadedOnce['maxMemory'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and service =~ /driver/';

                    VizopsService.getMetrics('graphite',
                        'max(heap_used), heap_max', 'spark', tags).then(
                    function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['maxMemory'] = _getLastTimestampFromSeries(newData);

                            self.maxUsedDriverMem = d3.format(".4s")(newData.values[0].split(' ')[1]);
                            self.maxAvailableDriverMem = d3.format(".4s")(newData.values[0].split(' ')[2]);

                            self.hasLoadedOnce['maxMemory'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching maxMemoryDriver metric.', ttl: 10000});
                        }
                    );
                };

                var updateRDDCacheDiskSpill = function() {
                    if (!self.now && self.hasLoadedOnce['rddCacheDiskSpill'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('rddCacheDiskSpill')
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'mean(memory_memUsed_MB), last(disk_diskSpaceUsed_MB)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['rddCacheDiskSpill'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length - 1; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateRDDCacheDiskSpill[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templateRDDCacheDiskSpill[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                }

                                self.hasLoadedOnce['rddCacheDiskSpill'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching rddCacheDiskSpill metrics.', ttl: 10000});
                        }
                    );
                };

                var updateGCTime = function() {
                    if (!self.now && self.hasLoadedOnce['gcTime'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('gcTime')
                               ' and service = \'driver\'';

                    VizopsService.getMetrics('graphite', 'non_negative_derivative(mean(\"PS-MarkSweep_time\"), 1s),' +
                                             'non_negative_derivative(mean(\"PS-Scavenge_time\"), 1s)', 'spark', tags,
                                             'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['gcTime'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length - 1; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateGCTime[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    $scope.templateGCTime[1].values.push({'x': +splitEntry[0], 'y': +splitEntry[2]});
                                }

                                self.hasLoadedOnce['gcTime'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching gcTime metrics.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updateMemorySpace();
                    updateMaxMemory();
                    updateGraphVCPU();
                    updateRDDCacheDiskSpill();
                    updateGCTime();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didnt use groupBy calls then it would be enough to upper limit the time with now()
                    var limits = 'time >= ' + self.startTimeMap[graphName] + 'ms';

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

                            // Initialize the graph timers
                            for (var key in self.startTimeMap) {
                              if (self.startTimeMap.hasOwnProperty(key)) {
                                self.startTimeMap[key] = info.startTime;
                              }
                            }

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                            self.nbExecutorsOnDriverHost = self.hostnames[self.executorInfo.entry[0].value[1]].length - 1;

                            if (self.now) { // only schedule the interval if app is running
                                self.appinfoInterval = $interval(function() { // update appinfo data
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            var info = success.data;

                                            self.nbExecutors = info.nbExecutors;
                                            self.executorInfo = info.executorInfo;
                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            // get the unique hostnames and the number of executors running on them
                                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                                            self.nbExecutorsOnDriverHost = self.hostnames[self.executorInfo.entry[0].value[1]].length - 1;

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(driver).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                            updateMetrics();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                }

                init();

                self.poller = $interval(function () {
                    updateMetrics();
                }, 10000);

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
            }
        ]
    );