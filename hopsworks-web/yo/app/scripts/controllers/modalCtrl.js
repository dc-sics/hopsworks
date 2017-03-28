'use strict';

angular.module('hopsWorksApp')
        .controller('ModalCtrl', ['$uibModalInstance',  'title', 'msg',
          function ($uibModalInstance, title, msg) {

            var self = this;
            self.title = title;
            self.msg = msg;
            
            self.ok = function () {
              $uibModalInstance.close();
            };

            self.cancel = function () {
              $uibModalInstance.dismiss('cancel');
            };
            
            self.reject = function () {
              $uibModalInstance.dismiss('reject');
            };

          }]);
