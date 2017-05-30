'use strict';


angular.module('hopsWorksApp')
         .controller('VizopsOverviewCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
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
                self.nbHosts; // number of machines that ran application executors
                self.clusterCPUUtilization = "0.0";

                self.startTimeMap = {
                    'totalActiveTasksApp': -1,
                    'totalCompletedTasksApp': -1,
                    'clusterCPUUtilizationCard': -1
                };

                self.hasLoadedOnce = {
                    'totalActiveTasksApp': false,
                    'totalCompletedTasksApp': false,
                    'clusterCPUUtilizationCard': false
                };

                $scope.optionsTotalActiveTasks = vizopsTotalActiveTasksOptions();
                $scope.templateTotalActiveTasks = [];

                $scope.optionsTotalCompletedTasks = vizopsTotalCompletedTasksOptions();
                $scope.templateTotalCompletedTasks = [];

                var updateTotalActiveTasks = function() {
                    if (!self.now && self.hasLoadedOnce['totalActiveTasksApp'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalActiveTasksApp');

                    VizopsService.getMetrics('graphite',
                                               'sum(threadpool_activeTasks)', 'spark', tags, 'time(30s)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['totalActiveTasksApp'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');

                                    $scope.templateTotalActiveTasks[0].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                }

                                self.hasLoadedOnce['totalActiveTasksApp'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching TotalActiveTasksApp metrics.', ttl: 10000});
                        }
                    );
                };

                var updateTotalCompletedTasks = function() {
                    if (!self.now && self.hasLoadedOnce['totalCompletedTasksApp'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalCompletedTasksApp');

                    VizopsService.getMetrics('graphite',
                                               'non_negative_derivative(max(threadpool_completeTasks)),max(threadpool_completeTasks)',
                                               'spark', tags, 'time(30s), service fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;
                                self.startTimeMap['totalCompletedTasksApp'] = _getLastTimestampFromSeries(newData[0]);


                                for(var i = 0; i < newData[0].values.length; i++) {
                                    var timestamp = +newData[0].values[i].split(' ')[0];
                                    var totals = _.reduce(newData, function(sum, serie) {
                                        sum[0] += +serie.values[i].split(' ')[1];
                                        sum[1] += +serie.values[i].split(' ')[2];
                                        return sum;
                                    }, [0, 0]);


                                    $scope.templateTotalCompletedTasks[0].values.push({'x': timestamp, 'y': totals[0]}); // rate
                                    $scope.templateTotalCompletedTasks[1].values.push({'x': timestamp, 'y': totals[1]}); // total
                                }

                                self.hasLoadedOnce['totalCompletedTasksApp'] = true; // dont call backend again
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching TotalCompletedTasksApp metrics.', ttl: 10000});
                        }
                    );
                };

                var updateClusterCPUUtilization = function() {
                    if (!self.now && self.hasLoadedOnce['clusterCPUUtilizationCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'source =~ /' + self.executorInfo.entry[0].value[0] + '/' +
                               ' and time > ' + self.startTimeMap['clusterCPUUtilizationCard'] + 'ms' +
                               ' and time <= now()';

                    VizopsService.getMetrics('graphite',
                        'max(heap_used), heap_max, service', 'spark', tags).then(
                    function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['clusterCPUUtilizationCard'] = _getLastTimestampFromSeries(newData);

                            self.clusterCPUUtilization = d3.format(".1%")(newData.values[0].split(' ')[1]);

                            self.hasLoadedOnce['clusterCPUUtilizationCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching clusterCPUUtilization metric.', ttl: 10000});
                        }
                    );
                };

                var updateMetrics = function() {
                    updateTotalActiveTasks();
                    updateTotalCompletedTasks();
                    updateClusterCPUUtilization();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didnt use groupBy calls then it would be enough to upper limit the time with now()
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

                            // Initialize the graph timers
                            for (var key in self.startTimeMap) {
                              if (self.startTimeMap.hasOwnProperty(key)) {
                                self.startTimeMap[key] = info.startTime;
                              }
                            }

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                            self.nbHosts = Object.keys(self.hostnames).length;

                            $scope.templateTotalActiveTasks = vizopsTotalActiveTasksTemplate();
                            $scope.templateTotalCompletedTasks = vizopsTotalCompletedTasksTemplate();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching Overview info.', ttl: 10000});
                        }
                    );
                }

                init();

                self.poller = $interval(function () {
                    updateMetrics();
                }, vizopsUpdateInterval());

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
            }
        ]
    );