'use strict';

angular.module('hopsWorksApp')
        .controller('HistoryCtrl', ['$scope' , 'HistoryService' , 'ModalService' , '$routeParams' ,
          function ($scope  , HistoryService , ModalService , $routeParams) {

            var self = this;
            self.jobs = [];
            self.projectId = $routeParams.projectID;
            $scope.convertMS;
            
            $scope.pageSize = 12;
            
            $scope.searchChoices=[
                {
                    id : 0, 
                    name : "-- Search By Type --", 
                    searchName: "none", 
                    values:[
                        { ind:"none" , id: "null" , type: "null" }
                    ]
                },
                {
                    id : 1, 
                    name : "Job Type", 
                    searchName: "jobType",
                    values:[
                        { ind:"jobType" , id: "null" , type: "-- Select App Type --" },
                        { ind:"jobType" , id: "Spark" , type: "Spark" },
                        { ind:"jobType" , id: "Flink" , type: "Flink" },
                        { ind:"jobType" , id: "Adam" , type: "Adam" },
                        { ind:"jobType" , id: "HadoopJava" , type: "Hadoop Java" },
                        { ind:"jobType" , id: "Map Reduce" , type: "Map Reduce" }
                    ]
                },
                {
                    id : 2, 
                    name : "Severity", 
                    searchName: "severity",
                    values:[
                        { ind:"severity" , id: "null" , type: "-- Select Severity --" },
                        { ind:"severity" , id: 0 , type: "None" },
                        { ind:"severity" , id: 1 , type: "Low" },
                        { ind:"severity" , id: 2 , type: "Moderate" },
                        { ind:"severity" , id: 3 , type: "Severe" },
                        { ind:"severity" , id: 4 , type: "Critical" } 
                    ]
                }
            ];  
            
            $scope.sortType = 'id';
            $scope.sortReverse = false;
            $scope.searchFilter;  
            
            $scope.enableSearch = false;
            
            // Attributes for the Search - Drop Down Menus
            $scope.name = '-- Search By Type --';
            $scope.searchName = 'none';
            $scope.fisrtFormSelected = false;
            
            $scope.valueId = 'null';
            $scope.valueType = '-- Select Value --';
            
            $scope.sort = function (keyname) {
              $scope.sortType = keyname;   //set the sortKey to the param passed
              $scope.sortReverse = !$scope.sortReverse; //if true make it false and vice versa
            };
            
            self.showDetails = function (job) {
              ModalService.historyDetails(job , 'lg');
            };
            
            self.selectFirstForm = function (name, searchName) {
              $scope.name = name;
              $scope.searchName = searchName;
              $scope.valueId = 'null';
              $scope.valueType = '-- Select Value --';
              if (searchName == "jobType"){
                  $scope.valueType = '-- Select App Type --';
              }
              else if(searchName == "severity"){
                  $scope.valueType = '-- Select Severity --';
              }
              if (searchName === "none") {
                    $scope.fisrtFormSelected = false;
              } else {
                    $scope.fisrtFormSelected = true;
              }
            };
            
            self.selectSecondForm = function (type, id) {
              $scope.valueType = type;
              $scope.valueId = id;
            };
            
            self.clear = function () {
                $scope.name = '-- Search By Type --';
                $scope.searchName = 'none';
                $scope.fisrtFormSelected = false;
            
                $scope.valueId = 'null';
                $scope.valueType = '-- Select Value --';
            };
            
            $scope.filterJobs = function(job){
                if($scope.searchName === "jobType" && $scope.valueId !== "null"){
                    return job.yarnAppResult.jobType === $scope.valueId;
                }
                else if($scope.searchName === "severity" && $scope.valueId !== "null"){
                    return job.yarnAppResult.severity === $scope.valueId;
                }
                else{
                    return job;
                }
            };
            
            var getAllHistory = function () {;
              HistoryService.getAllHistoryRecords(self.projectId).then(
                      function (success) {
                        self.jobs = success.data;
                      });
            };
            
            $scope.convertMS = function(ms) {
                    var m, s;
                    s = Math.floor(ms / 1000);
                    m = Math.floor(s / 60);
                    s = s % 60;
                    if (s.toString().length < 2) {
                        s = '0'+s;
                    }
                    if (m.toString().length < 2) {
                        m = '0'+m;
                    }
                    
                    var ret = m + ":" + s;
                    return ret;
            };
            
            getAllHistory();
            
          }]);

