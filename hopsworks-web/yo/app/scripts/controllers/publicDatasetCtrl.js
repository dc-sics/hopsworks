'use strict';

angular.module('hopsWorksApp')
        .controller('PublicDatasetCtrl', ['$location', '$anchorScroll', '$scope',
          '$showdown', 'md5', 'ModalService', 'PublicDatasetService', 'DelaGService',
          function ($location, $anchorScroll, $scope, $showdown, md5, ModalService,
                  PublicDatasetService, DelaGService) {
            var self = this;
            self.comments;
            self.readme;
            self.selectedList;
            self.displayCategories;
            self.selectedCategory;
            self.selectedDataset;
            self.userRating = 0;
            self.myUserId;
            self.commentEditable = false;
            self.newComment;
            self.updateComment;

            var getUser = function () {
              PublicDatasetService.getUserId().then(function (success) {
                console.log("getUser", success);
                self.myUserId = success.data;
              }, function (error) {
                self.myUserId = undefined;
                console.log("getUser", error);
              });
            };

            var getDisplayCategories = function () {
              PublicDatasetService.getDisplayCategories().then(function (success) {
                console.log("getDisplayCategories", success);
                self.displayCategories = success.data;
              }, function (error) {
                self.displayCategories = [];
                console.log("getDisplayCategories", error);
              });
            };

            var initCtrl = function () {
              getUser();
              getDisplayCategories();
            };

            initCtrl();

            self.selectDisplayCategory = function (category) {
              console.log("selectDisplayCategory", category);
              self.selectedDataset = undefined;
              self.selectedCategory = category;
              doGet(category.categoryName);
            };

            self.selectItem = function (selectItem) {
              self.selectedDataset = selectItem;
              self.selectedSubCategory = undefined;
              getBasicReadme(self.selectedDataset.publicId);
              getComments(self.selectedDataset.publicId);
              getUserRating(self.selectedDataset.publicId);
            };

            var doGet = function (type) {
              PublicDatasetService.doGet(type).then(function (success) {
                console.log("doGet", success);
                self.selectedList = success.data;
                self.selectedSubCategoryList = success.data;
              }, function (error) {
                self.selectedList = [];
                self.selectedSubCategoryList = [];
                console.log("doGet", error);
              });
            };

            var getBasicReadme = function (publicId) {
              DelaGService.getDetails(publicId).then(function (success) {
                getreadme(publicId, success.data.bootstrap);
              }, function (error) {
                self.readme = "No details found.";
                console.log("getBasicReadme", error);
              });
            };
            
            var getreadme = function (publicId, bootstrap) {
              // DelaGService.getReadme(publicDSId, peers)
              DelaGService.getReadme(publicId, bootstrap).then(function (success) {
                console.log("getreadme", success);
                self.readme = $showdown.makeHtml(success.data.content);
              }, function (error) {
                self.readme = "No readme found.";
                console.log("getreadme", error);
              });
            };

            var getComments = function (publicId) {
              PublicDatasetService.getComments(publicId).then(function (success) {
                console.log("getComments", success);
                self.comments = success.data;
              }, function (error) {
                self.comments = [];
                console.log("getComments", error);
              });
            };

            var getUserRating = function (publicId) {
              PublicDatasetService.getUserRating(publicId).then(function (success) {
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
              PublicDatasetService.postComment(self.selectedDataset.publicId, self.newComment).then(function (success) {
                console.log("saveComment", success);
                self.newComment = '';
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            self.deleteComment = function (commentId) {
              PublicDatasetService.deleteComment(self.selectedDataset.publicId, commentId).then(function (success) {
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
              PublicDatasetService.updateComment(self.selectedDataset.publicId, commentId, self.updateComment).then(function (success) {
                console.log("saveComment", success);
                $('#commentdiv').css("background-color", "#FFF");
                self.updateComment = '';
                getComments(self.selectedDataset.publicId);
              }, function (error) {
                console.log("saveComment", error);
              });
            };
            
            var postCommentIssue = function (commentId, commentIssue) {
              PublicDatasetService.postCommentIssue(self.selectedDataset.publicId, commentId, commentIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postDatasetIssue = function (publicId, datasetIssue) {
              PublicDatasetService.postDatasetIssue(publicId, datasetIssue).then(function (success) {
                console.log("postCommentIssue", success);
              }, function (error) {
                console.log("postCommentIssue", error);
              });
            };
            
            var postRating = function (publicDSId, rating) {
              PublicDatasetService.postRating(publicDSId, rating).then(function (success) {
                console.log("postRating", success);
              }, function (error) {
                console.log("postRating", error);
              });
            };

            var init = function () {
              $('.keep-open').on({
                'shown.bs.dropdown': function () {
                  $(this).attr('closable', false);
                },
                'click': function () { },
                'hide.bs.dropdown': function () {
                  return $(this).attr('closable') === 'true';
                }
              });

              $('.keep-open #dLabel').on({
                'click': function () {
                  $(this).parent().attr('closable', true);
                }
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

          }]);


