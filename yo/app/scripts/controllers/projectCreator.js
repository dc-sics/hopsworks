'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectCreatorCtrl', ['$modalInstance', '$scope', 'ProjectService', 'UserService', 'growl',
          function ($modalInstance, $scope, ProjectService, UserService, growl) {

            var self = this;
            
            self.working = false;
            self.card = {};
            self.myCard = {};
            self.cards = [];

            self.projectMembers = [];
            self.projectTeam = [];
            self.projectTypes = ['JOBS', 'ZEPPELIN'];
//            self.projectTypes = ['JOBS', 'ZEPPELIN', 'BIOBANKING', 'CHARON', 'SSH']; 

            self.selectionProjectTypes = ['JOBS', 'ZEPPELIN'];
            self.projectName = '';
            self.projectDesc = '';

            self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#).*$/;


            UserService.profile().then(
                    function (success) {
                      if (success.data.email != undefined) {
                        self.myCard.email = success.data.email;
                        if (success.data.firstName != undefined) {
                          self.myCard.firstname = success.data.firstName;
                          if (success.data.email != undefined) {
                            self.myCard.lastname = success.data.lastName;
                            UserService.allcards().then(
                                    function (success) {
                                      self.cards = success.data;
                                      // remove my own 'card' from the list of members
                                      self.cards.splice(self.cards.indexOf(self.myCard), 1);
                                    }, function (error) {
                              self.errorMsg = error.data.msg;
                            });
                          }
                        }
                      }

                    },
                    function (error) {
                      self.errorMsg = error.data.errorMsg;
                    });


            $scope.$watch('projectCreatorCtrl.card.selected', function (selected) {
              var projectTeamPK = {'projectId': "", 'teamMember': ""};
              var projectTeam = {'projectTeamPK': projectTeamPK};
              if (selected !== undefined) {
                projectTeamPK.teamMember = selected.email;
                if (self.projectMembers.indexOf(selected.email) == -1) {
                  self.projectMembers.push(selected.email);
                  self.projectTeam.push(projectTeam);
                }
                self.card.selected = undefined;
              }
            });


            self.exists = function exists(projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);

              if (idx > -1) {
                self.selectionProjectTypes.splice(idx, 1);
              } else {
                self.selectionProjectTypes.push(projectType);
              }
            };


            self.removeMember = function (member) {
              self.projectMembers.splice(self.projectMembers.indexOf(member), 1);
            };
                   
            self.createProject = function () {
              self.working = true;
              $scope.newProject = {
                'projectName': self.projectName,
                'description': self.projectDesc,
                'retentionPeriod': "",
                'status': 0,
                'services': self.selectionProjectTypes,
                'projectTeam': self.projectTeam
              };

              ProjectService.save($scope.newProject).$promise.then(
                      function (success) {
                        self.working = false;
                        growl.success(success.successMessage, {title: 'Success', ttl: 2000});
                        if (success.errorMsg) {
                          growl.warning(success.errorMsg, {title: 'Error', ttl: 10000});
                        }
                        if (success.fieldErrors.length > 0) {
                          success.fieldErrors.forEach(function (entry) {
                            growl.warning(entry + ' could not be added', {title: 'Error', ttl: 10000});
                          });

                        }
                        $modalInstance.close($scope.newProject);
                      }, function (error) {
                self.working = false;
              }
              );
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

          }]);
