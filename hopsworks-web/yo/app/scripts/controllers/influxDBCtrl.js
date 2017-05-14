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
                self.endTime = -1; // application completion time
                self.now; // is the application running now?
                // array of dictionaries: self.executorInfo.entry[executor].value[0: container, 1: hostname, 2: nm vcores]
                self.executorInfo;
                self.nbExecutors;
                // a list of objects holding as a key the hostname and value the number of executors
                // and whatever else comes up
                self.hostnames = {};

                // Each graph will have its own startTimer - initialized by init to application's start time
                self.startTimeMap = {
                    'executorCPU': -1,
                    'hostCPU': -1,
                    'executorMemory': -1,
                    'executorPerNode': -1,
                    'taskPerNode': -1
                };

                // For Graph options, see js/graph-settings.js

                // graph initialization - per graph
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

                var updateGraphExecutorCPU = function() {
                    /* ask for the metrics coming from all the containers, otherwise we would bombard
                     the backend with nbExecutors requests. We choose the metrics for each executor
                     from the results. application_1491828160189_0001 -> 1491828160189_0001
                    */
                    InfluxDBService.getNodemanagerMetrics(self.projectId, self.appId, self.startTimeMap['executorCPU'],
                                                    ['MilliVcoreUsageIMinMilliVcores/' + (+self.executorInfo.entry[0].value[2]*1000),
                                                     'source'],
                                                    self.appId.substring(self.appId.indexOf('_') + 1)).then(
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
                        // projectId, appId, fields, host, startTime, endTime
                        InfluxDBService.getTelegrafCPUMetrics(self.projectId, self.appId, ['usage_user'],
                                        host, startTime, endTime).then(
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
                        var service = (i === 0) ? 'driver' : '' + i;
                        InfluxDBService.getSparkMetrics(self.projectId, self.appId, self.startTimeMap['executorMemory'],
                                                        ['heap_used', 'service'], service).then(
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
                    // Call the new graph function here
                    updateGraphExecutorCPU();
                    updateGraphExecutorMemory();
                    updateGraphHostCPU();
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
                            var colorMap = vizopsMapColorExecutorsHost(self.hostnames);

                            // TODO use info.now to call the endpoints only once and skip polling
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