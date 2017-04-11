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

                self.projectId = $routeParams.projectID;
                self.appId = "";

                var updateData = function() {
                    // Fetch data from influx

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