'use strict';

angular.module('hopsWorksApp')
        .controller('AirpalCtrl', ['$scope', '$routeParams','$route','$sce',
          function ($scope, $routeParams, $route, $sce) {

            var self = this;

            self.getAirpalURL = function () {
              return $sce.trustAsResourceUrl("http://bbc2.sics.se:64364/app");
            };
          }]);


