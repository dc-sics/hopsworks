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
        .controller('LoginCtrl', ['$location', '$cookies', 'growl', 'AuthService', 'BannerService', 'md5',
          function ($location, $cookies, growl, AuthService, BannerService, md5) {

            var self = this;

            self.announcement = "";
            self.secondFactorRequired = false;
            
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
                      }, function (error) {
                        self.working = false;
                        if (error.data !== undefined && error.data.statusCode === 417 &&
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
