'use strict';

angular.module('hopsWorksApp')
         .controller('VizopsWorkerCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'VizopsService',

            function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;

                self.appId;

                var init = function() {
                    self.appId = VizopsService.getAppId();


                }

                init();

                self.poller = $interval(function () {
                    //updateMetrics();
                }, vizopsUpdateInterval());

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
            }
        ]
    );