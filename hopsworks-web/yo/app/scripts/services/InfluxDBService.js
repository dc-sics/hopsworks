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
            getSparkMetrics: function(projectId, appId, startTime) {
                var query = '/api/project/' + projectId + '/jobs/' + appId + '/influxdb/spark?' +
                            'fields=total_used,heap_used&startTime=' + startTime + '&service=driver';
                return $http.get(query);
            }
        };

        return service;
    }]);