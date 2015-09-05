/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

/**
 * Created by ermiasg on 2015-05-17.
 */
'use strict';


angular.module('hopsWorksApp')
        .controller('DataSetCreatorCtrl', ['$cookies', '$modalInstance', 'DataSetService', 'MetadataActionService', '$routeParams', 'growl', 'path',
          function ($cookies, $modalInstance, DataSetService, MetadataActionService, $routeParams, growl, path) {

            var self = this;

            self.working = false;
            self.datasets = [];
            self.selectedTemplate = {};
            self.temps = [{'temp': "temp"}];
            self.dataSet = {'name': "", 'description': "", 'template': "", 'searchable': true};
            var pId = $routeParams.projectID;
            var dataSetService = DataSetService(pId);

            self.templates = [];

            MetadataActionService.fetchTemplates($cookies['email'])
                    .then(function (response) {
                      var temps = JSON.parse(response.board);
                      angular.forEach(temps.templates, function (value, key) {
                        self.templates.push(value);
                      });
                    }, function (error) {
                      console.log("ERROR " + JSON.stringify(error));
                    });

            var createDataSetDir = function (dataSet) {
              self.working = true;
              dataSetService.createDataSetDir(dataSet)
                      .then(function (success) {
                        self.working = false;
                        $modalInstance.close(success);
                      },
                      function (error) {
                        self.working = false;
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                      });
            };

            var createTopLevelDataSet = function (dataSet) {
              self.working = true;
              dataSetService.createTopLevelDataSet(dataSet)
                      .then(function (success) {
                        self.working = false;
                        $modalInstance.close(success);
                      },
                      function (error) {
                        self.working = false;
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                      });
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

            self.saveDataSetDir = function () {
              if (path) {
                self.dataSet.name = path + '/' + self.dataSet.name;
                self.dataSet.template = self.selectedTemplate.id;
                createDataSetDir(self.dataSet);
              } else {
                self.dataSet.template = self.selectedTemplate.id;
                createTopLevelDataSet(self.dataSet);
              }
            };
          }]);

