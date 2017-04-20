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
                // Job details
                self.jobName = $routeParams.name;
                self.appId = ""; // startTime, endTime, now will be filled by init
                self.startTime = -1;
                self.endTime = -1;
                self.now;

                // Graph options, see js/graph-settings.js

                // graph initialization
                $scope.options = vizopsExecutorGraphOptions();

                $scope.data = [
                    {
                        values: [],
                        key: 'Total used',
                        color: '#ff7f0e'
                    },
                    {
                        values: [],
                        key: 'Heap used',
                        color: '#7777ff',
                        area: true
                    }
                ];

                var updateData = function() {
                    // Fetch data from influx
                    InfluxDBService.getSparkMetrics(self.projectId, self.appId, self.startTime,
                                                    ['total_used', 'heap_used'], 'driver').then(
                        function(success) {
                            if (success.status === 200) { // new measurements
                                var newData = success.data;
                                self.startTime = +newData.lastMeasurementTimestamp;
                                updateGraph(newData.series.values);
                            } // dont do anything if response 204(no content), nothing new
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching spark metrics.', ttl: 15000});
                        }
                    );
                };

                var updateGraph = function(newData) {
                    var labels = newData.map(function(x) {
                        return +x.split(' ')[0];
                    });

                    var new_total_used = newData.map(function(x) {
                        return +x.split(' ')[1];
                    });

                    var new_heap_used = newData.map(function(x) {
                        return +x.split(' ')[2];
                    });

                    for(var i = 0; i < labels.length; i++) {
                        $scope.data[0].values.push({'x': labels[i], 'y': new_total_used[i]});
                        $scope.data[1].values.push({'x': labels[i], 'y': new_heap_used[i]});
                    }
                };

                var init = function() {
                    self.appId = InfluxDBService.getAppId();

                    JobService.getAppInfo(self.projectId, self.appId).then(
                        function(success) {
                            var info = success.data;
                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                // Every X seconds retrieve the new data
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