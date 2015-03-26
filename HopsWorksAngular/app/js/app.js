'use strict';

angular.module('hopsWorksApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/login', {
        templateUrl: 'views/login.html',
        controller: 'SecurityCtrl'
      })
      .otherwise({
        templateUrl: 'views/login.html',
        controller: 'SecurityCtrl'      
    });
  });
