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
            getMetrics: function(projectId, appId, database, columns, measurement, tags, groupBy) {
                var query = '/api/project/' + projectId + '/jobs/' + appId + '/influxdb/' + database + '?' +
                            'columns=' + columns + '&measurement=' + measurement + '&tags=' + tags;
                if (groupBy) {
                    query += '&groupBy=' + groupBy;
                }
                return $http.get(query);
            }
        };

        return service;
    }]);