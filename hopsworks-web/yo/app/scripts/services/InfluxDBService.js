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
            getSparkMetrics: function(projectId, appId, startTime, fields, service) {
                var query = '/api/project/' + projectId + '/jobs/' + appId + '/influxdb/spark?' +
                            'fields=' + fields.join(',') + '&startTime=' + startTime + '&service=' + service;
                return $http.get(query);
            },
            getTelegrafCPUMetrics: function(projectId, appId, fields, host, startTime, endTime) {
                var query = '/api/project/' + projectId + '/jobs/' + appId + '/influxdb/tgcpu?' +
                            'fields=' + fields.join(',') + '&host=' + host +
                            '&startTime=' + startTime + '&endTime=' + endTime;
                return $http.get(query);
            },
            getNodemanagerMetrics: function(projectId, appId, startTime, fields, container) {
                var query = '/api/project/' + projectId + '/jobs/' + appId + '/influxdb/nodemanager?' +
                            'fields=' + fields.join(',') + '&container=' + container + '&startTime=' + startTime;
                return $http.get(query);
            }
        };

        return service;
    }]);