'use strict';

angular.module('hopsWorksApp')

        .factory('DelaService', ['$http', function ($http) {
            return function (id) {
              var service = {
                publishByInodeId: function (inodeId) {
                  return $http.post('/api/project/' + id + '/dela/dataset/publish/inodeId/' + inodeId);
                },
                cancelByInodeId: function (inodeId) {
                  return $http.post('/api/project/' + id + '/dela/dataset/cancel/inodeId/' + inodeId);
                },
                cancelByPublicDSId: function (publicDSId) {
                  return $http.post('/api/project/' + id + '/dela/dataset/cancel/publicDSId/' + publicDSId);
                },
                cancelAndClean: function(publicDSId) {
                  return $http.post('/api/project/' + id + '/dela/dataset/cancelclean/publicDSId/' + publicDSId);
                },
                startDownload: function (json) {
                  return $http({
                    method: 'PUT',
                    url: '/api/project/' + id + '/dela/dataset/download/start',
                    data: json
                  });
                },
                downloadHdfs: function (json) {
                  return $http({
                    method: 'PUT',
                    url: '/api/project/' + id + '/dela/dataset/download/hdfs',
                    data: json
                  });
                },
                downloadKafka: function (json) {
                  return $http({
                    method: 'PUT',
                    url: '/api/project/' + id + '/dela/dataset/download/kafka',
                    data: json
                  });
                },
                getContents: function () {
                  return $http({
                    method: 'GET',
                    url: '/api/project/' + id + '/dela/contents'
                  });
                },
                getExtendedDetails: function (json) {
                  return $http({
                    method: 'PUT',
                    url: '/api/project/' + id + '/dela/details',
                    data: json
                  });
                },
                showManifest: function (inodeId) {
                  return $http.get('/api/project/' + id + '/dela/dataset/manifest/inodeId/' + inodeId);
                }
              };
              return service;
            };
          }]);
