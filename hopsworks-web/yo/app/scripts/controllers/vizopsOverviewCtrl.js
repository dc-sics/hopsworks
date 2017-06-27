'use strict';

/*  Group by calls have an issue right now that the last measurement has to be ignored. This happens because the last
    time bucket is not full yet and when the next backend call happens that bucket will be full and as such more accurate.
    In the 200ok case you should be ignoring the last measurement for now.
*/

angular.module('hopsWorksApp')
         .controller('VizopsOverviewCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'VizopsService',

            function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;

                self.appId;
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now = null; // is the application running now?
                // array of dictionaries: self.executorInfo.entry[executor].value[0: container, 1: hostname, 2: nm vcores]
                self.executorInfo;
                self.nbExecutors;
                // a list of objects holding as a key the hostname and value the number of executors
                self.hostnames = {};
                self.nbHosts; // number of machines that ran application executors
                self.clusterCPUUtilization = "0.0";
                self.nbLiveHosts = 0;
                self.appinfoInterval;
                self.uiShuffleRead = 0.0;
                self.uiShuffleWrite = 0.0;

                self.startTimeMap = {
                    'totalActiveTasksApp': -1,
                    'totalCompletedTasksApp': -1,
                    'clusterCPUUtilizationCard': -1,
                    'hdfsReadRateTotal': -1,
                    'hdfsWriteRateTotal': -1,
                    'liveHostsCard': -1,
                    'containerMemoryUsedTotal': -1,
                    'applicationShuffleTotal': -1
                };

                self.hasLoadedOnce = {
                    'totalActiveTasksApp': false,
                    'totalCompletedTasksApp': false,
                    'clusterCPUUtilizationCard': false,
                    'hdfsReadRateTotal': false,
                    'hdfsWriteRateTotal': false,
                    'liveHostsCard': false,
                    'containerMemoryUsedTotal': false,
                    'applicationShuffleTotal': false
                };

                $scope.optionsTotalActiveTasks = vizopsTotalActiveTasksOptions();
                $scope.optionsTotalCompletedTasks = vizopsTotalCompletedTasksOptions();
                $scope.optionsHDFSReadRateTotal = vizopsHDFSReadRateTotalOptions();
                $scope.optionsHDFSWriteRateTotal = vizopsHDFSWriteRateTotalOptions();
                $scope.optionsContainerMemoryUsedTotal = vizopsContainerMemoryUsedTotalOptions();

                $scope.templateTotalActiveTasks = [];
                $scope.templateTotalCompletedTasks = [];
                $scope.templateHDFSReadRateTotal = [];
                $scope.templateHDFSWriteRateTotal = [];
                $scope.templateContainerMemoryUsedTotal = [];

                var updateTotalActiveTasks = function() {
                    if (!self.now && self.hasLoadedOnce['totalActiveTasksApp'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('totalActiveTasksApp');

                    VizopsService.getMetrics('graphite',
                                               'sum(threadpool_activeTasks)', 'spark', tags,
                                               'time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series[0];
                                var metrics = newData.values;

                                self.startTimeMap['totalActiveTasksApp'] = _getLastTimestampFromSeries(newData);

                                for(var i = 0; i < metrics.length - 1; i++) {
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
                                               'spark', tags, 'time(' + VizopsService.getGroupByInterval() + 's), service fill(0)').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data.result.results[0].series;
                                self.startTimeMap['totalCompletedTasksApp'] = _getLastTimestampFromSeries(newData[0]);

                                for(var i = 0; i < newData[0].values.length - 1; i++) {
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

                    var tags = 'source != \'' + self.executorInfo.entry[0].value[0] + '\' and '
                                + _getTimestampLimits('clusterCPUUtilizationCard') + ' and ' +
                                'MilliVcoreUsageIMinMilliVcores <= ' + (+self.executorInfo.entry[0].value[2]*1000);

                    VizopsService.getMetrics('graphite',
                        'mean(MilliVcoreUsageIMinMilliVcores)/' + (+self.executorInfo.entry[0].value[2]*1000),
                        'nodemanager', tags).then(
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

                var updateHdfsReadRateTotal = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsReadRateTotal'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsReadRateTotal');

                    VizopsService.getMetrics('graphite',
                                             'non_negative_derivative(max(filesystem_hdfs_read_bytes)),max(filesystem_hdfs_read_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + 's), service fill(0)').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['hdfsReadRateTotal'] = _getLastTimestampFromSeries(newData[0]);

                            for(var i = 0; i < newData[0].values.length - 1; i++) { // each serie has the same number of values
                                var timestamp = +newData[0].values[i].split(' ')[0];
                                var totals = _.reduce(newData, function(sum, serie) {
                                    sum[0] += +serie.values[i].split(' ')[1];
                                    sum[1] += +serie.values[i].split(' ')[2];
                                    return sum;
                                }, [0, 0]);


                                $scope.templateHDFSReadRateTotal[0].values.push({'x': timestamp, 'y': totals[0]}); // rate-read
                                $scope.templateHDFSReadRateTotal[1].values.push({'x': timestamp, 'y': totals[1]}); // read
                            }

                            self.hasLoadedOnce['hdfsReadRateTotal'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching hdfsReadRateTotal metric.', ttl: 10000});
                        }
                    );

                };

                var updateHdfsWriteRateTotal = function() {
                    if (!self.now && self.hasLoadedOnce['hdfsWriteRateTotal'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('hdfsWriteRateTotal');

                    VizopsService.getMetrics('graphite',
                                             'non_negative_derivative(max(filesystem_hdfs_write_bytes)),max(filesystem_hdfs_write_bytes)',
                                             'spark', tags, 'time(' + VizopsService.getGroupByInterval() + 's), service fill(0)').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['hdfsWriteRateTotal'] = _getLastTimestampFromSeries(newData[0]);

                            for(var i = 0; i < newData[0].values.length - 1; i++) {
                                var timestamp = +newData[0].values[i].split(' ')[0];
                                var totals = _.reduce(newData, function(sum, serie) {
                                    sum[0] += +serie.values[i].split(' ')[1];
                                    sum[1] += +serie.values[i].split(' ')[2];
                                    return sum;
                                }, [0, 0]);

                                $scope.templateHDFSWriteRateTotal[0].values.push({'x': timestamp, 'y': totals[0]}); // rate-write
                                $scope.templateHDFSWriteRateTotal[1].values.push({'x': timestamp, 'y': totals[1]}); // total-write
                            }

                            self.hasLoadedOnce['hdfsWriteRateTotal'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching hdfsWriteRateTotal metric.', ttl: 10000});
                        }
                    );
                };

                var updateLiveHosts = function() {
                    if (!self.now && self.hasLoadedOnce['liveHostsCard'])
                        return; // offline mode + we have loaded the information

                    var tags = 'host =~ /' + Object.keys(self.hostnames).join('|') + '/ and ' + _getTimestampLimits('liveHostsCard');

                    VizopsService.getMetrics('telegraf', 'last(usage_system)', 'cpu', tags, 'host').then(
                      function(success) {
                        if (success.status === 200) { // new measurements
                            var newData = success.data.result.results[0].series;
                            self.startTimeMap['liveHostsCard'] = _getLastTimestampFromSeries(newData[0]);

                            var tempAliveHosts = 0;
                            for(var i = 0; i < newData.length; i++) { // loop over each executor
                                var host = newData[i].tags.entry[0].value; // if it's there, it's alive
                                // log it
                                tempAliveHosts += 1;
                            }
                            self.nbLiveHosts = tempAliveHosts;

                            self.hasLoadedOnce['liveHostsCard'] = true;
                        } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching liveHostsCard metric.', ttl: 10000});
                        }
                    );
                };

                var updateContainerMemoryUsedTotal = function() {
                    // Memory from spark is much easier to work with than nodemanager's metrics
                    if (!self.now && self.hasLoadedOnce['containerMemoryUsedTotal'])
                        return; // offline mode + we have loaded the information

                    var tags = 'appid = \'' + self.appId + '\' and ' + _getTimestampLimits('containerMemoryUsedTotal'); // + sign encodes into space so....

                    VizopsService.getMetrics('graphite', 'mean(heap_used), max(heap_max)', 'spark', tags,
                                             'service, time(' + VizopsService.getGroupByInterval() + 's) fill(0)').then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data.result.results[0].series;
                                  self.startTimeMap['containerMemoryUsedTotal'] = _getLastTimestampFromSeries(newData[0]);

                                  var timestampDictionary = {}, timestampsOrder = []; // { timestamp: [used sum, total sum] }
                                  for(var i = 0; i < newData.length; i++) { // loop over each executor/driver and sum the memories
                                        for(var j = 0; j < newData[i].values.length - 1; j++) { // go through each timestamp
                                            var split = newData[i].values[j].split(' ');
                                            var timestamp = +split[0];
                                            /* add both used and max to the rest
                                               This if will only be activated by the first series's entries,
                                               so we can maintain a list to maintain insertion order since the Object
                                               keys order depends on the browser
                                            */
                                            if (!(timestamp in timestampDictionary)) {
                                                timestampDictionary[timestamp] = [0,0];
                                                timestampsOrder.push(timestamp);
                                            }

                                            timestampDictionary[timestamp][0] += +split[1];
                                            timestampDictionary[timestamp][1] += +split[2];
                                        }
                                  }

                                  for(var time of timestampsOrder) {
                                      $scope.templateContainerMemoryUsedTotal[0].values.push({'x': time,
                                                                                              'y': timestampDictionary[time][0]});
                                      $scope.templateContainerMemoryUsedTotal[1].values.push({'x': time,
                                                                                              'y': timestampDictionary[time][1]});
                                  }

                                  self.hasLoadedOnce['containerMemoryUsedTotal'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error.data.errorMsg, {title: 'Error fetching containerMemoryUsedTotal metric.', ttl: 10000});
                          }
                      );
                };

                var updateApplicationShuffleTotal = function() {
                    if (!self.now && self.hasLoadedOnce['applicationShuffleTotal'])
                        return; // offline mode + we have loaded the information

                    VizopsService.getAllExecutorMetrics().then(
                          function(success) {
                              if (success.status === 200) { // new measurements
                                  var newData = success.data;

                                  var totalShuffleWrite = 0, totalShuffleRead = 0;
                                  for (var entry of newData) {
                                    totalShuffleWrite += entry.totalShuffleWrite;
                                    totalShuffleRead += entry.totalShuffleRead;
                                  }

                                  self.uiShuffleRead = d3.format(".2s")(totalShuffleRead);
                                  self.uiShuffleWrite = d3.format(".2s")(totalShuffleWrite);

                                  self.hasLoadedOnce['applicationShuffleTotal'] = true;
                              } // dont do anything if response 204(no content), nothing new
                          }, function(error) {
                              growl.error(error, {title: 'Error fetching shuffleData(overview) metric.', ttl: 10000});
                          }
                    );
                };

                var updateMetrics = function() {
                    updateTotalActiveTasks();
                    updateTotalCompletedTasks();
                    updateClusterCPUUtilization();
                    updateHdfsReadRateTotal();
                    updateHdfsWriteRateTotal();
                    updateLiveHosts();
                    updateContainerMemoryUsedTotal();
                    updateApplicationShuffleTotal();
                };

                var resetGraphs = function() {
                    for (var key in self.startTimeMap) {
                      if (self.startTimeMap.hasOwnProperty(key)) {
                        self.startTimeMap[key] = self.startTime;
                        self.hasLoadedOnce[key] = false;
                      }
                    }

                    $scope.templateTotalActiveTasks = vizopsTotalActiveTasksTemplate();
                    $scope.templateTotalCompletedTasks = vizopsTotalCompletedTasksTemplate();
                    $scope.templateHDFSReadRateTotal = vizopsHDFSReadRateTotalTemplate();
                    $scope.templateHDFSWriteRateTotal = vizopsHDFSWriteRateTotalTemplate();
                    $scope.templateContainerMemoryUsedTotal = vizopsContainerMemoryUsedTotalTemplate();
                };

                var _getLastTimestampFromSeries = function(serie) {
                    // Takes as an argument a single serie
                    return +serie.values[serie.values.length - 1].split(' ')[0];
                };

                var _getTimestampLimits = function(graphName) {
                    // If we didn't use groupBy calls then it would be enough to upper limit the time with now()
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

                            // get the unique hostnames and the number of executors running on them
                            self.hostnames = _extractHostnameInfoFromResponse(self.executorInfo);
                            self.nbHosts = Object.keys(self.hostnames).length; // Remove driver

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
                                            self.nbHosts = Object.keys(self.hostnames).length; // Remove driver

                                            if (!self.now) $interval.cancel(self.appinfoInterval);
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching appinfo(overview).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                            resetGraphs();
                            updateMetrics();
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching Overview info.', ttl: 10000});
                        }
                    );
                }

                init();

                self.poller = $interval(function () {
                    updateMetrics();
                }, VizopsService.getGroupByInterval() * 1000);

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                  $interval.cancel(self.appinfoInterval);
                });
            }
        ]
    );