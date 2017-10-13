'use strict';

var controllers = angular.module("controllers", []);

controllers.controller("HomeController", ['$scope', function ($scope) {
    $scope.title = 'Cluster management';
  }]);

controllers.controller("RegisterController", ['$scope', 'ClusterService', function ($scope, ClusterService) {
    $scope.title = 'Register your cluster';
    $scope.working = false;
    $scope.successMessage = '';
    $scope.errorMessage = '';
    $scope.newUser = {firstName: '',
                      email: '',
                      chosenPassword: '',
                      repeatedPassword: '',
                      ToS: ''};
    $scope.validationKey = '';
    $scope.register = function () {
      ClusterService.register($scope.newUser).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
      });
    };

    $scope.confirmRegister = function () {
      if ($scope.validationKey === '' || $scope.validationKey.length < 32) {
        $scope.errorMessage = 'Key not set or too short.';
        return;
      }
      $scope.errorMessage = '';
      ClusterService.confirmRegister($scope.validationKey).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
      });
    };

  }]);

controllers.controller("UnregisterController", ['$scope', 'ClusterService', function ($scope, ClusterService) {
    $scope.title = 'Unregister your cluster';
    $scope.working = false;
    $scope.successMessage = '';
    $scope.errorMessage = '';
    $scope.user = {email: '',
                   chosenPassword: ''};
    $scope.validationKey = '';
    $scope.unregister = function () {
      ClusterService.unregister($scope.user).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
      });
    };

    $scope.confirmUnregister = function () {
      if ($scope.validationKey === '' || $scope.validationKey.length < 64) {
        $scope.errorMessage = 'Key not set or too short.';
        return;
      }
      $scope.errorMessage = '';
      ClusterService.confirmUnregister($scope.validationKey).then(
        function (success) {
          $scope.successMessage = success.data.successMessage;
        }, function (error) {
          $scope.errorMessage = error.data.errorMsg;
      });
    };
  }]);