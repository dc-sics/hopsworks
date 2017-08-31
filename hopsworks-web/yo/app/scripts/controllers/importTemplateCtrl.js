/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ImportTemplateCtrl',
                ['$scope', '$uibModalInstance', 'growl', 'flowFactory',
                  function ($scope, $uibModalInstance, growl, flowFactory) {

                    var self = this;
                    self.model = {};
                    self.errorMsg;
                    self.files = {};

                    //selectedTemplate is bound to the front end component
                    self.selectedTemplate = {};
                    self.templates = [];
                    self.template = {};

                    //File upload handling functions
                    self.target = getApiPath() + '/metadata/upload/';

                    self.size = function (fileSizeInBytes) {
                      return convertSize(fileSizeInBytes);
                    };

                    self.existingFlowObject = flowFactory.create({
                      target: self.target//,
                              //query: {template: self.selectedTemplate}
                    });

                    self.fileErrorHandler = function (file, message, flow) {
                      var msg = JSON.parse(message);
                      self.errorMsg = msg.errorMsg;
                      self.files[file.name] = msg.errorMsg;
                    };

                    self.errorHandler = function (file, message, flow) {
                      var msg = JSON.parse(message);
                      growl.error(msg.errorMsg, {title: 'Error', ttl: 5000, referenceId: 1});
                    };

                    self.fileAddedHandler = function (file, flow) {
                      console.log(file.name);
                      self.files[file.name] = '';
                    };

                    self.close = function () {
                      $uibModalInstance.dismiss('cancel');
                      console.log("closing the window");
                    };

                  }]);