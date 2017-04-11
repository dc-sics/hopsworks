'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('InfluxDBCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'InfluxDBService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, InfluxDBService) {

                var self = this;
                var startTime = -1; // startTime, endTime, now will be filled by init
                var endTime = -1;
                var now;
                var data;

                self.projectId = $routeParams.projectID;
                self.appId = "";
                // http://jsbin.com/yevopawiwe/edit?html,js,output
                var updateData = function() {
                    // Fetch data from influx
                    InfluxDBService.getSparkMetrics(self.projectId, self.appId, self.startTime).then(
                        function(success) {
                            var info = success.data;
                            //console.log(info);
                            updateGraph(info);
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                var updateGraph = function(data) {
                    var ctx = document.getElementById('myChart').getContext('2d');
                      var myChart = new Chart(ctx, {
                      type: 'line',
                      data: {
                        labels: data.series.values.map(
                              function(x) {
                                return x.split(' ')[0];
                              }),
                        datasets: [{
                          label: data.series.columns[1],
                          data: data.series.values.map(
                          function(x) {
                            return x.split(' ')[1];
                          }),
                          backgroundColor: "rgba(153,255,51,0.4)"
                        }, {
                          label: data.series.columns[2],
                          data: data.series.values.map(
                          function(x) {
                            return x.split(' ')[2];
                          }),
                          backgroundColor: "rgba(255,153,0,0.4)"
                        }]
                      }
                    });
                };

                // Every X seconds update all the data
                self.poller = $interval(function () {
                    updateData();
                }, 3000);

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

                /**
                 * Close the poller if the controller is destroyed.
                 */
                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
           }]);