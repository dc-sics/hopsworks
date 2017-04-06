'use strict';


angular.module('hopsWorksApp')

    .factory('InfluxDBService', ['$http', function ($http) {
        var service = {

            getInfluxDBVersion: function(projectId, appId) {
                return $http.get('/api/project/' + projectId + '/jobs/' + appId + '/influxdb/info');
            },
            getSparkMetrics: function(sparkAppId) {
                return 'sparkMetrics';
            },
            getYarnMetrics: function() {
                return 'yarnMetrics';
            }
        };

        return service;
    }]);