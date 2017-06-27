'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('VizopsCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval', '$routeParams', '$route', 'VizopsService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                let self = this;
                self.jobName = $routeParams.name;
                self.appId = ""; // startTime, endTime, now will be filled by init
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now; // is the application running now?

                self.durationInterval;
                self.appinfoInterval; // checks for the app status
                self.durationLabel = "00:00:00";

                let init = function() {
                    self.appId = VizopsService.getAppId();

                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                        function(success) {
                            let info = success.data;

                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            self.durationInterval = $interval(function () {
                                if (self.now) {
                                    self.durationLabel = Date.now() - self.startTime;
                                } else {
                                    self.durationLabel = self.endTime - self.startTime;
                                }
                            }, 1000);

                            if (self.now) {
                                self.appinfoInterval = $interval(function() {
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            let info = success.data;

                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            if (!self.now) {
                                                $interval.cancel(self.appinfoInterval);
                                            }
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching app info(overview).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.durationInterval);
                  $interval.cancel(self.appinfoInterval);
                });
           }]);