'use strict';


angular.module('hopsWorksApp')
    .factory('VizopsService', ['$http', '$interval', function ($http, $interval) {
        var self = this;
        var groupByInterval = 10;

        self.projectId = '';
        self.appId = '';
        self.endTime;
        self.now = null;

        var service = {
            init: function(projectId, appId) {
                self.projectId = projectId;
                self.appId = appId;
                self.groupByInterval = groupByInterval;
            },

            getAppId : function() { return self.appId; },
            getProjectId : function() { return self.projectId; },
            getGroupByInterval : function() { return self.groupByInterval; },
            setGroupByInterval : function(groupBy) { self.groupByInterval = groupBy; },

            getMetrics: function(database, columns, measurement, tags, groupBy) {
                var query = '/api/project/' + self.projectId + '/jobs/' + self.appId + '/influxdb/' + database + '?' +
                            'columns=' + columns + '&measurement=' + measurement + '&tags=' + tags;
                if (groupBy) {
                    query += '&groupBy=' + groupBy;
                }
                return $http.get(query);
            },

            getAllExecutorMetrics: function() {
                var query = '/api/project/' + self.projectId + '/jobs/' + self.appId + '/influxdb/allexecutors';
                return $http.get(query);
            }
        };

        return service;
    }]);