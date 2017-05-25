/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$routeParams', 'growl', 'JobService', '$location', 'ModalService', '$interval', 'StorageService',
                    'TourService', 'ProjectService','$timeout',
          function ($scope, $routeParams, growl, JobService, $location, ModalService, $interval, StorageService, TourService, ProjectService, $timeout) {

            var self = this;
            self.tourService = TourService;
            self.projectId = $routeParams.projectID;
            self.jobs; // Will contain all the jobs.
            self.runningInfo; //Will contain run information
            self.buttonArray = [];
            self.workingArray = [];
            self.jobFilter = "";
            self.hasSelectJob = false;

            self.currentjob = null;
            self.currentToggledIndex = -1;
            self.fetchingLogs = 0;
            self.loadingLog = 0;
            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };


            self.refreshSlider = function () {
              $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };
            
            self.editAsNew = function (job) {
              JobService.getConfiguration(self.projectId, job.id).then(
                      function (success) {
                        self.currentjob = job;
                        self.currentjob.runConfig = success.data;
                        self.refreshSlider();
                        self.copy();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };

            self.buttonClickedToggle = function (id, display) {
              self.buttonArray[id] = display;
              self.workingArray[id] = "true";
//              StorageService.store(self.projectId + "_jobstopclicked_"+id, self.workingArray[id]);
//              var status = StorageService.recover(self.projectId + "_jobstopclicked_"+id);
//              console.log("status: "+status);
            };

            self.stopbuttonClickedToggle = function (id, display) {
              self.workingArray[id] = display;
              var jobClickStatus = StorageService.recover(self.projectId + "_jobstopclicked_"+id);
              StorageService.store(self.projectId + "_jobstopclicked_"+id, jobClickStatus);
              if(jobClickStatus === "stopping"){
                StorageService.store(self.projectId + "_jobstopclicked_"+id, "killing");
              } else {
                StorageService.store(self.projectId + "_jobstopclicked_"+id, "stopping");
              }
            };
            
            self.getJobClickStatus = function(id){
              var status = StorageService.recover(self.projectId + "_jobstopclicked_"+id);
              if(status === "stopping" || status === "killing"){
                StorageService.store(self.projectId + "_jobstopclicked_"+id, status);
              }
              if(status !== "stopping" && status !== "killing"){
                status = "running";
              }
              return status;
            };

            self.copy = function () {
              var jobType;
              switch (self.currentjob.jobType.toUpperCase()) {
                case "SPARK":
                  jobType = 1;
                  break;
                case "ADAM":
                  jobType = 2;
                  break;
                case "FLINK":
                  jobType = 3;
                  break;
                case "PYSPARK":
                  jobType = 4;
                  break;
                case "TFSPARK":
                  jobType = 5;
                case "TENSORFLOW":
                  jobType = 6;  
              }
              var mainFileTxt, mainFileVal, jobDetailsTxt, sparkState, adamState, flinkState, tensorflowState;
              if (jobType === 1 || jobType === 4 || jobType === 5 ) {
                
                sparkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.appPath)
                };
                mainFileTxt = "App file";
                mainFileVal = sparkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType === 2) {
                adamState = {
                  "processparameter": null,
                  "commandList": null,
                  "selectedCommand": self.currentjob.runConfig.selectedCommand.command
                };
                mainFileTxt = "ADAM command";
                mainFileVal = adamState.selectedCommand;
                jobDetailsTxt = "Job arguments";
              } else if (jobType === 3) {
                flinkState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.appPath)
                };
                mainFileTxt = "JAR file";
                mainFileVal = flinkState.selectedJar;
                jobDetailsTxt = "Job details";
              } else if (jobType === 6) {
                tensorflowState = {
                  "selectedJar": getFileName(self.currentjob.runConfig.appPath)
                };
                mainFileTxt = "Python file";
                mainFileVal = tensorflowState.selectedJar;
                jobDetailsTxt = "Job details";
              }
              var state = {
                "jobtype": jobType,
                "jobname": self.currentjob.name,
                "localResources": self.currentjob.runConfig.localResources,
                "phase": 4,
                "runConfig": self.currentjob.runConfig,
                "sparkState": sparkState,
                "adamState": adamState,
                "flinkState": flinkState,
                "tensorflowState": tensorflowState,
                "accordion1": {//Contains the job name
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.currentjob.name,
                  "title": "Job name"},
                "accordion2": {//Contains the job type
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + self.currentjob.jobType,
                  "title": "Job type"},
                "accordion3": {// Contains the main execution file (jar, workflow,...)
                  "isOpen": false,
                  "visible": true,
                  "value": " - " + mainFileVal,
                  "title": mainFileTxt},
                "accordion4": {// Contains the job setup (main class, input variables,...)
                  "isOpen": false,
                  "visible": true,
                  "value": "",
                  "title": jobDetailsTxt},
                "accordion5": {//Contains the configuration and creation
                  "isOpen": false,
                  "visible": true,
                  "value": "",
                  "title": "Configure and create"},
                "accordion6" : {//Contains the pre-configuration and proposals for auto-configuration
                   "isOpen": false,
                   "visible": true,
                   "value": "",
                   "title": "Pre-Configuration"}
              };
              StorageService.store(self.projectId + "_newjob", state);
              $location.path('project/' + self.projectId + '/newjob');
            };



            var getAllJobs = function () {
              JobService.getAllJobsInProject(self.projectId).then(
                      function (success) {
                        self.jobs = success.data;
                        angular.forEach(self.jobs, function (job, key) {
                          job.showing = false;
                        });
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };
            
            self.getNumOfExecution = function () {
              if (self.hasSelectJob) {
                if (self.logset === undefined) {
                  return 0;
                }
                if (self.logset.length > 1) {
                  return self.logset.length;
                } else if (self.logset.length === 1) {
                  return 1;
                } else {
                  return 0;
                }
              }
            };

            /**
             * Retrieve status for all jobs of this project.
             * @returns {undefined}
             */
            self.getRunStatus = function () {
              JobService.getRunStatus(self.projectId).then(
                      function (success) {
                        self.runningInfo = success.data;
                        angular.forEach(self.jobs, function (temp, key) {
                          if (typeof self.runningInfo['' + temp.id] !== "undefined") {
                            if (!self.runningInfo['' + temp.id].running) {
                              self.buttonArray[temp.id] = false;
                            }
                          }
                        });
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Get data from runningInfo and update jobs.
             * @returns {undefined}
             */
            self.createAppReport = function () {
              angular.forEach(self.jobs, function (temp, key) {
                if (typeof self.runningInfo['' + temp.id] !== "undefined") {
                  if (temp.state !== self.runningInfo['' + temp.id].state && temp.showing === true) {
                    self.showLogs(temp.id);
                  }
                  temp.duration = self.runningInfo['' + temp.id].duration;
                  temp.finalStatus = self.runningInfo['' + temp.id].finalStatus;
                  temp.progress = self.runningInfo['' + temp.id].progress;
                  temp.running = self.runningInfo['' + temp.id].running;
                  temp.state = self.runningInfo['' + temp.id].state;
                  temp.submissiontime = self.runningInfo['' + temp.id].submissiontime;
                  temp.url = self.runningInfo['' + temp.id].url;
                }
              });
            };
            getAllJobs();
            self.getRunStatus();
            self.createAppReport();

            self.runJob = function (job, index) {
              var jobId = job.id;
              ProjectService.uberPrice({id: self.projectId}).$promise.then(
                      function (success) {
                        var price = success.multiplicator;
                        price = Math.ceil(parseFloat(price).toFixed(4)*100)/100;
                        ModalService.uberPrice('sm', 'Confirm', 'Do you still want to run this job?', price).then(
                                function (success) {
                                  JobService.runJob(self.projectId, jobId).then(
                                          function (success) {
                                            self.toggle(job, index);
                                            self.buttonClickedToggle(job.id, true);
                                            StorageService.store(self.projectId + "_jobstopclicked_"+job.id, "running");
//                                            self.stopbuttonClickedToggle(job.id, false);
                                            self.getRunStatus();
                                          }, function (error) {
                                    growl.error(error.data.errorMsg, {title: 'Failed to run job', ttl: 10000});
                                  });

                                }
                        );

                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not get the current YARN price.', ttl: 10000});
              });
            };

            self.stopJob = function (jobId) {
              self.stopbuttonClickedToggle(jobId, "true");
              JobService.stopJob(self.projectId, jobId).then(
                      function (success) {
                        self.getRunStatus();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to stop' +
                          ' job', ttl: 15000});
              });
            };
            
            self.killJob = function (jobId) {
              ModalService.confirm('sm', 'Confirm', 'Attemping to stop your job. For streaming jobs this operation can take a few minutes... Do you really want to kill this job and risk losing streaming events?').then(
                function (success) {
                  self.stopJob(jobId);
                }
              );
            };
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.newJob = function () {
              StorageService.clear();
              $location.path('project/' + self.projectId + '/newjob');
              if (self.tourService.currentStep_TourThree > -1) {
                self.tourService.resetTours();
              }
            };



            self.showDetails = function (job) {
              ModalService.jobDetails('lg', job, self.projectId);
            };

            self.showUI = function (job) {
              StorageService.store(self.projectId + "_jobui_" + job.name, job);
              $location.path('project/' + self.projectId + '/jobMonitor-job/' + job.name);

            };

            self.showLogs = function (jobId) {
              self.fetchingLogs = 1;
              JobService.showLog(self.projectId, jobId).then(
                  function (success) {
                    self.logset = success.data.logset;
                    self.fetchingLogs = 0;
                  }, function (error) {
                    self.fetchingLogs = 0;
                    growl.error(error.data.errorMsg, {title: 'Failed to show logs', ttl: 15000});
              });
            };
            
            self.getLog = function (job, type) {
              if (!(job[type] === undefined || job[type] === null)) {
                return;
              }
              self.loadingLog = 1;
              JobService.getLog(self.projectId, job.appId, type).then(
                  function (success) {
                    var logContent = success.data;
                    if (logContent[type] !== undefined) {
                      job[type] = logContent[type];
                    }
                    if (logContent[type + 'Path'] !== undefined) {
                      job[type + 'Path'] = logContent[type + 'Path'];
                    }
                    if (logContent['retriableErr'] !== undefined) {
                      job['retriableErr'] = logContent['retriableErr'];
                    }
                    if (logContent['retriableOut'] !== undefined) {
                      job['retriableOut'] = logContent['retriableOut'];
                    }
                    self.loadingLog = 0;
                  }, function (error) {
                    self.loadingLog = 0;
                    growl.error(error.data.errorMsg, {title: 'Failed to get logs', ttl: 5000});
              });
            };

            self.retryLogs = function (appId, type) {
              if (appId === '' || appId === undefined) {
                growl.error("Can not retry log. The job has not yet been assigned an Id", {title: 'Error', ttl: 5000});
              }
              JobService.retryLog(self.projectId, appId, type).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        self.showLogs(self.currentjob.id);
                      }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to get logs', ttl: 5000});
              });
            };

            self.deleteJob = function (jobId, jobName) {
              ModalService.confirm("sm", "Delete Job (" + jobName + ")",
                      "Do you really want to delete this job?\n\
                                This action cannot be undone.")
                      .then(function (success) {
                        JobService.deleteJob(self.projectId, jobId).then(
                                function (success) {
                                  getAllJobs();
                                  self.hasSelectJob = false;
                                  StorageService.remove(self.projectId + "_jobui_" + jobName);
                                  growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to delete job', ttl: 15000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 5000});
                      });
            };

            //Called when clicking on a job row
            self.toggle = function (job, index) {
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                if (job.id !== temp.id) {
                  temp.showing = false;
                }
              });

              //handle the clicked job accordingly
              job.showing = true;
              self.hasSelectJob = true;
              self.selectedIndex = index;
              self.currentToggledIndex = index;
              self.currentjob = job;
              StorageService.remove(self.projectId + "_jobui_" + job.name)
              StorageService.store(self.projectId + "_jobui_" + job.name, job)

            };
            
            //untoggle is not used in the jobsCtrl
            ////////////////////////////////////////////////////////////////////
            self.untoggle = function (job, index) {
              StorageService.remove(self.projectId + "_jobui_" + job.name)
              //reset all jobs showing flag
              angular.forEach(self.jobs, function (temp, key) {
                temp.showing = false;
              });

              if (self.currentToggledIndex !== index) {
                self.hasSelectJob = false;
                self.selectedIndex = -1;
                self.currentToggledIndex = -1;
              } else {
                job.showing = true;
              }
            };
            ////////////////////////////////////////////////////////////////////
            
            /**
             * Check if the jobType filter is null, and set to empty string if it is.
             * @returns {undefined}
             */
            self.checkJobTypeFilter = function () {
              if (self.jobFilter.jobType === null) {
                self.jobFilter.jobType = "";
              }
            };
            
            self.launchAppMasterUrl = function (trackingUrl) {
              window.open(trackingUrl);
            };


            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                self.getRunStatus();
                self.createAppReport();
              }, 5000);
            };
            startPolling();

            $scope.convertMS = function (ms) {
              if (ms === undefined) {
                return "";
              }
              var m, s;
              s = Math.floor(ms / 1000);
              m = Math.floor(s / 60);
              s = s % 60;
              if (s.toString().length < 2) {
                s = '0' + s;
              }
              if (m.toString().length < 2) {
                m = '0' + m;
              }
              var ret = m + ":" + s;
              return ret;
            };


            var init = function () {
              var stored = StorageService.contains(self.projectId + "_newjob");
              if (stored) {
//                self.newJob();
                  $location.path('project/' + self.projectId + '/newjob');
              }
            };

            init();
          }]);


