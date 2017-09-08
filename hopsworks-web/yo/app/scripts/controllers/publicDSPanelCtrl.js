'use strict';

angular.module('hopsWorksApp')
        .controller('PublicDSPanelCtrl', ['$scope', 'DelaGService',
          function ($scope, DelaGService) {
            var self = this;
            $scope.$watch("content", function (newValue, oldValue) {
              init(newValue);
            });

            var init = function (content) {
              DelaGService.getDetails(content.publicId).then(function (success) {
//                console.log("init", success);
                content["leechers"] = success.data.dataset.datasetHealth.leechers;
                content["seeders"] = success.data.dataset.datasetHealth.seeders;
                content["bootstrap"] = success.data.bootstrap;
                content["rating"] = success.data.dataset.rating;
              }, function (error) {
                return [];
                console.log("init", error);
              });
            };

            self.sizeOnDisk = function (fileSizeInBytes) {
              if (fileSizeInBytes === undefined) {
                return '--';
              }
              return convertSize(fileSizeInBytes);
            };
          }]);

