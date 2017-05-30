'use strict';


angular.module('hopsWorksApp')

    .factory('VizopsService', ['$http', function ($http) {
        var self = this;

        var projectId = '';
        var appId = '';

        var service = {
            init: function(projectId, appId) {
                self.projectId = projectId;
                self.appId = appId;
            },

            getAppId : function() { return self.appId; },

            getMetrics: function(database, columns, measurement, tags, groupBy) {
                var query = '/api/project/' + self.projectId + '/jobs/' + self.appId + '/influxdb/' + database + '?' +
                            'columns=' + columns + '&measurement=' + measurement + '&tags=' + tags;
                if (groupBy) {
                    query += '&groupBy=' + groupBy;
                }
                return $http.get(query);
            }
        };

        return service;
    }]);