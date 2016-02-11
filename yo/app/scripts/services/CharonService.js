/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('CharonService', ['$http', function ($http) {
        return function (id) {
          var services = {
            copyFromHdfsToCharon: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/fromHDFS',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
            copyFromCharonToHdfs: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/toHDFS',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
            getMySiteId: function () {
              return $http.get('/api/project/' + id + '/charon/mySiteID');
            },
            listSiteIds: function () {
              return $http.get('/api/project/' + id + '/charon/listSiteIds');
            },
            listShares: function () {
              return $http.get('/api/project/' + id + '/charon/listSharedRepos');
            },
            addSiteId: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/addSiteId',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
	        removeSiteId: function (siteId) {
              return $http.delete('/api/project/' + id + '/charon/removeSiteId/' + siteId);
            },
	        removeShare: function (op) {
             var removeReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/removeShare',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };
              return $http(removeReq);			  
            },			
            mkdir: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/mkdir',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
            createSharedRepository: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/createSharedRepository',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
	        importRepo: function (token) {
              return $http.get('/api/project/' + id + '/charon/importRepo/' + token);
            },			
            share: function (op) {
              var regReq = {
                method: 'POST',
                url: '/api/project/' + id + '/charon/share',
                headers: {
                  'Content-Type': 'application/json'
                },
                data: op
              };

              return $http(regReq);
            },
          };
          return services;
        };
      }]);
