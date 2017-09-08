'use strict';
angular.module('hopsWorksApp')
        .factory('DelaGService', ['$http', function ($http) {
            var service = {
              search: function (searchTerm) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/search/' + searchTerm});
              },
              getDetails: function (publicDSId) {
                return $http({
                  method: 'GET',
                  url: '/api/dela/dataset/' + publicDSId + '/details'});
              },
              getReadme: function (publicDSId, peers) {
                return $http({
                  method: 'POST',
                  url: '/api/dela/dataset/' + publicDSId + '/readme',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: peers
                });
              },
              getUserContents: function () {
                return $http.get('/api/dela/user/contents');
              }
            };
            return service;
          }]);
