'use strict';

angular.module('hopsWorksApp')
        .factory('JupyterService', ['$http', function ($http) {
            return {
              getAll: function () {
                return $http.get('/api/jupyter/'+ getCookie('projectID'));
              },
              getByUser: function (hdfsUser) {
                return $http.put('/api/jupyter/'+ getCookie('projectID') + '/running' );
              },
              deleteByUser: function (hdfsUser) {
                return $http.delete('/api/jupyter/'+ getCookie('projectID') + '/stop');
              },
              startServer: function (hdfsUser) {
                return $http.get('/api/jupyter/'+ getCookie('projectID') + '/start' );
              },
              stopServer: function (hdfsUser) {
                return $http.delete('/api/jupyter/'+ getCookie('projectID') + '/stop' );
              }              
            };
          }]);
