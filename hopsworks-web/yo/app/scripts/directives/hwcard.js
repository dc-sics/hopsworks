'use strict';

angular.module('hopsWorksApp').directive('hwCard', function() {
   return {
    restrict: 'E',
    scope: {
      content: '=',
      limit: '=',
      cardIndex: '=',
      pageNo: '=',
      detailsFn: '&'
    },
    templateUrl:'views/card.html',
    controller: 'CardCtrl as cardCtrl'
  };
});


