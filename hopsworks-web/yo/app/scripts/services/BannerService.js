/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
    .factory('BannerService', ['$http', function ($http) {
        var service = {

            findBanner: function () {
                return $http.get('/api/banner');
            },
            findUserBanner: function () {
                return $http.get('/api/banner/user');
            },
            isFirstTime: function () {
                return $http.get('/api/banner/firsttime');
            },
            isAdminPasswordChanged: function () {
                return $http.get('/api/banner/admin_pwd_changed');
            }
        };
        return service;
    }]);
