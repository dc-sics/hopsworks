'use strict';

angular.module('hopsWorksApp')
        .controller('JupyterCtrl', ['$scope', '$routeParams', '$route',
          'growl', 'ModalService', 'JupyterService', 'TensorFlowService', 'SparkService', '$location', '$timeout', '$window', '$sce',
          function ($scope, $routeParams, $route, growl, ModalService, JupyterService, TensorFlowService, SparkService, 
          $location, $timeout, $window, $sce) {

            var self = this;
            self.connectedStatus = false;
            self.loading = false;
            self.advanced = false;
            self.loadingText = "";
            $scope.tgState = true;
            self.jupyterServer;
            self.toggleValue = false;
            var projectId = $routeParams.projectID;
            var statusMsgs = ['stopped    ', "running    ", 'stopping...', 'restarting...'];
            self.ui = "";
            self.sparkStatic = false;
            self.sparkDynamic = false;
            self.tensorflow = false;
            self.val = {};
            $scope.tgState = true;
            self.config = {};

//            self.settings = function () {
//              JupyterService.settings(projectId).then(
//                      function (success) {
//                        self.val = success.data;
//                        self.toggleValue = true;
//                      }, function (error) {
//              }
//            };
            
            self.deselect = function () {
//              self.selected = null;
//              refresh();
//              getNotesInProject(null, false);
            };


            self.sliderVisible = false;

            self.sliderOptions = {
              min: 1,
              max: 10,
              options: {
                floor: 0,
                ceil: 500
              },
              getPointerColor: function (value) {
                return '#4b91ea';
              }

            };

            self.refreshSlider = function () {
              $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
              });
            };

            self.toggleSlider = function () {
              self.sliderVisible = !self.sliderVisible;
              if (self.sliderVisible)
                self.refreshSlider();
            };

            self.setInitExecs = function () {
              if (self.sliderOptions.min >
                      self.val.dynamicInitialExecutors) {
                self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.min);
              } else if (self.sliderOptions.max <
                      self.val.dynamicInitialExecutors) {
                self.val.dynamicInitialExecutors =
                        parseInt(self.sliderOptions.max);
              }
              self.val.dynamicMinExecutors = self.sliderOptions.min;
              self.val.dynamicMaxExecutors = self.sliderOptions.max;
            };


            //Set some (semi-)constants
            self.selectFileRegexes = {
              "SPARK": /.jar\b/,
              "PY": /.py\b/,
              "FILE": /[^]*/,
              "ARCHIVE": /[^]*/
            };
            self.selectFileErrorMsgs = {
              "SPARK": "Please select a JAR file.",
              "PY": "Please select a Python file.",
              "ARCHIVE": "Please select a file.",
              "FILE": "Please select a folder."
            };

            this.selectFile = function (reason, parameter) {
              if (reason.toUpperCase() === "PY") {
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

            this.selectDir = function (reason, parameter) {
              if (reason.toUpperCase() === reason) {
                self.adamState.processparameter = parameter;
              }
              ModalService.selectDir('lg', self.selectFileRegexes[reason],
                      self.selectFileErrorMsgs[reason]).then(
                      function (success) {
                        self.onFileSelected(reason, "hdfs://" + success);
                        if (reason.toUpperCase() === reason) {
                          growl.info("Insert output file name", {title: 'Required', ttl: 5000});
                        }
                      }, function (error) {
                //The user changed their mind.
              });
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
                case "PYSPARK":
                case "TFSPARK":
                  self.sparkState.selectedJar = filename;
                  SparkService.inspectJar(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            if(reason.toUpperCase() === "TFSPARK"){
                              self.jobtype = 5;
                            } else {
                              if(self.runConfig.appPath.toLowerCase().endsWith(".py")){
                                self.jobtype = 4;
                              } else {
                                self.jobtype = 1;
                              }
                            }
                            //Update the min/max spark executors based on 
                            //backend configuration 
                            if (typeof self.runConfig !== 'undefined') {
                              self.sliderOptions.options['floor'] = self.runConfig.
                                      minExecutors;
                              self.sliderOptions.options['ceil'] = self.runConfig.
                                      maxExecutors;
                            } else {
                              self.sliderOptions.options['floor'] = 1;
                              self.sliderOptions.options['ceil'] = 300;
                            }
                            self.mainFileSelected(filename);
                            // For Kafka tour
                            if (self.projectIsGuide) {
                              self.tourService.currentStep_TourSeven = 6;
                            }

                            if (self.tourService.currentStep_TourFour > -1) {
                              self.tourService.currentStep_TourFour = 6;
                            }

                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                case "LIBRARY":
                  //Push the new library into the localresources array
                  var libType = 'file';
                  if(path.endsWith(".zip") || path.endsWith(".tar") || path.endsWith(".gz")){
                    libType = 'archive';
                  } 
                  self.localResources.push({
                    'name': filename,
                    'path': path,
                    'type': libType,
                    'visibility': 'application',
                    'pattern': null
                  });
                  break;
                case "ADAM":
                  self.adamState.processparameter.value = path;
                  if (typeof runConfig != 'undefined') {
                    self.sliderOptions.options['floor'] = self.runConfig.minExecutors;
                    self.sliderOptions.options['ceil'] = self.runConfig.
                            maxExecutors;
                  } else {
                    self.sliderOptions.options['floor'] = 1;
                    self.sliderOptions.options['ceil'] = 300;
                  }
                  break;
                case "TENSORFLOW":
                  self.tensorflowState.selectedJar = filename;
                  TensorFlowService.inspectProgram(self.projectId, path).then(
                          function (success) {
                            self.runConfig = success.data;
                            self.mainFileSelected(filename);
                            if (self.tourService.currentStep_TourFour > -1) {
                              self.tourService.currentStep_TourFour = 6;
                            }
                          }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                  });
                  break;
                default:
                  break;
              }
            };
            

            $window.uploadDone = function () {
              stopLoading();
            }

            $scope.trustSrc = function (src) {
              return $sce.trustAsResourceUrl(self.ui);
            };

            self.tensorflow = function () {
              $scope.mode = "tensorflow"
            }
            self.spark = function () {
              $scope.mode = "spark"
            }

            self.restart = function () {
              $location.path('/#!/project/' + self.projectId + '/jupyter');
            }



            var init = function () {
              JupyterService.running(projectId).then(
                      function (success) {
                        self.config = success.data;
                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');			  
                        self.toggleValue = true;
                      }, function (error) {
                        JupyterService.settings(projectId).then(
                         function (success) {
                           self.val = success.data;
                           self.toggleValue = true;
                         }, function (error) {
                           growl.error("Could not stop the Jupyter Notebook Server.");
                         }
                        );
                     }
              );

            };


            var startLoading = function (label) {
              self.loading = true;
              self.loadingText = label;
            };
            var stopLoading = function () {
              self.loading = false;
              self.loadingText = "";
            };

            self.goBack = function () {
              $window.history.back();
            };

            self.stop = function () {
              startLoading("Stopping Jupyter...");

              JupyterService.stop(projectId).then(
                      function (success) {
                        self.ui = "";
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );



            };

            self.stopDataOwner = function (hdfsUsername) {
              startLoading("Stopping Jupyter...");
              JupyterService.stopDataOwner(projectId, hdfsUsername).then(
                      function (success) {
                        self.ui = ""
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );
            };
            self.stopAdmin = function (hdfsUsername) {
              startLoading("Stopping Jupyter...");
              JupyterService.stopAdmin(projectId, hdfsUsername).then(
                      function (success) {
                        self.ui = ""
                        stopLoading();
                      }, function (error) {
                growl.error("Could not stop the Jupyter Notebook Server.");
                stopLoading();
              }
              );
            };

            var load = function () {
              $scope.tgState = true;
            };

            init();



            var start = function () {
              startLoading("Connecting to Jupyter...");
              $scope.tgState = true;

              JupyterService.start(projectId, self.config).then(
                      function (success) {
                        self.toggleValue = true;
                        self.config = success.data;

                        self.ui = "/hopsworks-api/jupyter/" + self.config.port + "/?token=" + self.config.token;
                        $window.open(self.ui, '_blank');
                        $timeout(stopLoading(), 5000);

                      }, function (error) {
                growl.error("Could not start Jupyter.");
                stopLoading();
                self.toggleValue = true;
              }
              );

            };


            var configure = function () {
              var val = {};
              val.driverMemory = "500M";
              val.executorMemory = "500M";
              val.gpus = 1;
              val.driverCores = 1;
              val.executorCores = 1;
              val.archives = "";
              val.jars = "";
              val.files = "";
              val.pyFiles = "";
//              ModalService.jupyterConfig('md', '', '', val).then(
//                      function (success) {
//                        self.config = success.val;
//                        start();
//                      },
//                      function (error) {
//                        growl.error("Could not activate Jupyter.");
//                        self.toggleValue = true;
//
//                      });

            };


          }]);
