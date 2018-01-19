'use strict';

angular.module('hopsWorksApp')
        .controller('LoginCtrl', ['$location', '$cookies', 'growl', 'AuthService', 'BannerService', 'md5',
          function ($location, $cookies, growl, AuthService, BannerService, md5) {

            var self = this;

            self.announcement = "";
            self.secondFactorRequired = false;
            self.firstTime = false;
            self.isAdminPasswordChanged = true;


            self.working = false;
            self.otp = $cookies.get('otp');
            self.user = {email: '', password: '', otp: ''};
            self.emailHash = md5.createHash(self.user.email || '');
            
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

            var isFirstTime = function () {
              BannerService.isFirstTime().then(
                function (success) {
                  self.firstTime = true;
                  self.user.email="admin@kth.se"
                  self.user.password="admin"
                }, function (error) {
                  self.firstTime = false;
              });
            };              
            var isAdminPasswordChanged = function () {
              BannerService.isAdminPasswordChanged().then(
                function (success) {
                  self.isAdminPasswordChanged = true;
                }, function (error) {
                  self.isAdminPasswordChanged = false;
                  self.announcement = "Security risk: change the current default password for the 'admin@kth.se' account."
              });
            };      
            
            self.login = function () {
              self.working = true;
              AuthService.login(self.user).then(
                      function (success) {
                        self.working = false;
                        self.secondFactorRequired = false;
                        $cookies.put("email", self.user.email);
                        $location.path('/');
                      }, function (error) {
                        self.working = false;
                        if (error.data !== undefined && error.data.statusCode === 417 &&
                            error.data.errorMsg === "Second factor required.") {
                          self.errorMessage = "";
                          self.emailHash = md5.createHash(self.user.email || '');
                          self.secondFactorRequired = true;
                        } else if (error.data !== undefined && error.data.statusCode === 412  &&
                            error.data.errorMsg === "First time login") {
                            self.user.email = "admin@kth.se";
                            self.user.password = "admin";
                            self.login();
                            // self.turnOffFirstTimeLogin()
                            // Set
                        } else if (error.data !== undefined && 
                                   error.data !== null && 
                                   error.data.errorMsg !== undefined &&
                                   error.data.errorMsg !== null) {
                          self.errorMessage = error.data.errorMsg;
                        }
                        growl.error(error.data.errorMsg, {title: 'Cannot Login at this moment. Does your Internet work?', ttl: 4000});
              });
            };


            isAdminPasswordChanged();
            isFirstTime();
            getAnnouncement();

            
          }]);
