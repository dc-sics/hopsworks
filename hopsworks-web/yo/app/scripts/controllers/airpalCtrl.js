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
              
//            var startLoading = function (label) {
//              self.loading = true;
//              self.loadingText = label;
//            };       
//            var stopLoading = function () {
//              self.loading = false;
//              self.loadingText = "";
//            };
             
//             var getAppId = function (callback) {
//               console.log("2222222222222222222222");
//              if(self.appId==undefined || self.appId==false || self.appId==""){
//                console.log("333333333333333333333333333333");
//                  JobService.getAppId(self.projectId, self.job.id).then(
//                          function(success) {
//                            self.appId=success.data
//                            callback();
//                          }, function (error){
//                            growl.error(error.data.errorMsg, {title: 'Error fetching ui.', ttl: 15000});
//                              stopLoading();
//                          });
//                }else{
//                  callback();
//                }
//            }
//            
            
            
            
            
            self.getAirpalURL = function () {
              console.log("Loading Airpal UI...");
              console.log("1111111111111111111111");
              
//              getAppId(getAirpalUI);              
              
              
              
              self.ui = "/hopsworks-api/airpal/app?projectID="+self.projectId+"_"+self.email;
              return $sce.trustAsResourceUrl(self.ui);
            };
                
//            var getAirpalUI = function () {
//              console.log("444444444444444444444444444444444");
//              self.ui = "/hopsworks-api/airpal/app?projectID="+self.projectId+"_"+self.email+"_"+ self.appId;
//               console.log("5555555555555555555555555555555");
//            }
//            
            
            
            

//            self.getAirpalURL = function () {
//              return $sce.trustAsResourceUrl("http://bbc2.sics.se:64364/app");
//            };
          }]);


