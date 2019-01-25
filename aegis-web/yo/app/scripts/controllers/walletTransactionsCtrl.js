/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

'use strict';

angular.module('hopsWorksApp')
    .controller('WalletTransactionsCtrl', ['$location', '$anchorScroll', '$scope', '$rootScope',
        'md5', 'ModalService', 'HopssiteService', 'DelaService', 'ProjectService', 'growl',
        function ($location, $anchorScroll, $scope, $rootScope, md5, ModalService,
                  HopssiteService, DelaService, ProjectService, growl) {

            this.transactionCategories = [
                {
                    key: 'all',
                    name: 'All Transactions',
                },
                {
                    key: 'recent',
                    name: 'Recent Transactions',
                },
            ];
            this.selectedTransaction = this.transactionCategories[0];

            this.assetsSold = [
                {
                    id: 0,
                    dataset: 'dataset 0',
                    coins: '1000',
                    date: '10/1/2019',
                },
                {
                    id: 1,
                    dataset: 'dataset 1',
                    coins: '231',
                    date: '13/1/2019',
                },
                {
                    id: 2,
                    dataset: 'dataset 2',
                    coins: '234',
                    date: '14/1/2019',
                },
                {
                    id: 3,
                    dataset: 'dataset 3',
                    coins: '34534',
                    date: '15/1/2019',
                },
                {
                    id: 4,
                    dataset: 'dataset 4',
                    coins: '745',
                    date: '17/1/2019',
                },
                {
                    id: 5,
                    dataset: 'dataset 5',
                    coins: '123',
                    date: '18/1/2019',
                },
                {
                    id: 6,
                    dataset: 'dataset 6',
                    coins: '564',
                    date: '20/1/2019',
                },
            ];

            this.selectTransaction = function (transaction) {
              console.log('selectDisplayTransaction', transaction);

              this.selectedTransaction = transaction;
            };

            this.displayTransaction = function (asset) {
                ModalService.transaction('md', asset);
            };

            var init = function () {
              $('.keep-open').on('shown.bs.dropdown', '.dropdown', function () {
                $(this).attr('closable', false);
              });

              $('.keep-open').on('click', '.dropdown', function () {
                console.log('.keep-open: click');
              });

              $('.keep-open').on('hide.bs.dropdown', '.dropdown', function () {
                return $(this).attr('closable') === 'true';
              });

              $('.keep-open').on('click', '#dLabel', function() {
                $(this).parent().attr('closable', true );
              });

              $(window).scroll(function () {
                if ($(this).scrollLeft() > 0) {
                  $('#publicdataset').css({'left': 45 - $(this).scrollLeft()});
                }
              });
              $(window).resize(function () {
                var w = window.outerWidth;
                if (w > 1280) {
                  $('#publicdataset').css({'left': 'auto'});
                }
              });
            };

            var overflowY = function (val) {
              $('#hwWrapper').css({'overflow-y': val});
            };

            this.setupStyle = function () {
              init();
              overflowY('hidden');
              $('#publicdatasetWrapper').css({'width': '1200px'});
            };

            this.overflowYAuto = function () {
              overflowY('auto');
              $('#publicdatasetWrapper').css({'width': '1500px'});
            };

            $scope.$on("$destroy", function () {
              overflowY('auto');
            });
        }]);
