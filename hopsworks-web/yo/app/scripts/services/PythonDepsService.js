/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/**
 * @ngdoc service
 * @name hopsWorksApp.PythonDepsService
 * @description
 * # PythonDepsService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
        .factory('PythonDepsService', ['$http', function ($http) {
            return {
              index: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps');
              },
              enable: function (projectId, version, pythonKernel) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enable/' + version + "/" + pythonKernel);
              },
              enabled: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/enabled');
              },
              installed: function (projectId) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/installed');
              },
              install: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/install',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              installOneHost: function (projectId, host, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/installOneHost/' + host,
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              uninstall: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/remove',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              upgrade: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/upgrade',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              search: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/search',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              status: function (projectId, data) {
                var regReq = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/pythonDeps/status',
                  headers: {'Content-Type': 'application/json'},
                  data: data,
                  dataType: "json"
                };
                return $http(regReq);
              },
              clone: function (projectId, projName) {
                return $http.get('/api/project/' + projectId + '/pythonDeps/clone/' + projName);
              }
            };
          }]);
