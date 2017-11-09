'use strict';

angular.module('hopsWorksApp')
        .controller('HopsDatasetCtrl', ['$location', '$anchorScroll', '$scope', '$rootScope',
          'md5', 'ModalService', 'HopssiteService', 'DelaService', 'ProjectService', 'growl',
          function ($location, $anchorScroll, $scope, $rootScope, md5, ModalService,
                  HopssiteService, DelaService, ProjectService, growl) {
            var self = this;
            self.comments;
            self.readme;
            self.displayCategories;
            self.selectedCategory;
            self.selectedCategoryMap = {};
            self.selectedDataset;
            self.userRating = 0;
            self.myUserId;
            self.myClusterId;
            self.commentEditable = false;
            self.newComment;
            self.updateComment;
            self.publicDSId = $rootScope.publicDSId;
            $rootScope.publicDSId = undefined; //reset
            self.loadingDisplayCategories = false;
            self.loadingSelectedCategory = false;
            self.loadingReadme = false;
            self.loadingComments = false;

            var getUser = function () {
              HopssiteService.getUserId().then(function (success) {
                console.log("getUser", success);
                self.myUserId = success.data;
              }, function (error) {
                self.myUserId = undefined;
                console.log("getUser", error);
              });
            };
            
            var getClusterId = function () {
              HopssiteService.getClusterId().then(function (success) {
                console.log("getClusterId", success);
                self.myClusterId = success.data;
              }, function (error) {
                self.myClusterId = undefined;
                console.log("getClusterId", error);
              });
            };

            var getDisplayCategories = function () {
              self.loadingDisplayCategories = true;
              HopssiteService.getDisplayCategories().then(function (success) {
                console.log("getDisplayCategories", success);
                self.displayCategories = success.data;
                self.loadingDisplayCategories = false;
              }, function (error) {
                self.displayCategories = [];
                self.loadingDisplayCategories = false;
                console.log("getDisplayCategories", error);
              });
            };
            
            var getDataset = function (publicId) {
              HopssiteService.getDataset(publicId).then(function (success) {
                console.log("getDataset", success);
                self.selectedDataset = success.data;
                self.selectedSubCategory = undefined;
                getBasicReadme(publicId);
                getComments(publicId);
                getUserRating(publicId);
              }, function (error) {
                console.log("getDataset", error);
              });
            };
            
            var getDatasetFromLocal = function (inodeId) {
              ProjectService.getDatasetInfo({inodeId: inodeId}).$promise.then(
                function (response) {
                  console.log('getDatasetFromLocal: ', response);
                  self.selectedDataset = response;
                  getReadMeLocal(self.selectedDataset.inodeId);
                }, function (error) {
                  console.log('getDatasetFromLocal Error: ', error);
              });
            };

            var initCtrlDela = function () {
              getUser();
              getClusterId();
              getDisplayCategories();
              if (self.publicDSId !== undefined) {
                getDataset(self.publicDSId);
              }
            };
            
            var initCtrl = function () {
              self.displayCategories = [{'categoryName': 'all', 'displayName': 'All'}];
              if (self.publicDSId !== undefined) {
                getDatasetFromLocal(self.publicDSId);
              }
            };

            initCtrlDela();
            
            self.selectCategory = function (category) {
              console.log("selectDisplayCategory", category);
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
            };
            
            self.selectDisplayCategory = function (category) {
              console.log("selectDisplayCategory", category);
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              self.selectedCategoryMap[category.categoryName] = category;
              if ($rootScope.isDelaEnabled) {
                doGet(self.selectedCategoryMap[category.categoryName]);
              } else {
                doGetLocalCluster(self.selectedCategoryMap[category.categoryName]);
              }
            };

            self.selectItem = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getBasicReadme(self.selectedDataset.publicId);
              getComments(self.selectedDataset.publicId);
              getUserRating(self.selectedDataset.publicId);
            };
            
            self.selectDataset = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getReadMeLocal(selectItem.inodeId);
            };
            
            var doGetLocalCluster = function (category) {
              if (category['selectedList'] === undefined || category['selectedList'].lenght === 0) {
                self.loadingSelectedCategory = true;
              }
              
              DelaService.getLocalPublicDatasets().then(
                      function (success) {
                        category['selectedList'] = success;
                        category['selectedSubCategoryList'] = success.data;
                        self.loadingSelectedCategory = false;
                        console.log("doGetLocalCluster", success);
                      },
                      function (error) {
                        category['selectedList'] = [];
                        category['selectedSubCategoryList'] = [];
                        self.loadingSelectedCategory = false;
                        console.log("doGetLocalCluster", error);
                        growl.info("Could not load Public Datasets", {title: 'Info', ttl: 5000});
                      }
              );
            };

            var doGet = function (category) {
              if (category['selectedList'] === undefined || category['selectedList'].lenght === 0) {
                self.loadingSelectedCategory = true;
              }

              HopssiteService.getType(category.categoryName).then(function (success) {
                console.log("doGet", success);
                category['selectedList'] = success.data;
                category['selectedSubCategoryList'] = success.data;
                self.loadingSelectedCategory = false;
              }, function (error) {
                category['selectedList'] = [];
                category['selectedSubCategoryList'] = [];
                self.loadingSelectedCategory = false;
                console.log("doGet", error);
              });
            };

            var getBasicReadme = function (publicId) {
              DelaService.getDetails(publicId).then(function (success) {
                getreadme(publicId, success.data.bootstrap);
              }, function (error) {
                self.readme = "No details found.";
                console.log("getBasicReadme", error);
              });
            };
            
            var getReadMeLocal = function (inodeId) {
              self.readme = '';
              self.loadingReadme = true;
              HopssiteService.getReadmeByInode(inodeId).then(function (success) {
                console.log("getReadMeLocal", success);
                var conv = new showdown.Converter({parseImgDimensions: true});
                self.readme =  conv.makeHtml(success.data.content);
                self.loadingReadme = false;
              }, function (error) {
                self.readme = "No readme found.";
                self.loadingReadme = false;
                console.log("getReadMeLocal", error);
              });
            };
            
            var getreadme = function (publicId, bootstrap) {
              self.readme = '';
              self.loadingReadme = true;
              DelaService.getReadme(publicId, bootstrap).then(function (success) {
                console.log("getreadme", success);
                var conv = new showdown.Converter({parseImgDimensions: true});
                self.readme = conv.makeHtml(success.data.content);
                self.loadingReadme = false;
              }, function (error) {
                self.readme = "No readme found.";
                self.loadingReadme = false;
                console.log("getreadme", error);
              });
            };

            var getComments = function (publicId) {
              self.loadingComments = true;
              self.comments = [];
              HopssiteService.getComments(publicId).then(function (success) {
                console.log("getComments", success);
                self.comments = success.data;
                self.loadingComments = false;
              }, function (error) {
                self.comments = [];
                self.loadingComments = false;
                console.log("getComments", error);
              });
            };

            var getUserRating = function (publicId) {
              HopssiteService.getUserRating(publicId).then(function (success) {
                console.log("getUserRating", success);
                self.userRating = success.data.rate;
              }, function (error) {
                self.userRating = 1;
                console.log("getUserRating", error);
              });
            };           

            self.getEmailHash = function (email) {
              return md5.createHash(email || '');
            };

            self.gotoComment = function () {
              var old = $location.hash();
              $location.hash('commentbtn');
              $anchorScroll();
              $location.hash(old);
            };
            
            var getIssueObject = function (type, msg) {
                var issue = {};
                issue['type'] = type;
                issue['msg'] = msg;
                return issue;
            };

            self.reportAbuse = function (commentId) {
              ModalService.reportIssueModal('md', 'Report issue', '').then(function (success) {
                var issue = getIssueObject('CommentIssue', success);
                console.log(issue);
                postCommentIssue(commentId, issue);
              }, function (error) {
                console.log(error);
              });
            };
            
            self.rate = function () {
              postRating(self.selectedDataset.publicId, self.userRating);
            };
            
            
            self.reportDataset = function () {
              ModalService.reportIssueModal('md', 'Report issue', '').then(function (success) {
                var issue = getIssueObject('DatasetIssue', success);
                console.log(issue);
                postDatasetIssue(self.selectedDataset.publicId, issue);
              }, function (error) {
                console.log(error);
              });
            };

            self.commentMakeEditable = function () {
              self.commentEditable = !self.commentEditable;
              if (this.commentEditable) {
                $('#commentdiv').css("background-color", "#FFFFCC");
              }
            };

            self.postComment = function () {
              HopssiteService.postComment(self.selectedDataset.publicId, self.newComment).then(function (success) {
                console.log("saveComment", success);
                self.newComment = '';
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            self.deleteComment = function (commentId) {
              HopssiteService.deleteComment(self.selectedDataset.publicId, commentId).then(function (success) {
                console.log("deleteComment", success);
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("deleteComment", error);
              });
            };

            self.saveComment = function (commentId) {
              self.commentEditable = !self.commentEditable;
              self.updateComment = $('#commentdiv').text();
              //TODO (Ermias): delete if empty
              HopssiteService.updateComment(self.selectedDataset.publicId, commentId, self.updateComment).then(function (success) {
                console.log("saveComment", success);
                $('#commentdiv').css("background-color", "#FFF");
                self.updateComment = '';
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            var postCommentIssue = function (commentId, commentIssue) {
              HopssiteService.postCommentIssue(self.selectedDataset.publicId, commentId, commentIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postDatasetIssue = function (publicId, datasetIssue) {
              HopssiteService.postDatasetIssue(publicId, datasetIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postRating = function (publicDSId, rating) {
              HopssiteService.postRating(publicDSId, rating).then(function (success) {
                console.log("postRating", success);
              }, function (error) {
                console.log("postRating", error);
              });
            };
            
            self.addPublicDatasetModal = function (dataset) {
              var datasetDto = dataset;
              var projects;
              HopssiteService.getLocalDatasetByPublicId(dataset.publicId).then(function (response) {
                datasetDto = response.data;
                console.log('datasetDto: ', datasetDto);
                //fetch the projects to pass them in the modal.
                ProjectService.query().$promise.then(function (success) {
                  projects = success;
                  console.log('projects: ', projects);
                  //show dataset
                  ModalService.viewPublicDataset('md', projects, datasetDto).then(function (success) {
                    growl.success(success.data.successMessage, {title: 'Success', ttl: 1000, referenceId: 13});
                  }, function (error) {

                  });
                }, function (error) {
                  console.log('Error: ' + error);
                });

              }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000, referenceId: 13});
              });
            };


            var init = function () {
              $('.keep-open').on('shown.bs.dropdown', '.dropdown', function () {
                $(this).attr('closable', false);
              });

              $('.keep-open').on('click', '.dropdown', function () {
                console.log('.keep-open: click');
              });

              $('.keep-open').on('hide.bs.dropdown', '.dropdown', function () {
                return $(this).attr('closable') === 'true';
              });

              $('.keep-open').on('click', '#dLabel', function() {
                $(this).parent().attr('closable', true );
              });
              
              $(window).scroll(function () {
                if ($(this).scrollLeft() > 0) {
                  $('#publicdataset').css({'left': 45 - $(this).scrollLeft()});
                }
              });
              $(window).resize(function () {
                var w = window.outerWidth;
                if (w > 1280) {
                  $('#publicdataset').css({'left': 'auto'});
                }
              });
            };

            var overflowY = function (val) {
              $('#hwWrapper').css({'overflow-y': val});
            };

            self.setupStyle = function () {
              init();
              overflowY('hidden');
              $('#publicdatasetWrapper').css({'width': '1200px'});
            };

            self.overflowYAuto = function () {
              overflowY('auto');
              $('#publicdatasetWrapper').css({'width': '1500px'});
            };

            $scope.$on("$destroy", function () {
              overflowY('auto');
            });
            
            $scope.isSelected = function (name) {
              if (self.selectedCategory === undefined) {
                return false;
              }
              return self.selectedCategory.displayName === name;
            };
            
            self.sizeOnDisk = function (fileSizeInBytes) {
              if (fileSizeInBytes === undefined) {
                return '--';
              }
              return convertSize(fileSizeInBytes);
            };

          }]);


