/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
    .controller('BiobankingCtrl', ['$routeParams', 'growl', 'BiobankingService', 'DataSetService',
      function ($routeParams, growl, BiobankingService, DataSetService) {

        var self = this;
        self.projectId = $routeParams.projectID;
        var biobankingService = BiobankingService(self.projectId);
        var dataSetService = DataSetService(self.projectId);
        self.undefinedConsents = [];
        self.registeredConsents = [];

        self.registerDisabled = false;

        self.consentTypes = [{name: 'Undefined'}, {name: 'Ethical Approval'}, {name: 'Consent Info'}, {name: 'Non Consent Info'}];
        self.consentStatus = [{name: 'Undefined'}, {name: 'Approved'}, {name: 'Pending'}, {name: 'Rejected'}];

        self.fileName = function (path) {
          return path.replace(/^.*[\\\/]/, '');
        };

        self.downloadFile = function (path) {
          dataSetService.fileDownload(path);
        };

        self.registerConsents = function () {
          var consents = [];
          var j = 0;
          for (var i = self.undefinedConsents.length - 1; i >= 0; i--) {
            if (self.undefinedConsents[i].consentType !== "Undefined") {
              consents[j] = self.undefinedConsents[i];
              j++;
              var allConsents = {"consents" : consents}; 
              biobankingService.registerConsents(allConsents).then(
                  function (success) {
                    console.log("Success Registering consent");
                    growl.success(success.data.successMessage, {title: 'Consent form registered.', ttl: 1000});
                    self.undefinedConsents = [];
                    self.registeredConsents = [];
                    getUndefinedConsents();
                    getRegisteredConsents();
                  }, function (error) {
                console.log("Failure Registering consent");
//                growl.error(error.data, {title: 'Error', ttl: 5000});
                    self.undefinedConsents = [];
                    self.registeredConsents = [];
                    getUndefinedConsents();
                    getRegisteredConsents();
              });
            }
          }
        };


        var getUndefinedConsents = function () {
          biobankingService.getAllConsentsInProject().then(
              function (success) {
                console.log("Received unregistered consents");
                console.log(success.data);
                var j = 0;
                self.undefinedConsents = [];
                for (var i = success.data.length - 1; i >= 0; i--) {
                  if (success.data[i].consentType.toLowerCase() === "Undefined".toLowerCase()) {
                    self.undefinedConsents[j] = success.data[i];
                    j++;
                  }
                }
              }, function (error) {
            growl.error(error.data.errorMsg, {title: 'Error getting unregistered consent forms.', ttl: 5000});
          });
        }

        var getRegisteredConsents = function () {
          biobankingService.getAllConsentsInProject().then(
              function (success) {
                console.log("Received registered consents");
                console.log(success.data);
                var j = 0;
                self.registeredConsents = [];
                for (var i = success.data.length - 1; i >= 0; i--) {
                  if (success.data[i].consentType.toLowerCase() !== "Undefined".toLowerCase()) {
                    self.registeredConsents[j] = success.data[i];
                    j++;
                  }
                }
              }, function (error) {
            growl.error(error.data.errorMsg, {title: 'Error getting registered consent forms.', ttl: 5000});
          });
        }


        // View consent form  - download it!
        // PDF Viewer - https://github.com/winkerVSbecks/angular-pdf-viewer
        // https://github.com/sayanee/angularjs-pdf
//          var downloadPathArray = self.pathArray.slice(0);
//          downloadPathArray.push(file.name);
//          var filePath = getPath(downloadPathArray);
//          dataSetService.checkFileExist(filePath).then(
//                  function (success) {
//                    dataSetService.fileDownload(filePath);
//                  }, function (error) {
//            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
//          });
//        }
        self.init = function () {
          getUndefinedConsents();
          getRegisteredConsents();
        };

        self.init();

      }]);
