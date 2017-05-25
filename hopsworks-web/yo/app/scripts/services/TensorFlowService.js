'use strict';
/*
 * Service responsible for communicating with the Spark backend.
 */
angular.module('hopsWorksApp')

        .factory('TensorFlowService', ['$http', function ($http) {
            var service = {
              /**
               * Inspect the python at the given path.
               * @param {int} projectId
               */
              inspectJar: function (projectId, path) {
                return $http.get('/api/project/' + projectId + '/jobs/tensorflow/inspect/' + path);
              }
            };
            return service;
          }]);
