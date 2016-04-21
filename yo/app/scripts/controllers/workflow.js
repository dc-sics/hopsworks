'use strict';

/**
 * @ngdoc function
 * @name hopsWorksApp.controller:WorkflowCtrl
 * @description
 * # WorkflowCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
  .controller('WorkflowCtrl',[ '$routeParams', 'growl','WorkflowService', 'ModalService',
      function ($routeParams, growl, WorkflowService, ModalService) {
    var self = this;
      self.workflows = [];
      var projectId = $routeParams.projectID

      var index = function(){
          WorkflowService.index(projectId).then(function(success){
              console.log(success);
              self.workflows = success.data;
          },function (error) {
              growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 10})
          })
      }
      index();

      self.newWorkflowModal = function () {
          ModalService.newWorkflow('md').then(
              function (success) {
                  index();
              }, function (error) {
                  index();
              });
      };
  }]);
