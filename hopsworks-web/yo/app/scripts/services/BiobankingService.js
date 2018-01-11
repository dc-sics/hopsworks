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
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('BiobankingService', ['$http', function ($http) {
            return function (id) {
              var services = {
                /**
                 * Get all the jobs defined in the project with given id.
                 * @param {int} projectId
                 * @returns {unresolved} A list of job objects.
                 */
                getAllConsentsInProject: function () {
                  return $http.get('/api/project/' + id + '/biobanking');
                },
                registerConsents: function (consent) {
                  var req = {
                    method: 'POST',
                    url: '/api/project/' + id + '/biobanking',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    data: consent
                  };
                  return $http(req);
                }
              };
              return services;
            };
          }]);
