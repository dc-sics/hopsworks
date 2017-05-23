'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('InfluxDBCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'InfluxDBService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, InfluxDBService) {

                var self = this;
                self.projectId = $routeParams.projectID;
                self.updateLabel = vizopsGetUpdateLabel();
                // Job details
                self.jobName = $routeParams.name;
                self.appId = ""; // startTime, endTime, now will be filled by init
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now; // is the application running now?
                // array of dictionaries: self.executorInfo.entry[executor].value[0: container, 1: hostname, 2: nm vcores]
                self.executorInfo;
                self.nbExecutors;
                // a list of objects holding as a key the hostname and value the number of executors
                self.hostnames = {};

                // UI VARIABLES
                self.nbHosts; // number of machines that ran application executors
                // Executor TAB variables
                self.maxUsedExecutorMem = "0.0";
                self.maxAvailableExecutorMem = "0.0";

                // Each graph will have its own startTimer - initialized by init to application's start time
                self.startTimeMap = {
                    'totalActiveTasksApp': -1,
                    'totalCompletedTasksApp': -1,
                    'maxMemoryExecutorCard': -1,
                    'executorCPU': -1,
                    'hostCPU': -1,
                    'executorMemory': -1,
                    'executorPerNode': -1,
                    'taskPerNode': -1
                };

                // It is used to stop the update functions from calling the backend more times than needed
                self.hasLoadedOnce = {
                    'totalActiveTasksApp': false,
                    'totalCompletedTasksApp': false,
                    'maxMemoryExecutorCard': false,
                    'executorCPU': false,
                    'hostCPU': false,
                    'executorMemory': false,
                    'executorPerNode': false,
                    'taskPerNode': false
                };

                // For Graph options, see js/graph-settings.js

                // graph initialization - per graph
                // OVERVIEW: Total active tasks for the whole application
                $scope.optionsTotalActiveTasks = vizopsTotalActiveTasksOptions();
                $scope.templateTotalActiveTasks = [];
                // OVERVIEW: Total completed tasks for the whole application
                $scope.optionsTotalCompletedTasks = vizopsTotalCompletedTasksOptions();
                $scope.templateTotalCompletedTasks = [];
                // EXECUTOR CPU GRAPH
                $scope.optionsExecutorCPU = vizopsExecutorCPUOptions();
                $scope.templateExecutorCPU = [];
                // HOST CPU GRAPH
                $scope.optionsHostCPU = vizopsHostCPUOptions();
                $scope.templateHostCPU = [];
                // Executor MEMORY GRAPH
                $scope.optionsExecutorMemory = vizopsExecutorMemoryOptions();
                $scope.templateExecutorMemory = [];
                // Executor per node
                $scope.optionsExecutorPerNode = vizopsExecutorsPerNodeOptions();
                $scope.templateExecutorPerNode = [];
                // Tasks per node
                $scope.optionsTasksPerNode = [];
                $scope.templateTasksPerNode = [];

                var updateTotalActiveTasksOverview = function() {
                    if (!self.now && self.hasLoadedOnce['totalActiveTasksApp'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\'' +
                               ' and time > ' + self.startTimeMap['totalActiveTasksApp'] + 'ms';
                    // app has finished running and we havent called the backend yet
                    if (!self.now && !self.hasLoadedOnce['totalActiveTasksApp']) {
                        tags += ' and time < ' + self.endTime + 'ms';
                    } else if (self.now) {
                        tags += ' and time < now()';
                    }

                    InfluxDBService.getMetrics(self.projectId, self.appId, 'graphite',
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

                var updateTotalCompletedTasksOverview = function() {
                    if (!self.now && self.hasLoadedOnce['totalCompletedTasksApp'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\'' +
                               ' and time > ' + self.startTimeMap['totalCompletedTasksApp'] + 'ms';
                    // app has finished running and we havent called the backend yet
                    if (!self.now && !self.hasLoadedOnce['totalCompletedTasksApp']) {
                        tags += ' and time < ' + self.endTime + 'ms';
                    } else if (self.now) {
                        tags += ' and time < now()';
                    }

                    InfluxDBService.getMetrics(self.projectId, self.appId, 'graphite',
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

                var updateMaxMemoryExecutor = function() {
                    if (!self.now && self.hasLoadedOnce['maxMemoryExecutorCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\'' +
                               ' and service =~ /[0-9]%2B/'; // + sign encodes into space so....

                    InfluxDBService.getMetrics(self.projectId, self.appId, 'graphite',
                        'max(heap_used), heap_max, service', 'spark', tags).then(
                    function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series[0];
                            self.startTimeMap['maxMemoryExecutorCard'] = _getLastTimestampFromSeries(newData);

                            self.maxUsedExecutorMem = d3.format(".4s")(newData.values[0].split(' ')[1]);
                            self.maxAvailableExecutorMem = d3.format(".4s")(newData.values[0].split(' ')[2]);

                            self.hasLoadedOnce['maxMemoryExecutorCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching MaxExecMemory metric.', ttl: 10000});
                        }
                    );
                };

                var updateGraphExecutorCPU = function() {
                    /* ask for the metrics coming from all the containers, otherwise we would bombard
                     the backend with nbExecutors requests. We choose the metrics for each executor
                     from the results. application_1491828160189_0001 -> 1491828160189_0001
                    */
                    var tags = 'source =~ /.*' + (self.appId.substring(self.appId.indexOf('_') + 1)) + '.*/' +
                               ' and time > ' + self.startTimeMap['executorCPU'] + 'ms';
                    InfluxDBService.getMetrics(self.projectId, self.appId, 'graphite',
                        'MilliVcoreUsageIMinMilliVcores/' + (+self.executorInfo.entry[0].value[2]*1000) + ',source',
                        'nodemanager', tags).then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data;
                                self.startTimeMap['executorCPU'] = +newData.lastMeasurementTimestamp;

                                var metrics = newData.series.values;

                                for(var i = 0; i < metrics.length; i++) {
                                    var splitEntry = metrics[i].split(' ');
                                    var executorID = +splitEntry[2].charAt(splitEntry[2].length - 1);

                                    $scope.templateExecutorCPU[executorID - 1].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                }
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching ExecutorCPU metrics.', ttl: 10000});
                        }
                    );
                };

                var updateGraphHostCPU = function() {
                    /*
                        Telegraf will keep receiving data so we need to limit it. If it's the first time
                        we call the backend then get the first few seconds, otherwise use the last
                        measured time from executorCPU
                    */

                    var startTime = self.startTimeMap['hostCPU'], endTime = -1;
                    if (self.startTimeMap['executorCPU'] === self.startTimeMap['hostCPU'])
                        endTime = self.startTimeMap['hostCPU'] + 10000;
                    else
                        endTime = self.startTimeMap['executorCPU'] + 10000;

                    // assume that the template has been created with the same sequence as the for loop
                    var unique_hosts = Object.keys(self.hostnames);
                    for(var host of unique_hosts) {
                        // (projectId, appId, database, columns, measurement, tags, groupBy)
                        var tags = 'host=\''+ host + '\' and time > ' + startTime + 'ms and time < ' + endTime + 'ms';
                        InfluxDBService.getMetrics(self.projectId, self.appId, 'telegraf', 'usage_user', 'cpu', tags).then(
                            function(success) {
                                if (success.status === 200) { // new measurements
                                    var newData = success.data;
                                    self.startTimeMap['hostCPU'] = +newData.lastMeasurementTimestamp;

                                    var metrics = newData.series.values;
                                    var labels = metrics.map(function(x) { return +x.split(' ')[0]; });
                                    var cpu_usage = metrics.map(function(x) { return +x.split(' ')[1]; });

                                    for(var i = 0; i < labels.length; i++) {
                                        $scope.templateHostCPU[unique_hosts.indexOf(host)].values.push({'x': labels[i], 'y': cpu_usage[i]});
                                    }
                                } // dont do anything if response 204(no content), nothing new
                            }, function(error) {
                                growl.error(error.data.errorMsg, {title: 'Error fetching HostCPU metrics.', ttl: 10000});
                            }
                        );
                    }
                };

                var updateGraphExecutorMemory = function() {
                    // Executor id 0 is the driver, 1+ the rest
                    for(var i = 0; i < self.nbExecutors; i++) {
                        // (projectId, appId, database, columns, measurement, tags, groupBy)
                        var service = (i === 0) ? 'driver' : '' + i;
                        var tags = 'appid=\'' + self.appId + '\' ' +
                                    'and service =\''+ service + '\' ' +
                                    'and time > ' + self.startTimeMap['executorMemory'] + 'ms';
                        InfluxDBService.getMetrics(self.projectId, self.appId, 'graphite',
                                                        'heap_used, service', 'spark', tags).then(
                            function(success) {
                                if (success.status === 200) { // new measurements
                                    var newData = success.data;
                                    self.startTimeMap['executorMemory'] = +newData.lastMeasurementTimestamp;

                                    var metrics = newData.series.values;

                                    for(var i = 0; i < metrics.length; i++) {
                                        var splitEntry = metrics[i].split(' ');
                                        var executorID = (splitEntry[2] === 'driver') ? 0 : +splitEntry[2];

                                        $scope.templateExecutorMemory[executorID].values.push({'x': +splitEntry[0], 'y': +splitEntry[1]});
                                    }
                                } // dont do anything if response 204(no content), nothing new
                            }, function(error) {
                                growl.error(error.data.errorMsg, {title: 'Error fetching ExecutorMemory metrics.', ttl: 10000});
                            }
                        );
                    }
                };

                var updateData = function() {
                    // Overview tab update functions
                    updateTotalActiveTasksOverview();
                    updateTotalCompletedTasksOverview();
                    // Executor tab update functions
                    updateMaxMemoryExecutor();
                    /*updateGraphExecutorCPU();
                    updateGraphExecutorMemory();
                    updateGraphHostCPU();*/
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
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
                    self.appId = InfluxDBService.getAppId();

                    JobService.getAppInfo(self.projectId, self.appId).then(
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

                            var colorMap = vizopsMapColorExecutorsHost(self.hostnames);

                            $scope.templateTotalActiveTasks = vizopsTotalActiveTasksTemplate();
                            $scope.templateTotalCompletedTasks = vizopsTotalCompletedTasksTemplate();
                            $scope.templateExecutorCPU = vizopsExecutorCPUDataTemplate(self.nbExecutors, colorMap);
                            $scope.templateHostCPU = vizopsHostCPUDataTemplate(Object.keys(self.hostnames), colorMap);
                            $scope.templateExecutorMemory = vizopsExecutorMemoryDataTemplate(self.nbExecutors, colorMap);
                            $scope.templateExecutorPerNode = vizopsExecutorsPerNodeTemplate(self.hostnames, colorMap);
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                self.poller = $interval(function () {
                    updateData();
                }, vizopsUpdateInterval());

                /**
                 * Close the poller if the controller is destroyed.
                 */
                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
           }]);