'use strict';

angular.module('hopsWorksApp')
        .factory('PublicDatasetService', ['$http', function ($http) {
            var service = {
              getServiceInfo: function (serviceName) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/serviceInfo/' + serviceName
                });
              },
              getClusterId: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/clusterId'
                });
              },
              getAll: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset'
                });
              },
              getDataset: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/' + publicDSId
                });
              },
              getLocalDatasetByPublicId: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/localByPublicId/' + publicDSId
                });
              },
              getUserId: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/userId'
                });
              },
              doGet: function (type) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/' + type
                });
              },
              getTopRated: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/topRated'
                });
              },
              getNew: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/new'
                });
              },
              getDisplayCategories: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/displayCategories'
                });
              },
              getCategories: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/categories'
                });
              },
              getByCategory: function (category) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/byCategory/' + category
                });
              },
              postDatasetIssue: function (publicDSId, issue) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/issue',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: issue
                });
              },
              getComments: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/comments'
                });
              },
              postComment: function (publicDSId, comment) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/comments',
                  data: comment
                });
              },
              updateComment: function (publicDSId, commentId, comment) {
                return $http({
                  method: 'put',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/comments/' + commentId,
                  data: comment
                });
              },
              deleteComment: function (publicDSId, commentId) {
                return $http({
                  method: 'delete',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/comments/' + commentId
                });
              },
              postCommentIssue: function (publicDSId, commentId, issue) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/comments/report/' + commentId,
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: issue
                });
              },
              getRating: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/rating'
                });
              },
              getUserRating: function (publicDSId, user) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/rating/user'
                });
              },
              postRating: function (publicDSId, rating) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/publicDataset/' + publicDSId + '/rating/' + rating
                });
              },
              getReadmeByInode: function (inodeId) {
                return $http.get('/api/project/readme/byInodeId/' + inodeId);
              }
            };
            return service;
          }]);


