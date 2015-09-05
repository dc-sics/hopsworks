/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

/**
 * A helper service to enable communication between datasetsCtrl and 
 * metadataCtrl.
 */
angular.module('hopsWorksApp')
        .service('MetadataHelperService', ['$cookies', '$q', 'MetadataActionService',
          function ($cookies, $q, MetadataActionService) {

            var currentFile = {};
            var availableTemplates = [];
            var closeSlider = "false";
            var dirContents = "false";

            return {
              setCurrentFile: function (currentfile) {
                currentFile = currentfile;
              },
              getCurrentFile: function () {
                return currentFile;
              },
              fetchAvailableTemplates: function () {
                var defer = $q.defer();

                MetadataActionService.fetchTemplates($cookies['email'])
                        .then(function (data) {
                          angular.copy(JSON.parse(data.board).templates, availableTemplates);
                          defer.resolve(data);
                        });

                return defer.promise;
              },
              getAvailableTemplates: function () {
                return availableTemplates;
              },
              getCloseSlider: function(){
                return closeSlider;
              },
              setCloseSlider: function(value){
                closeSlider = value;
              },
              getDirContents: function(){
                return dirContents;
              },
              setDirContents: function(value){
                dirContents = "true";
              }
            };
          }]);


