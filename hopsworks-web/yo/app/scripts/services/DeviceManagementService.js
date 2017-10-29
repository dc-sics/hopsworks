'use strict';
/*
 * Service allowing management of a kafka project's registered devices.
 */
angular.module('hopsWorksApp')

    .factory('DeviceManagementService', ['$http', function ($http) {
        var service = {

            getDevices: function (projectId) {
                return $http.get('/api/project/' + projectId + '/deviceManagement/devices');
            },

            getDevicesFilterByState: function (projectId, state) {
                return $http.get('/api/project/' + projectId + '/deviceManagement/devices?state=' + state);
            },

            putDevice: function (projectId, device) {
                var req = {
                    method: 'PUT',
                    url: '/api/project/' + projectId + '/deviceManagement/device',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    data: device
                };
                return $http(req);
            },

            deleteDevice: function (projectId, device) {
                var req = {
                    method: 'DELETE',
                    url: '/api/project/' + projectId + '/deviceManagement/device',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    data: device
                };
                return $http(req);
            },

            getDevicesSettings: function (projectId) {
                return $http.get('/api/project/' + projectId + '/deviceManagement/devicesSettings');
            },

            postDevicesSettings: function (projectId, devicesSettings) {
                var req = {
                    method: 'POST',
                    url: '/api/project/' + projectId + '/deviceManagement/devicesSettings',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    data: devicesSettings
                };
                return $http(req);
            },
        }

        return service;
    }]);
