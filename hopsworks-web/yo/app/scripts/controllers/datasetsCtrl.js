'use strict';

angular.module('hopsWorksApp')
        .controller('DatasetsCtrl', ['$scope', '$q', '$mdSidenav', '$mdUtil', '$log',
          'DataSetService', '$routeParams', '$route', 'ModalService', 'growl', '$location',
          'MetadataHelperService', '$showdown', '$rootScope',
          function ($scope, $q, $mdSidenav, $mdUtil, $log, DataSetService, $routeParams,
                  $route, ModalService, growl, $location, MetadataHelperService,
                  $showdown, $rootScope) {

            var self = this;
            self.itemsPerPage = 14;
            self.working = false;
            //Some variables to keep track of state.
            self.files = []; //A list of files currently displayed to the user.
            self.projectId = $routeParams.projectID; //The id of the project we're currently working in.
            self.pathArray; //An array containing all the path components of the current path. If empty: project root directory.
            self.sharedPathArray; //An array containing all the path components of a path in a shared dataset 
            self.highlighted;

            // Details of the currently selecte file/dir
            self.selected = null; //The index of the selected file in the files array.
//            self.fileDetail = null; //The details about the currently selected file.
            self.sharedPath = null; //The details about the currently selected file.
            self.routeParamArray = [];
            $scope.readme = null;
            var dataSetService = DataSetService(self.projectId); //The datasetservice for the current project.

            $scope.all_selected = false;
            self.selectedFiles = {}; //Selected files


            self.dir_timing;

            self.isPublic = undefined;
            self.shared = undefined;
            self.status = undefined;

            self.tgState = true;

            self.onSuccess = function (e) {
              growl.success("Copied to clipboard", {title: '', ttl: 1000});
              e.clearSelection();
            };

            self.metadataView = {};
            self.availableTemplates = [];
            self.closeSlider = false;
            self.breadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return 0;
              }
              var displayPathLen = 10;
              if (self.pathArray.length <= displayPathLen) {
                return self.pathArray.length - 1;
              }
              return displayPathLen;
            };

            self.cutBreadcrumbLen = function () {
              if (self.pathArray === undefined || self.pathArray === null) {
                return false;
              }
              if (self.pathArray.length - self.breadcrumbLen() > 0) {
                return true;
              }
              return false;
            };

            $scope.sort = function (keyname) {
              $scope.sortKey = keyname;   //set the sortKey to the param passed
              $scope.reverse = !$scope.reverse; //if true make it false and vice versa
            };

            /**
             * watch for changes happening in service variables from the other controller
             */
            $scope.$watchCollection(MetadataHelperService.getAvailableTemplates, function (availTemplates) {
              if (!angular.isUndefined(availTemplates)) {
                self.availableTemplates = availTemplates;
              }
            });


            $scope.$watch(MetadataHelperService.getDirContents, function (response) {
              if (response === "true") {
                getDirContents();
                MetadataHelperService.setDirContents("false");
              }
            });


            self.isSharedDs = function (name) {
              var top = name.split("::");
              if (top.length === 1) {
                return false;
              }
              return true;
            };

            self.isShared = function () {
              var top = self.pathArray[0].split("::");
              if (top.length === 1) {
                return false;
              }
              return true;
            };

            self.sharedDatasetPath = function () {
              var top = self.pathArray[0].split("::");
              if (top.length === 1) {
                self.sharedPathArray = [];
                return;
              }
              // /proj::shared_ds/path/to  -> /proj/ds/path/to
              // so, we add '1' to the pathLen
              self.sharedPathArray = new Array(self.pathArray.length + 1);
              self.sharedPathArray[0] = top[0];
              self.sharedPathArray[1] = top[1];
              for (var i = 1; i < self.pathArray.length; i++) {
                self.sharedPathArray[i + 1] = self.pathArray[i];
              }
              return self.sharedPathArray;
            };


            /*
             * Get all datasets under the current project.
             * @returns {undefined}
             */
            self.getAllDatasets = function () {
              //Get the path for an empty patharray: will get the datasets
              var path = getPath([]);
              dataSetService.getContents(path).then(
                      function (success) {
                        self.files = success.data;
                        self.pathArray = [];
                        console.log(success);
                      }, function (error) {
                console.log("Error getting all datasets in project " + self.projectId);
                console.log(error);
              });
            };


            /**
             * Get the contents of the directory at the path with the given path components and load it into the frontend.
             * @param {type} The array of path compontents to fetch. If empty, fetches the current path.
             * @returns {undefined}
             */
            var getDirContents = function (pathComponents) {
              //Construct the new path array
              var newPathArray;
              if (pathComponents) {
                newPathArray = pathComponents;
              } else if (self.routeParamArray) {
                newPathArray = self.pathArray.concat(self.routeParamArray);
              } else {
                newPathArray = self.pathArray;
              }
              //Convert into a path
              var newPath = getPath(newPathArray);
              self.files = [];
              self.working = true;
              self.dir_timing = new Date().getTime();
              //Get the contents and load them
              dataSetService.getContents(newPath).then(
                      function (success) {
                        //Clear any selections
                        self.all_selected = false;
                        self.selectedFiles = {};
                        self.selected = null;
                        //Reset the selected file
                        self.selected = null;
                        self.working = false;
                        //Set the current files and path
                        self.files = success.data;
                        self.pathArray = newPathArray;
                        console.log(success);
//                        alert('Execution time: ' + (new Date().getTime() - self.dir_timing)); 
                        console.log('Execution time: ' + (new Date().getTime() - self.dir_timing));
                        if ($rootScope.selectedFile) {
                          var filePathArray = self.pathArray.concat($rootScope.selectedFile);
                          self.getFile(filePathArray);
                          $rootScope.selectedFile = undefined;
                        }
                      }, function (error) {
                if (error.data.errorMsg.indexOf("Path is not a directory.") > -1) {
                  var popped = newPathArray.pop();
                  console.log(popped);
                  self.openDir({name: popped, dir: false, underConstruction: false});
                  self.pathArray = newPathArray;
                  self.routeParamArray = [];
                  //growl.info(error.data.errorMsg, {title: 'Info', ttl: 2000});
                  getDirContents();
                } else if (error.data.errorMsg.indexOf("Path not found :") > -1) {
                  self.routeParamArray = [];
                  //$route.updateParams({fileName:''});
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
                  getDirContents();
                }
                self.working = false;
                console.log("Error getting the contents of the path "
                        + getPath(newPathArray));
                console.log(error);
              });
            };

            self.getFile = function (pathComponents) {
              var newPathArray;

              newPathArray = pathComponents;

              //Convert into a path
              var newPath = getPath(newPathArray);
              dataSetService.getFile(newPath).then(
                      function (success) {
                        self.highlighted = success.data;
                        self.select(self.highlighted.name, self.highlighted, undefined);
                        $scope.search = self.highlighted.name;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
              });
            };


            var init = function () {
              //Check if the current dataset is set
              if ($routeParams.datasetName) {
                //Dataset is set: get the contents
                self.pathArray = [$routeParams.datasetName];
              } else {
                //No current dataset is set: get all datasets.
                self.pathArray = [];
              }
              if ($routeParams.datasetName && $routeParams.fileName) {
                //file name is set: get the contents
                var paths = $routeParams.fileName.split("/");
                paths.forEach(function (entry) {
                  if (entry !== "") {
                    self.routeParamArray.push(entry);
                  }
                });
              }
              getDirContents();

              self.tgState = true;
            };

            init();

            /**
             * Upload a file to the specified path.
             * @param {type} path
             * @returns {undefined}
             */
            var upload = function (path) {
              dataSetService.upload(path).then(
                      function (success) {
                        console.log("upload success");
                        console.log(success);
                        getDirContents();
                      }, function (error) {
                console.log("upload error");
                console.log(error);
              });
            };

            /**
             * Remove the inode at the given path. If called on a folder, will 
             * remove the folder and all its contents recursively.
             * @param {type} path. The project-relative path to the inode to be removed.
             * @returns {undefined}
             */
            var removeInode = function (path) {
              dataSetService.removeDataSetDir(path).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                        getDirContents();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            /**
             * Open a modal dialog for folder creation. The folder is created at the current path.
             * @returns {undefined}
             */
            self.newDataSetModal = function () {
              ModalService.newFolder('md', getPath(self.pathArray)).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                        getDirContents();
                      }, function (error) {
                //The user changed his/her mind. Don't really need to do anything.
//                getDirContents();
              });
            };

            /**
             * Delete the file with the given name under the current path. 
             * If called on a folder, will remove the folder 
             * and all its contents recursively.
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteFile = function (fileName) {
              var removePathArray = self.pathArray.slice(0);
              removePathArray.push(fileName);
              removeInode('file/' + getPath(removePathArray));
            };

            /**
             * Delete the dataset with the given name under the current path. 
             * @param {type} fileName
             * @returns {undefined}
             */
            self.deleteDataset = function (fileName) {
              var removePathArray = self.pathArray.slice(0);
              removePathArray.push(fileName);
              removeInode(getPath(removePathArray));
            };



            /**
             * Makes the dataset public for anybody within the local cluster or any outside cluster.
             * @param id inodeId
             */
            self.makePublic = function (id) {

              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet public? \n\
This will make all its files available for any registered user to download and process.').then(
                      function (success) {
                        dataSetService.makePublic(id).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'The DataSet is now Public.', ttl: 1500});
                                  getDirContents();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
                        });

                      }
              );

            };

            self.removePublic = function (id) {

              ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet private? \n\
This will make all its files unavailable to other projects unless you share it explicitly.').then(
                      function (success) {
                        dataSetService.removePublic(id).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'The DataSet is now Private.', ttl: 1500});
                                  getDirContents();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
                        });

                      }
              );
            };


            self.parentPathArray = function () {
              var newPathArray = self.pathArray.slice(0);
              var clippedPath = newPathArray.splice(1, newPathArray.length - 1);
              return clippedPath;
            };

            self.unzip = function () {
              var pathArray = self.pathArray.slice(0);
              pathArray.push(self.selected);
              var filePath = getPath(pathArray);

                growl.info("Started unzipping...", 
                {title: 'Unzipping Started', ttl: 2000, referenceId: 4});
                dataSetService.unzip(filePath).then(
                      function (success) {
                growl.success("Refresh your browser when finished", 
                {title: 'Unzipping in Background', ttl: 5000, referenceId: 4});
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error unzipping file', ttl: 5000, referenceId: 4});
              }); 
            };

            self.isZippedfile = function () {

// https://stackoverflow.com/questions/680929/how-to-extract-extension-from-filename-string-in-javascript
              var re = /(?:\.([^.]+))?$/;
              var ext = re.exec(self.selected)[1];
              switch (ext) {
                case "zip":
                  return true; 
                case "rar":
                  return true;
                case "tar":
                  return true;
                case "tgz":
                  return true;
                case "gz":
                  return true;
                case "bz2":
                  return true;
                case "7z":
                  return true;
              }

              return false;
            };


            /**
             * Preview the requested file in a Modal. If the file is README.md
             * and the preview flag is false, preview the file in datasets.
             * @param {type} dataset
             * @param {type} preview
             * @returns {undefined}
             */
            self.filePreview = function (dataset, preview, readme) {
              var fileName = "";
              //handle README.md filename for datasets browser viewing here
              if (readme && !preview) {
                if (dataset.shared === true) {
                  fileName = dataset.selectedIndex + "/README.md";
                } else {
                  fileName = dataset.path.substring(dataset.path.lastIndexOf('/')).replace('/', '') + "/README.md";
                }
              } else {
                fileName = dataset;
              }

              var previewPathArray = self.pathArray.slice(0);
              previewPathArray.push(fileName);
              var filePath = getPath(previewPathArray);
              //If filename is README.md then try fetching it without the modal
              if (readme && !preview) {
                dataSetService.filePreview(filePath, "head").then(
                        function (success) {
                          var fileDetails = JSON.parse(success.data.data);
                          var content = fileDetails.filePreviewDTO[0].content;
                          $scope.readme = $showdown.makeHtml(content);
                        }, function (error) {
                  //To hide README from UI
                  growl.error(error.data.errorMsg, {title: 'Error retrieving README file', ttl: 5000, referenceId: 4});
                  $scope.readme = null;
                });
              } else {
                ModalService.filePreview('lg', fileName, filePath, self.projectId, "head").then(
                        function (success) {

                        }, function (error) {
                });
              }
            };


            self.copy = function (inodeId, name) {
              ModalService.selectDir('lg', "/[^]*/", "problem selecting folder").then(function (success) {
                var destPath = success;
                // Get the relative path of this DataSet, relative to the project home directory
                // replace only first occurrence 
                var relPath = destPath.replace("/Projects/" + self.projectId + "/", "");
                var finalPath = relPath + "/" + name;

                dataSetService.copy(inodeId, finalPath).then(
                        function (success) {
                          getDirContents();
                          growl.success('', {title: 'Copied ' + name + ' successfully', ttl: 5000, referenceId: 4});
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: name + ' was not copied', ttl: 5000, referenceId: 4});
                });
              }, function (error) {
              });
            };


            self.copySelected = function () {
              //Check if we are to move one file or many
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                if (self.selected !== null && self.selected !== undefined) {
                  self.copy(self.selected.id, self.selected.name);
                }
              } else if (Object.keys(self.selectedFiles).length !== 0 && self.selectedFiles.constructor === Object) {

                ModalService.selectDir('lg', "/[^]*/", "problem selecting folder").then(
                        function (success) {
                          var destPath = success;
                          // Get the relative path of this DataSet, relative to the project home directory
                          // replace only first occurrence 
                          var relPath = destPath.replace("/Projects/" + self.projectId + "/", "");
                          //var finalPath = relPath + "/" + name;
                          var names = [];
                          var i = 0;
                          //Check if have have multiple files 
                          for (var name in self.selectedFiles) {
                            names[i] = name;
                            i++;
                          }

                          for (var name in self.selectedFiles) {
                            dataSetService.copy(self.selectedFiles[name].id, relPath + "/" + name).then(
                                    function (success) {
                                      //If we copied the last file
                                      if (name === names[names.length - 1]) {
                                        getDirContents();
                                        for (var i = 0; i < names.length; i++) {
                                          delete self.selectedFiles[names[i]];
                                        }
                                        self.all_selected = false;
                                      }
                                      //growl.success('',{title: 'Copied successfully', ttl: 5000, referenceId: 4});
                                    }, function (error) {
                              growl.error(error.data.errorMsg, {title: name + ' was not copied', ttl: 5000, referenceId: 4});
                            });
                          }
                        }, function (error) {
                  //The user changed their mind.
                });
              }
            };


            self.move = function (inodeId, name) {
              ModalService.selectDir('lg', "/[^]*/",
                      "problem selecting folder").then(
                      function (success) {
                        var destPath = success;
                        // Get the relative path of this DataSet, relative to the project home directory
                        // replace only first occurrence 
                        var relPath = destPath.replace("/Projects/" + self.projectId + "/", "");
                        var finalPath = relPath + "/" + name;

                        dataSetService.move(inodeId, finalPath).then(
                                function (success) {
                                  getDirContents();
                                  growl.success(success.data.successMessage, {title: 'Moved successfully. Opened dest dir: ' + relPath, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: name + ' was not moved', ttl: 5000, referenceId: 4});
                        });
                      }, function (error) {
              });

            };


            self.isSelectedFiles = function () {
              return Object.keys(self.selectedFiles).length;
            };

            self.moveSelected = function () {
              //Check if we are to move one file or many
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                if (self.selected !== null && self.selected !== undefined) {
                  self.move(self.selected.id, self.selected.name);
                }
              } else if (Object.keys(self.selectedFiles).length !== 0 && self.selectedFiles.constructor === Object) {

                ModalService.selectDir('lg', "/[^]*/",
                        "problem selecting folder").then(
                        function (success) {
                          var destPath = success;
                          // Get the relative path of this DataSet, relative to the project home directory
                          // replace only first occurrence 
                          var relPath = destPath.replace("/Projects/" + self.projectId + "/", "");
                          //var finalPath = relPath + "/" + name;
                          var names = [];
                          var i = 0;
                          //Check if have have multiple files 
                          for (var name in self.selectedFiles) {
                            names[i] = name;
                            i++;
                          }

                          for (var name in self.selectedFiles) {
                            dataSetService.move(self.selectedFiles[name].id, relPath + "/" + name).then(
                                    function (success) {
                                      //If we moved the last file
                                      if (name === names[names.length - 1]) {
                                        getDirContents();
                                        for (var i = 0; i < names.length; i++) {
                                          delete self.selectedFiles[names[i]];
                                        }
                                        self.all_selected = false;
                                      }
                                    }, function (error) {
                              growl.error(error.data.errorMsg, {title: name + ' was not moved', ttl: 5000, referenceId: 4});
                            });
                          }
                        }, function (error) {
                  //The user changed their mind.
                });
              }
            };

            var renameModal = function (inodeId, name) {
              var pathComponents = self.pathArray.slice(0);
              var newPath = getPath(pathComponents);
              var destPath = newPath + '/';
              ModalService.enterName('sm', "Rename File or Directory", name).then(
                      function (success) {
                        var fullPath = destPath + success.newName;
                        dataSetService.move(inodeId, fullPath).then(
                                function (success) {
                                  getDirContents();
                                  self.all_selected = false;
                                  self.selectedFiles = {};
                                  self.selected = null;
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
                          self.all_selected = false;
                          self.selectedFiles = {};
                          self.selected = null;
                        });
                      });
            };

            self.rename = function (inodeId, name) {
              renameModal(inodeId, name);
            };
            self.renameSelected = function () {
              if (self.isSelectedFiles() === 1) {
                var inodeId, inodeName;
                for (var name in self.selectedFiles) {
                  inodeName = name;
                }
                inodeId = self.selectedFiles[inodeName]['id'];
                renameModal(inodeId, inodeName);
              }
            };
            /**
             * Opens a modal dialog for file upload.
             * @returns {undefined}
             */
            self.uploadFile = function () {
              var templateId = -1;

              ModalService.upload('lg', self.projectId, getPath(self.pathArray), templateId).then(
                      function (success) {
                        growl.success(success, {ttl: 5000});
                        getDirContents();
                      }, function (error) {
//                growl.info("Closed without saving.", {title: 'Info', ttl: 5000});
                getDirContents();
              });
            };

            /**
             * Sends a request to erasure code a file represented by the given path.
             * It checks
             * .. if the given path resolves to a file or a dir
             * .. if the given path is an existing file
             * .. if the given file is large enough (comprises more than 10 blocks)
             * 
             * If all of the above are met, the compression takes place in an asynchronous operation
             * and the user gets notified when it finishes via a message
             * 
             * @param {type} file
             * @returns {undefined}
             */
            self.compress = function (file) {
              var pathArray = self.pathArray.slice(0);
              pathArray.push(file.name);
              var filePath = getPath(pathArray);

              //check if the path is a dir
              dataSetService.isDir(filePath).then(
                      function (success) {
                        var object = success.data.successMessage;
                        switch (object) {
                          case "DIR":
                            ModalService.alert('sm', 'Alert', 'You can only compress files');
                            break;
                          case "FILE":
                            //if the path is a file go on
                            dataSetService.checkFileExist(filePath).then(
                                    function (successs) {
                                      //check the number of blocks in the file
                                      dataSetService.checkFileBlocks(filePath).then(
                                              function (successss) {
                                                var noOfBlocks = parseInt(successss.data);
                                                console.log("NO OF BLOCKS " + noOfBlocks);
                                                if (noOfBlocks >= 10) {
                                                  ModalService.alert('sm', 'Confirm', 'This operation is going to run in the background').then(
                                                          function (modalSuccess) {
                                                            console.log("FILE PATH IS " + filePath);
                                                            dataSetService.compressFile(filePath);
                                                          });
                                                } else {
                                                  growl.error("The requested file is too small to be compressed", {title: 'Error', ttl: 5000, referenceId: 4});
                                                }
                                              }, function (error) {
                                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
                                      });
                                    });
                        }

                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 4});
              });
            };

            /**
             * Opens a modal dialog for sharing.
             * @returns {undefined}
             */
            self.share = function (name) {
              ModalService.shareDataset('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Opens a modal dialog to make dataset editable
             * @returns {undefined}
             */
            self.makeEditable = function (name) {
              ModalService.makeEditable('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Opens a modal dialog to remove editable from the dataset 
             * @returns {undefined}
             */
            self.removeEditable = function (name) {
              ModalService.removeEditable('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Opens a modal dialog for unsharing.
             * @returns {undefined}
             */
            self.unshare = function (name) {
              ModalService.unshareDataset('md', name).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'Success', ttl: 5000});
                        getDirContents();
                      }, function (error) {
              });
            };

            /**
             * Upon click on a inode in the browser:
             *  + If folder: open folder, fetch contents from server and display.
             *  + If file: open a confirm dialog prompting for download.
             * @param {type} file
             * @returns {undefined}
             */
            self.openDir = function (file) {
              if (file.dir) {
                var newPathArray = self.pathArray.slice(0);
                newPathArray.push(file.name);
                getDirContents(newPathArray);
              } else if (!file.underConstruction) {
                ModalService.confirm('sm', 'Confirm', 'Do you want to download this file?').then(
                        function (success) {
                          var downloadPathArray = self.pathArray.slice(0);
                          downloadPathArray.push(file.name);
                          var filePath = getPath(downloadPathArray);
                          dataSetService.checkFileForDownload(filePath).then(
                                  function (success) {
                                    dataSetService.fileDownload(filePath);
                                  }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                          });
                        }
                );
              } else {
                growl.info("File under construction.", {title: 'Info', ttl: 5000});
              }
            };

            /**
             * Go up to parent directory.
             * @returns {undefined}
             */
            self.back = function () {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.pop();
              if (newPathArray.length === 0) {
                $location.path('/project/' + self.projectId + '/datasets');
              } else {
                getDirContents(newPathArray);
              }
            };
            self.goToDataSetsDir = function () {
              $location.path('/project/' + self.projectId + '/datasets');
            };
            /**
             * Go to the folder at the index in the pathArray array.
             * @param {type} index
             * @returns {undefined}
             */
            self.goToFolder = function (index) {
              var newPathArray = self.pathArray.slice(0);
              newPathArray.splice(index, newPathArray.length - index);
              getDirContents(newPathArray);
            };

            self.menustyle = {
              "opacity": 0.2
            };

            /**
             * Select an inode; updates details panel.
             * @param {type} selectedIndex
             * @param {type} file
             * @returns {undefined}
             */
            self.select = function (selectedIndex, file, event) {

              // 1. Turn off the selected file at the top of the browser.
              // Add existing selected file (idempotent, if already added)
              // If file already selected, deselect it.
              if (event && event.ctrlKey) {

              } else {
                self.selectedFiles = {};
              }
              if (self.isSelectedFiles() > 0) {
                self.selected = null;
              } else {
                self.selected = file.name;
              }
              self.selectedFiles[file.name] = file;
              self.selectedFiles[file.name].selectedIndex = selectedIndex;
              self.menustyle.opacity = 1.0;
              console.log(self.selectedFiles);
            };

            self.haveSelected = function (file) {
              if (file === undefined || file === null || file.name === undefined || file.name === null) {
                return false;
              }
              if (file.name in self.selectedFiles) {
                return true;
              }
              return false;
            };


            self.selectAll = function () {
              var i = 0;
              var min = Math.min(self.itemsPerPage, self.files.length);
              for (i = 0; i < min; i++) {
                var f = self.files[i];
                self.selectedFiles[f.name] = f;
                self.selectedFiles[f.name].selectedIndex = i;
              }
              self.menustyle.opacity = 1;
              self.selected = null;
              self.all_selected = true;
              if (Object.keys(self.selectedFiles).length === 1
                      && self.selectedFiles.constructor === Object) {
                self.selected = Object.keys(self.selectedFiles)[0];
              }
            };

            //TODO: Move files to hdfs trash folder
            self.trashSelected = function () {

            };

            self.deleteSelected = function () {
              var i = 0;
              var names = [];
              for (var name in self.selectedFiles) {
                names[i] = name;
                self.deleteFile(name);
              }
              for (var i = 0; i < names.length; i++) {
                delete self.selectedFiles[names[i]];
              }
              self.all_selected = false;
              self.selectedFiles = {};
              self.selected = null;
            };


            self.deselect = function (selectedIndex, file, event) {
              var i = 0;
              if (Object.keys(self.selectedFiles).length === 1 && self.selectedFiles.constructor === Object) {
                for (var name in self.selectedFiles) {
                  if (file.name === name) {
                    delete self.selectedFiles[name];
                    //break;
                  }
                }
              } else {
                if (event.ctrlKey) {
                  for (var name in self.selectedFiles) {
                    if (file.name === name) {
                      delete self.selectedFiles[name];
                      break;
                    }
                  }
                } else {
                  for (var name in self.selectedFiles) {
                    if (file.name !== name) {
                      delete self.selectedFiles[name];
                      //break;
                    }
                  }
                }
              }
              if (Object.keys(self.selectedFiles).length === 0 && self.selectedFiles.constructor === Object) {
                self.menustyle.opacity = 0.2;
                self.selected = null;
              } else if (Object.keys(self.selectedFiles).length === 1 && self.selectedFiles.constructor === Object) {
                self.menustyle.opacity = 1.0;
                self.selected = Object.keys(self.selectedFiles)[0];
              }
              self.all_selected = false;

            };

            self.deselectAll = function () {
              self.selectedFiles = {};
              self.selected = null;
              self.sharedPath = null;
              self.menustyle.opacity = 0.2;
            };

            self.toggleLeft = buildToggler('left');
            self.toggleRight = buildToggler('right');

            function buildToggler(navID) {
              var debounceFn = $mdUtil.debounce(function () {
                $mdSidenav(navID).toggle()
                        .then(function () {
                          MetadataHelperService.fetchAvailableTemplates()
                                  .then(function (response) {
                                    self.availableTemplates = JSON.parse(response.board).templates;
                                  });
                        });
              }, 300);
              return debounceFn;
            };
            
            self.getSelectedPath = function (selectedFile) {
              if (self.isSelectedFiles() !== 1) {
                return "";
              }
              return "hdfs://" + selectedFile.path;
            };

          }]);

/**
 * Turn the array <i>pathArray</i> containing, path components, into a path string.
 * @param {type} pathArray
 * @returns {String}
 */
var getPath = function (pathArray) {
  return pathArray.join("/");
};
