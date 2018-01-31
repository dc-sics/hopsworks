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

'use strict'

angular.module('hopsWorksApp')
    .controller('SshKeysCtrl', ['UserService', '$location', '$scope', 'growl', '$uibModalInstance',
        function (UserService, $location, $scope, growl, $uibModalInstance) {

            var self = this;
            self.working = false;

            $scope.keys = [];

            self.key = {
                name: '',
                publicKey: ''
            };


            var idx = function (obj) {
                return obj.name;
            };

            $scope.status = {};

            self.addSshKey = function () {
                self.working = true;
                self.key.status = false;
                UserService.addSshKey(self.key).then(
                //UserService.addSshKey(self.key.name, self.key.publicKey).then(
                    function (success) {
                        growl.success("Your ssh key has been successfully added.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.keys.push(self.key);
                        $scope.sshKeysForm.$setPristine();
                        $scope.keys[$scope.keys.length - 1].status = false;
                        //var name = self.key.name;
                        //$scope.status[idx(self.key)] = false;
                        self.working = false;
                    },
                    function (error) {
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to add your ssh key: " + self.errorMsg, {
                            title: 'Error',
                            ttl: 5000,
                            referenceId: 1
                        });
                        $scope.sshKeysForm.$setPristine();
                        self.working = false;
                    });
            };


            self.removeSshKey = function (keyName) {
                self.working = true;
                UserService.removeSshKey(keyName).then(
                    function (success) {
                        self.working = false;
                        self.getSshKeys();
                        growl.success("Your ssh key has been successfully removed.", {
                            title: 'Success',
                            ttl: 5000,
                            referenceId: 1
                        });
                    }, function (error) {
                        self.working = false;
                        self.errorMsg = error.data.errorMsg;
                        growl.error("Failed to remove your ssh key.", {title: 'Error', ttl: 5000, referenceId: 1});
                    });
            };

            self.getSshKeys = function () {
                UserService.getSshKeys().then(
                    function (success) {
                        $scope.keys = success.data;
                    });
            }, function (error) {
                self.errorMsg = error.data.errorMsg;
                growl.info("No ssh keys registered: " + error.data.errorMsg, {
                    title: 'Info',
                    ttl: 5000,
                    referenceId: 1
                });
            };


            self.reset = function () {
                $scope.sshKeysForm.$setPristine();
            };

            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };


            self.getSshKeys();

        }
    ]
);
