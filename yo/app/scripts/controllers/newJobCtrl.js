/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs creation page.
 * 
 * As it stands, self controller contains a lot of logic concerning all different 
 * job types. It would be nicer to have these as Mixins in a different file. 
 * Guess that's a TODO.a
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService',
          '$location', 'ModalService', 'StorageService', '$scope', 'SparkService',
          'AdamService', 'FlinkService', 'TourService', 'HistoryService',
          function ($routeParams, growl, JobService,
                  $location, ModalService, StorageService, $scope, SparkService,
                  AdamService, FlinkService, TourService, HistoryService) {

            var self = this;
            self.tourService = TourService;
            self.flinkjobtype = ["Streaming", "Batch"];
            //Set services as attributes 
            self.ModalService = ModalService;
            self.growl = growl;

            // keep the proposed configurations
            self.autoConfigResult;

            //Set some (semi-)constants
            self.selectFileRegexes = {
              "SPARK": /.jar\b/,
              "FLINK": /.jar\b/,
              "LIBRARY": /.jar\b/,
              "ADAM": /[^]*/
            };
            self.selectFileErrorMsgs = {
              "SPARK": "Please select a JAR file.",
              "FLINK": "Please select a JAR file.",
              "LIBRARY": "Please select a JAR file.",
              "ADAM-FILE": "Please select a file.",
              "ADAM-FOLDER": "Please select a folder."
            };
            self.projectId = $routeParams.projectID;

            //Create variables for user-entered information
            self.jobtype; //Will hold the selection of which job to create.
            self.jobname; //Will hold the name of the job

            self.localResources = [];//Will hold extra libraries

            self.newJobName = self.projectId + "_newjob";

            self.phase = 0; //The phase of creation we are in.
            self.runConfig; //Will hold the job configuration
            self.sliderOptions = {
                from: 1,
                to: 500,      
                floor: true,
                step: 1,
                vertical: false
//                callback: function(value, elt) {
//                    self.runConfig.numberOfExecutorsMin = value.split(";")[0];
//                    self.runConfig.numberOfExecutorsMax = value.split(";")[1];
//                }				
            };
            self.sliderValue = self.sliderOptions.from + ";" + 10;
            self.setInitExecs = function () {
              if (self.sliderValue.split(";")[0] >
                      self.runConfig.numberOfExecutorsInit) {
                self.runConfig.numberOfExecutorsInit =
                        parseInt(self.sliderValue.split(";")[0]);
              } else if (self.sliderValue.split(";")[1] <
                      self.runConfig.numberOfExecutorsInit) {
                self.runConfig.numberOfExecutorsInit =
                        parseInt(self.sliderValue.split(";")[1]);
              }
            };

            self.sparkState = {//Will hold spark-specific state
              "selectedJar": null //The path to the selected jar
            };
            self.flinkState = {//Will hold flink-specific state
              "selectedJar": null //The path to the selected jar
            };
            self.adamState = {//Will hold ADAM-specific state
              "processparameter": null, //The parameter currently being processed
              "commandList": null, //The ADAM command list.
              "selectedCommand": null //The selected ADAM command
            };
            //Variables for front-end magic
            self.accordion1 = {//Contains the job name
              "isOpen": true,
              "visible": true,
              "value": "",
              "title": "Job name"};
            self.accordion2 = {//Contains the job type
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Job type"};
            self.accordion3 = {// Contains the main execution file (jar, workflow,...)
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": ""};
            self.accordion4 = {// Contains the job setup (main class, input variables,...)
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": ""};
            self.accordion5 = {//Contains the configuration and creation
              "isOpen": false,
              "visible": false,
              "value": "",
              "title": "Configure and create"};

            self.undoable = false; //Signify if a clear operation can be undone.

            /**
             * Clear the current state (and allow for undo).
             * @returns {undefined}
             */
            self.clear = function () {
              var state = {
                "jobtype": self.jobtype,
                "jobname": self.jobname,
                "localResources": self.localResources,
                "phase": self.phase,
                "runConfig": self.runConfig,
                "sparkState": self.sparkState,
                "adamState": self.adamState,
                "accordions": [self.accordion1, self.accordion2, self.accordion3, self.accordion4, self.accordion5]
              };
              self.undoneState = state;
              self.undoable = true;
              self.jobtype = null;
              self.jobname = null;
              self.localResources = [];
              self.phase = 0;
              self.runConfig = null;
              self.sparkState = {
                "selectedJar": null //The path to the selected jar
              };
              self.adamState = {//Will hold ADAM-specific state
                "processparameter": null, //The parameter currently being processed
                "commandList": null, //The ADAM command list.
                "selectedCommand": null //The selected ADAM command
              };
              //Variables for front-end magic
              self.accordion1 = {//Contains the job name
                "isOpen": true,
                "visible": true,
                "value": "",
                "title": "Job name"};
              self.accordion2 = {//Contains the job type
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Job type"};
              self.accordion3 = {// Contains the main execution file (jar, workflow,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion4 = {// Contains the job setup (main class, input variables,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion5 = {//Contains the configuration and creation
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Configure and create"};
            };


            /**
             * Clear the current state (and allow for undo).
             * @returns {undefined}
             */
            self.clear = function () {
              var state = {
                "jobtype": self.jobtype,
                "jobname": self.jobname,
                "localResources": self.localResources,
                "phase": self.phase,
                "runConfig": self.runConfig,
                "sparkState": self.sparkState,
                "flinkState": self.flinkState,
                "adamState": self.adamState,
                "accordions": [self.accordion1, self.accordion2, self.accordion3, self.accordion4, self.accordion5],
              };
              self.undoneState = state;
              self.undoable = true;
              self.jobtype = null;
              self.jobname = null;
              self.localResources = [];
              self.phase = 0;
              self.runConfig = null;
              self.sparkState = {
                "selectedJar": null //The path to the selected jar
              };
              self.flinkState = {
                "selectedJar": null //The path to the selected jar
              };
              self.adamState = {//Will hold ADAM-specific state
                "processparameter": null, //The parameter currently being processed
                "commandList": null, //The ADAM command list.
                "selectedCommand": null //The selected ADAM command
              };
              //Variables for front-end magic
              self.accordion1 = {//Contains the job name
                "isOpen": true,
                "visible": true,
                "value": "",
                "title": "Job name"};
              self.accordion2 = {//Contains the job type
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Job type"};
              self.accordion3 = {// Contains the main execution file (jar, workflow,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion4 = {// Contains the job setup (main class, input variables,...)
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": ""};
              self.accordion5 = {//Contains the configuration and creation
                "isOpen": false,
                "visible": false,
                "value": "",
                "title": "Configure and create"};
            };
            
            self.exitToJobs = function() {
              self.clear();
              StorageService.remove(self.newJobName);
              self.removed = true;
              $location.path('project/' + self.projectId + '/jobs');
            };

            self.undoClear = function () {
              if (self.undoneState !== null) {
                self.jobtype = self.undoneState.jobtype;
                self.jobname = self.undoneState.jobname;
                self.localResources = self.undoneState.localResources;
                self.phase = self.undoneState.phase;
                self.runConfig = self.undoneState.runConfig;
                self.sparkState = self.undoneState.sparkState;
                self.flinkState = self.undoneState.flinkState;
                self.adamState = self.undoneState.adamState;
                self.accordion1 = self.undoneState.accordions[0];
                self.accordion2 = self.undoneState.accordions[1];
                self.accordion3 = self.undoneState.accordions[2];
                self.accordion4 = self.undoneState.accordions[3];
                self.accordion5 = self.undoneState.accordions[4];
              }
              self.unodeState = null;
              self.undoable = false;
            };
            /**
             * Create the job.
             * @returns {undefined}
             */
            self.createJob = function () {
              self.runConfig.appName = self.jobname;
              self.runConfig.localResources = self.localResources;
              self.runConfig.selectedMinExecutors = self.sliderValue.split(";")[0];
              self.runConfig.selectedMaxExecutors = self.sliderValue.split(";")[1];
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.resetTours();
                self.tourService.currentStep_TourThree = 2;
                self.tourService.createdJobName = self.jobname;
              }
              JobService.createNewJob(self.projectId, self.getJobType(), self.runConfig).then(
                      function (success) {
                        $location.path('project/' + self.projectId + '/jobs');
                        StorageService.remove(self.newJobName);
                        self.removed = true;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Callback method for when the user filled in a job name. Will then 
             * display the type selection.
             * @returns {undefined}
             */
            self.nameFilledIn = function () {
              if (self.phase === 0) {
                if (!self.jobname) {
                  var date = new Date().getTime() / 1000;
                  self.jobname = "Job-" + date;
                }
                self.phase = 1;
                self.accordion2.isOpen = true; //Open type selection
                self.accordion2.visible = true; //Display type selection
              }
              self.accordion1.value = " - " + self.jobname; //Edit panel title
              self.removed = false;
              self.undoneState = null; //Clear previous state.
              self.undoable = false;
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 2;
              }
            };



            /**
             * Callback method for when the user selected a job type. Will then 
             * display the file selection.
             * @returns {undefined}
             */
            self.jobTypeChosen = function () {
              self.phase = 2;
              self.accordion3.isOpen = true; //Open file selection
              var selectedType;
              switch (self.jobtype) { //Set the panel titles according to job type
                case 1:
                  self.accordion3.title = "JAR file";
                  self.accordion4.title = "Job details";
                  selectedType = "Spark";
                  break;
                case 2:
                  self.accordion3.title = "ADAM command";
                  self.accordion4.title = "Job arguments";
                  selectedType = "ADAM";
                  break;
                case 3:
                  self.accordion3.title = "JAR file";
                  self.accordion4.title = "Job details";
                  selectedType = "Flink";
                  break;
              }
              self.accordion1.isOpen = false; //Close job name panel
              self.accordion1.value = " - " + self.jobname; //Set job name panel title
              self.accordion3.visible = true; //Display file selection
              self.accordion2.value = " - " + selectedType; //Set job type panel title
              self.accordion2.isOpen = false; //Close job type panel
              self.accordion4.isOpen = false; //Close job setup
              self.accordion4.visible = false; //Hide job setup
              self.accordion5.visible = false; // Hide job configuration
              self.accordion3.value = ""; //Reset selected file
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 4;
              }
            };


            /**
             * Get the String representation of the selected jobType.
             * @returns {String}
             */
            self.getJobType = function () {
              switch (self.jobtype) {
                case 1:
                  return "SPARK";
                case 2:
                  return "ADAM";
                case 3:
                  return "FLINK";
                default:
                  return null;
              }
            };

            self.jobTypeSpark = function () {
              self.jobtype = 1;
              self.jobTypeChosen();
            };

            self.chooseParameters = function () {
              if (!self.runConfig.mainClass && !self.runConfig.args) {
                self.runConfig.mainClass = 'org.apache.spark.examples.SparkPi';
                self.runConfig.args = '1 111';
              }
              if (self.tourService.currentStep_TourFour > -1) {
                self.tourService.currentStep_TourFour = 7;
              }
            };




            /**
             * Callback method for when the main job file has been selected.
             * @param {type} path
             * @returns {undefined}
             */
            self.mainFileSelected = function (path) {
              self.phase = 3;
              self.accordion4.isOpen = true; // Open job setup
              self.accordion4.visible = true; // Show job setup
              self.accordion5.visible = true; // Show job config
              self.accordion3.value = " - " + path; // Set file selection title
              self.accordion3.isOpen = false; //Close file selection
            };

            /**
             * Callback for when the job setup has been completed.
             * @returns {undefined}
             */
            self.jobDetailsFilledIn = function () {
              self.phase = 4;
            };

            /**
             * Callback for when the user selected a file.
             * @param {String} reason
             * @param {String} path
             * @returns {undefined}
             */
            self.onFileSelected = function (reason, path) {
              var filename = getFileName(path);
              switch (reason.toUpperCase()) {
                case "SPARK":
                  self.sparkState.selectedJar = filename;
                  SparkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            //Update the min/max spark executors based on 
                            //backend configuration 
                            if (typeof runConfig !== 'undefined') {
                              self.sliderOptions.from = self.runConfig.
                                      minExecutors;
                              self.sliderOptions.to = self.runConfig.
                                      maxExecutors;
                            } else {
                              self.sliderOptions.from = 1;
                              self.sliderOptions.to = 300;
                            }
                            self.mainFileSelected(filename);
                            if (self.tourService.currentStep_TourFour > -1) {
                              self.tourService.currentStep_TourFour = 6;
                            }

                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                case "LIBRARY":
                  //Push the new library into the localresources array
                  self.localResources.push({
                      'name': filename,
                      'path': path, 
                      'type': null,
                      'visibility': null,
                      'pattern': null
                  });
                  break;
                case "ADAM":
                    self.adamState.processparameter.value = path;
                  if(typeof runConfig != 'undefined'){
                    self.sliderOptions.from = self.runConfig.minExecutors;
                    self.sliderOptions.to = self.runConfig.
                            maxExecutors;
                  } else {
                    self.sliderOptions.from = 1;
                    self.sliderOptions.to = 300;
                  }
                  break;
                case "FLINK":
                  self.flinkState.selectedJar = filename;
                  FlinkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
              }
            };

                /**
                 * Open a dialog for file selection.
                 * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
                 * @param {Object} parameter The Adam parameter to bind.
                 * @returns {undefined}
                 */
                this.selectFile = function (reason, parameter) {
                    if (reason.toUpperCase() === "ADAM") {
                        self.adamState.processparameter = parameter;
                    }
                    ModalService.selectFile('lg', self.selectFileRegexes[reason],
                            self.selectFileErrorMsgs["ADAM-FILE"]).then(
                            function (success) {
                                self.onFileSelected(reason, "hdfs://" + success);
                            }, function (error) {
                        //The user changed their mind.
                    });
                };
                /**
                 * Open a dialog for directory selection.
                 * @param {String} reason Goal for which the file is selected. (JobType or "LIBRARY").
                 * @param {Object} parameter The Adam parameter to bind.
                 * @returns {undefined}
                 */
                this.selectDir = function (reason, parameter) {
                    if (reason.toUpperCase() === "ADAM") {
                        self.adamState.processparameter = parameter;
                    }
                    ModalService.selectDir('lg', self.selectFileRegexes[reason],
                            self.selectFileErrorMsgs["ADAM-FOLDER"]).then(
                            function (success) {
                                self.onFileSelected(reason, "hdfs://" + success);
                                if (reason.toUpperCase() === "ADAM") {
                                  growl.info("Insert output file name", {title: 'Required', ttl: 5000});
                                }
                            }, function (error) {
                        //The user changed their mind.
                    });
                };


                /**
                 * Get a list of ADAM commands from the server.
                 * @returns {undefined}
                 */
                this.getCommandList = function () {
                    AdamService.getCommandList(self.projectId).then(
                            function (success) {
                                self.adamState.commandList = success.data;
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                    });
                };

                /**
                 * Remove the given entry from the localResources list.
                 * @param {type} lib
                 * @returns {undefined}
                 */
                this.removeLibrary = function (name) {
                    var arlen = self.localResources.length;
                    for (var i = 0; i < arlen; i++) {
                        if (self.localResources[i].name === name) {
                            self.localResources.splice(i, 1);
                            return;
                        }
                    }
                };

            /**
             * Save state upon destroy.
             */
            $scope.$on('$destroy', function () {
              if (self.removed) {
                //The state was removed explicitly; do not add again.
                return;
              }
              var state = {
                "jobtype": self.jobtype,
                "jobname": self.jobname,
                "localResources": self.localResources,
                "phase": self.phase,
                "runConfig": self.runConfig,
                "sparkState": self.sparkState,
                "adamState": self.adamState,
                "flinkState": self.flinkState,
                "accordion1": self.accordion1,
                "accordion2": self.accordion2,
                "accordion3": self.accordion3,
                "accordion4": self.accordion4,
                "accordion5": self.accordion5
              };
              StorageService.store(self.newJobName, state);
            });
            /**
             * Init method: restore any previous state.
             * @returns {undefined}
             */
            var init = function () {
              var stored = StorageService.recover(self.newJobName);
              if (stored) {
                //Job information
                self.jobtype = stored.jobtype;
                self.jobname = stored.jobname;
                self.localResources = stored.localResources;
                self.phase = stored.phase;
                self.runConfig = stored.runConfig;
                if (self.runConfig) {
                  self.runConfig.schedule = null;
                  self.sliderOptions.from = self.runConfig.minExecutors;
                  self.sliderOptions.to = self.runConfig.maxExecutors;
                  self.sliderValue = self.runConfig.selectedMinExecutors + ";" + self.runConfig.selectedMaxExecutors;
                }
                if (self.jobtype === 1) {
                  self.sparkState = stored.sparkState;
                } else if (self.jobtype === 2) {
                  self.adamState = stored.adamState;
                } else if (self.jobtype === 3) {
                  self.flinkState = stored.flinkState;
                }
                //GUI state
                self.accordion1 = stored.accordion1;
                self.accordion2 = stored.accordion2;
                self.accordion3 = stored.accordion3;
                self.accordion4 = stored.accordion4;
                self.accordion5 = stored.accordion5;
              }
              if (self.adamState.commandList === null) {
                self.getCommandList();
              }
            };
            init(); //Call upon create;
            /**
             * Select an ADAM command by sending the name to the server, gets an 
             * AdamJobConfiguration back.
             * @param {string} command
             * @returns {undefined}
             */
            self.selectCommand = function (command) {
              self.adamState.selectedCommand = command;
              AdamService.getCommand(self.projectId, self.adamState.selectedCommand).then(
                      function (success) {
                        self.runConfig = success.data;
                        self.mainFileSelected(self.adamState.selectedCommand);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            /**
             * Creates a jobDetails object with the arguments typed by the user and send  
             * these attributes to the server. The server responds with the results from the 
             * heuristic search.
             * @returns {undefined}
             */
            self.autoConfig = function (filterValue) {
              self.autoConfigResult = {};
              var jobDetails = {};
              jobDetails.className = self.runConfig.mainClass;
              jobDetails.selectedJar = self.sparkState.selectedJar;
              jobDetails.inputArgs = self.runConfig.args;
              jobDetails.jobType = self.getJobType();
              jobDetails.projectId = self.projectId;
              jobDetails.jobName = self.jobname;
              jobDetails.filter = filterValue;

              if (!angular.isUndefined(jobDetails.className) && !angular.isUndefined(jobDetails.inputArgs) &&
                      !angular.isUndefined(jobDetails.selectedJar) && !angular.isUndefined(jobDetails.jobType)) {

                HistoryService.getHeuristics(jobDetails).then(
                        function (success) {
                          self.autoConfigResult = success.data;
                          console.log(self.autoConfigResult);
                        });
              }
            };

            /**
             * Checks the value of the proposed configuration.
             * The function is used to initialized the checked radio button
             * @param {type} value
             * @returns {Boolean}
             */
            $scope.checkRadio = function (value) {
              if (value === "Minimum") {
                return true;
              } else
                return false;
            };

            /**
             * When the user changes configutaion (using the radio button) the 
             * runConfig values change.
             * @param {type} value
             * @returns {undefined}
             */
            $scope.selectConfig = function (value) {
              for (var i = 0; i < self.autoConfigResult.jobProposedConfig.length; i++) {
                var obj = self.autoConfigResult.jobProposedConfig[i];
                if (obj.configType === value) {
                  self.runConfig.amMemory = obj.amMemory;
                  self.runConfig.amVCores = obj.amVcores;
                  self.runConfig.amQueue = "default";
                  self.runConfig.numberOfExecutors = obj.numOfExecutors;
                  self.runConfig.executorCores = obj.executorCores;
                  self.runConfig.executorMemory = obj.executorMemory;
                }
              }
            };

          }]);


