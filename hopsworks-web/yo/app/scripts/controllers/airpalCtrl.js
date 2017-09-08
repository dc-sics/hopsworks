'use strict';

angular.module('hopsWorksApp')
        .controller('AirpalCtrl', ['$scope','JobService' ,'ProjectService', '$routeParams','$route','$sce', '$cookies','growl','$log',
          function ($scope,JobService,ProjectService, $routeParams, $route, $sce, $cookies,growl,$log) {

            var self = this;
           
            self.projectId = $routeParams.projectID;
            self.appId = $routeParams.appId;
            self.email = $cookies.get("email");
            self.ui = "";
            self.loading = false;
            self.loadingText = "";
            self.job;
              
            self.getAirpalURL = function () {
                          
              self.ui = "/hopsworks-api/airpal/app?projectID="+self.projectId+"_"+self.email;
              return $sce.trustAsResourceUrl(self.ui);
            };
                
          }]);


