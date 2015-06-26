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
              }


            };
          }]);
