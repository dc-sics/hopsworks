'use strict';

angular.module('hopsWorksApp')
        .controller('AirpalCtrl', ['$scope','JobService' ,'ProjectService', '$routeParams','$route','$sce', '$cookies','growl','$log',
          function ($scope,JobService,ProjectService, $routeParams, $route, $sce, $cookies,growl,$log) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.email = $cookies.get("email");
            self.ui = "";
              
                   
             
            self.getAirpalURL = function () {
              console.log("Loading Airpal UI...");
              console.log("1111111111111111111111");
              self.ui = "/hopsworks-api/airpal/app?projectID="+self.projectId+"_"+self.email;
              return $sce.trustAsResourceUrl(self.ui);
            };
                   
            
            
            

//            self.getAirpalURL = function () {
//              return $sce.trustAsResourceUrl("http://bbc2.sics.se:64364/app");
//            };
          }]);


