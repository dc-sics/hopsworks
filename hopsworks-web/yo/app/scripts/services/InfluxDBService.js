'use strict';


angular.module('hopsWorksApp')

    .factory('InfluxDBService', ['$http', function ($http) {
        var projectId = ''; // Used only for state sharing
        var appId = '';     // jobUICtrl <-> InfluxdbCtrl

        var service = {
            init: function(projectId, appId) {
                this.projectId = projectId;
                this.appId = appId;
            },
            getAppId : function() {
                return this.appId;
            },
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