'use strict';
/*
 * Service allowing fetching job history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('AirpalService', ['$http', function ($http, $q) {
            var service = {
              register: function (projectid) {

                var regReq = {
                  method: 'POST',
                  url: '/airpal',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: projectid
                };


                return $http(regReq);
              }

            };
            return service;

          }]);

