'use strict';

angular.module('hopsWorksApp')
        .factory('UserService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            return {
              UpdateProfile: function (user) {
                return $http.post('/api/user/updateProfile', TransformRequest.jQueryStyle(user));
              },
              profile: function () {
                return $http.get('/api/user/profile');
              },
              changeLoginCredentials: function (newCredentials) {
                return $http.post('/api/user/changeLoginCredentials', TransformRequest.jQueryStyle(newCredentials));
              },
              allcards: function () {
                return $http.get('/api/user/allcards');
              },
              createProject: function (newProject) {
                return $http.post('/api/user/newProject', newProject);
              },
              getRole: function (projectId) {
                return $http.post('/api/user/getRole', "projectId=" + projectId);
              },
              changeTwoFactor: function (newCredentials) {
                return $http.post('/api/user/changeTwoFactor', TransformRequest.jQueryStyle(newCredentials));
              },
              getQR: function (pwd) {
                return $http.post('/api/user/getQRCode', "password=" + pwd);
              },
              addSshKey: function (sshKey) {
              //addSshKey: function (name, sshKey) {
                return $http({
                  method: 'post',
                  url: '/api/user/addSshKey',
                  headers: {'Content-Type': 'application/json'},
                  isArray: false,
                  data: sshKey
                });

                //return $http.post('/api/user/addSshKey', "name=" + name + "&sshKey=" + sshKey);
              },
              removeSshKey: function (name) {
                return $http.post('/api/user/removeSshKey', "name="+name);
              },
              getSshKeys: function () {
                return $http.get('/api/user/getSshKeys');
              }
            };
          }]);
