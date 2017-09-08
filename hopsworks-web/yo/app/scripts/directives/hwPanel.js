'use strict';

angular.module('hopsWorksApp').directive('hwPanel', function() {
   return {
    restrict: 'E',
    scope: {
      content: '=',
      cardIndex: '=',
      pageNo: '=',
      detailsFn: '&',
      downloadFn: '&'
    },
    templateUrl:'views/publicDSPanel.html',
    controller: 'PublicDSPanelCtrl as publicDSPanelCtrl'
  };
});

