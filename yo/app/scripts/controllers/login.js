'use strict';

angular.module('hopsWorksApp')
        .controller('LoginCtrl', ['$location', '$cookies', 'growl', 'AuthService', 'BannerService', 'md5',
          function ($location, $cookies, growl, AuthService, BannerService, md5) {

            var self = this;

            self.announcement = "";
            self.secondFactorRequired = false;

            self.showAnnouncement = function () {
              if (self.announcement === "") {
                return false;
              } else {
                return true;
              }
            };
            
            var getAnnouncement = function () {
              BannerService.findBanner().then(
                      function (success) {
                        console.log(success);
                        self.otp = success.data.otp;
                        if (success.data.status === 1) {
                          self.announcement = success.data.message;
                        }
                      }, function (error) {
                self.announcement = '';
              });
            };


            self.working = false;
            self.otp = $cookies.get('otp');
            self.user = {email: '', password: '', otp: ''};
            self.emailHash = md5.createHash(self.user.email || '');
            getAnnouncement();
            
            self.login = function () {
              self.working = true;
              AuthService.login(self.user).then(
                      function (success) {
                        self.working = false;
                        self.secondFactorRequired = false;
                        $cookies.put("email", self.user.email);
                        $location.path('/');
                        AuthService.isAdmin().then(
                            function (success) {
                              $cookies.put("isAdmin", 1);
                          },function (error) {
                              $cookies.put("isAdmin", 0);
                        });
                      }, function (error) {
                        self.working = false;
                        if (error.data !== undefined && 
                            error.data.statusCode === 400 &&
                            error.data.errorMsg === "Second factor required.") {
                          self.errorMessage = "";
                          self.emailHash = md5.createHash(self.user.email || '');
                          self.secondFactorRequired = true;
                        } else if (error.data !== undefined && 
                                   error.data !== null && 
                                   error.data.errorMsg !== undefined &&
                                   error.data.errorMsg !== null) {
                          self.errorMessage = error.data.errorMsg;
                        }
                        growl.error(error.data.errorMsg, {title: 'Cannot Login at this moment. Does your Internet work?', ttl: 4000});
              });
            };

          }]);
