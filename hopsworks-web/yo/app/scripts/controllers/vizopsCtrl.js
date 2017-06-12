'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('VizopsCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval', '$routeParams', '$route', 'VizopsService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;
                self.jobName = $routeParams.name;
                self.appId = ""; // startTime, endTime, now will be filled by init
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now; // is the application running now?

                self.durationInterval;
                self.durationLabel = "0m00s";

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

                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            if (self.now) {
                                self.durationInterval = $interval(function () {
                                    self.durationLabel = Date.now() - self.startTime;
                                }, 1000);
                            } else {
                                self.durationLabel = self.endTime - self.startTime;
                            }

                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.durationInterval);
                });
           }]);