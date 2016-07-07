angular.module('hopsWorksApp')
        .controller('CreateAclCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 'topicName',
          function ($modalInstance, KafkaService, growl, projectId, topicName) {

            var self = this;
            self.users =[];
            self.projectId = projectId;
            self.topicName = topicName;
            self.permission_type;
            
            self.project;
            self.user_email = "admin@kth.se";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";
            
            
            self.init = function() {
                KafkaService.aclUsers(self.projectId, self.topicName).then(
                    function (success) {
                        self.users = success.data;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Could not load ACL users', ttl: 5000, referenceId: 21});
                   });
            };
            
            self.init();
            
            self.createTopicAcl = function () {
                
                var acl = {};
//              acl.topic_name = self.topicName;
//              acl.project_id = self.projectId;
              acl.projectName = self.project.projectName;
              acl.role = self.role;
              acl.userEmail = self.userEmail;
              acl.permissionType = self.permission_type;
              acl.operationType = self.operation_type;
              acl.host = self.host;

              KafkaService.createTopicAcl(self.projectId, topicName, acl).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to add topic', ttl: 5000});
              });
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

