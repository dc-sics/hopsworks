'use strict';
/*
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')

    .factory('DeviceManagementService', ['$http', function ($http) {
        var service = {
            /**
             * Get all the topics defined in the project with given id.
             * @param {int} projectId
             * @returns {unresolved} A list of topic objects.
             */
            getDevices: function (projectId) {
                return $http.get('/api/project/' + projectId + '/devices/devices');
            },

            validateSchema: function (projectId, schemaDetails) {
                var req = {
                    method: 'POST',
                    url: '/api/project/' + projectId + '/devices/device',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    data: schemaDetails
                };
                return $http(req);
            }
        }

        return service;
    }]);
