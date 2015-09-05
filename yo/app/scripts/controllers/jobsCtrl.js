/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('JobsCtrl', ['$scope', '$routeParams', 'growl', 'JobService', '$location', 'ModalService', '$interval',
          function ($scope, $routeParams, growl, JobService, $location, ModalService, $interval) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobs; // Will contain all the jobs.
            this.running; //Will contain run information
            this.jobFilter = {
              "creator":{
                "email":""
              },
              "jobType":"",
              "name":""
            };

            var getAllJobs = function () {
              JobService.getAllJobsInProject(self.projectId).then(
                      function (success) {
                        self.jobs = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            this.getRunStatus = function () {
              JobService.getRunStatus(self.projectId).then(
                      function (success) {
                        self.running = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };

            getAllJobs();
            self.getRunStatus();

            this.runJob = function (jobId) {
              JobService.runJob(self.projectId, jobId).then(
                      function (success) {
                        self.getRunStatus();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to run job', ttl: 15000});
              });
            };

            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.newJob = function () {
              $location.path('project/' + self.projectId + '/newjob');
            };

            self.showDetails = function (job) {
              ModalService.jobDetails('lg', job, self.projectId);
            };
            
            /**
             * Check if the jobType filter is null, and set to empty string if it is.
             * @returns {undefined}
             */
            this.checkJobTypeFilter = function(){
              if(self.jobFilter.jobType == null){
                self.jobFilter.jobType = "";
              }
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                self.getRunStatus();
              }, 5000);
            };
            startPolling();

          }]);


