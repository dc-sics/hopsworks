/*
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
 *
 */
'use strict';
/*
 * Controller for the job UI dialog.
 */
angular.module('hopsWorksApp')
    .controller('ExperimentsCtrl', ['$scope', '$timeout', 'growl', 'JobService', 'TensorBoardService', '$interval',
        '$routeParams', '$route', '$sce', '$window',
        function($scope, $timeout, growl, JobService, TensorBoardService, $interval,
            $routeParams, $route, $sce, $window) {

            var self = this;
            self.appIds = [];
            self.ui = "";
            self.id = "";
            self.current = "";
            self.projectId = $routeParams.projectID;
            self.tb = "";
            self.reloadedOnce = false;

            self.loading = false;
            self.loadingText = "";

            var startLoading = function(label) {
                self.loading = true;
                self.loadingText = label;
            };
            var stopLoading = function() {
                self.loading = false;
                self.loadingText = "";
            };


            var tbRunning = function() {
                TensorBoardService.running(self.projectId).then(
                    function(success) {
                        self.tb = success.data;
                    },
                    function(error) {
                        if (error.data !== undefined && error.status !== 404) {
                        self.tb = "";
                            growl.error(error.data.errorMsg, {
                                title: 'Error fetching TensorBoard status',
                                ttl: 15000
                            });
                        }
                    });
            };

            tbRunning();

            self.viewTB = function() {
                    self.tbUI();
                TensorBoardService.view(self.projectId).then(

                    function(success) {
                    },
                    function(error) {
                    });
            };

            self.startTB = function() {
                     if(self.id === '' || !self.id.startsWith('application')) {
                            growl.error("Please specify a valid experiment _id", {
                                                                    title: 'Invalid argument',
                                                                    ttl: 15000
                                                                });
                                            return;
                                        }

            startLoading("Starting TensorBoard...");


                TensorBoardService.start(self.projectId, self.id).then(
                    function(success) {
                    self.tb = success.data;
                    self.tbUI();
                    self.id = "";
                    },
                    function(error) {
                    stopLoading();
                        growl.error(error.data.errorMsg, {
                            title: 'Error starting TensorBoard',
                            ttl: 15000
                        });
                    });
            };


            self.tbUI = function() {
                startLoading("Loading TensorBoard...");
                self.ui = "/hopsworks-api/tensorboard/experiments/" + self.tb.endpoint + "/";
                self.current = "tensorboardUI";

                self.reloadedOnce = false;
                var iframe = document.getElementById('ui_iframe');
                if (iframe === null) {
                    stopLoading();
                } else {
                    iframe.onload = function() {
                        if(!self.reloadedOnce) {
                            self.reloadedOnce = true;
                            self.refresh();
                        } else {
                            stopLoading();
                            self.reloadedOnce = false;
                            }
                    };
                }
                if (iframe !== null) {
                    iframe.src = $sce.trustAsResourceUrl(self.ui);
                }
                self.reloadedOnce = false;
            };

            self.hitEnter = function (event) {
                          var code = event.which || event.keyCode || event.charCode;
                          if (angular.equals(code, 13)) {
                          self.startTB();
                          }
                        };

            self.kibanaUI = function() {

                startLoading("Loading Experiments Overview...");
                JobService.getProjectName(self.projectId).then(
                    function(success) {
                        var projectName = success.data;

                        self.ui = "/hopsworks-api/kibana/app/kibana?projectId=" + self.projectId + "#/dashboard/" + projectName.toLowerCase() + "_experiments_summary-dashboard?_g=()&_a=" +
                            "(description:'A%20summary%20of%20all%20experiments%20run%20in%20this%20project',filters:!()," +
                            "fullScreenMode:!f,options:(darkTheme:!f,hidePanelTitles:!f,useMargins:!t),panels:!((gridData:" +
                            "(h:9,i:'1',w:12,x:0,y:0),id:" + projectName.toLowerCase() + "_experiments_summary-search,panelIndex:'1',type:search," +
                            "version:'6.2.3')),query:(language:lucene,query:''),timeRestore:!f,title:" +
                            "'Experiments%20summary%20dashboard',viewMode:view)";

                        self.current = "kibanaUI";
                        var iframe = document.getElementById('ui_iframe');
                        if (iframe !== null) {
                            iframe.src = $sce.trustAsResourceUrl(self.ui);
                        }
                        stopLoading();
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error fetching project name',
                            ttl: 15000
                        });
                        stopLoading();
                    });
            };

            self.stopTB = function() {

                TensorBoardService.stop(self.projectId).then(
                    function(success) {
                    self.tb="";
                    $route.reload();
                    },
                    function(error) {
                        growl.error(error.data.errorMsg, {
                            title: 'Error stopping TensorBoard',
                            ttl: 15000
                        });
                    });
            };

            var init = function() {
                if(self.tb === '') {
                    self.kibanaUI();
                } else {
                    self.tbUI();
                }
            }

            init();

            self.openUiInNewWindow = function() {
                $window.open(self.ui, '_blank');
            };

            self.refresh = function() {
                var ifram = document.getElementById('ui_iframe');
                if (ifram !== null) {
                    ifram.contentWindow.location.reload();
                }
            };

            var startPolling = function() {
                self.poller = $interval(function() {
                    TensorBoardService.running(self.projectId).then(
                        function(success) {
                            self.tb = success.data;
                        },
                        function(error) {
                            if(self.tb !== "") {
                                self.kibanaUI();
                            }
                            self.tb = "";
                            if (error.data !== undefined && error.status !== 404) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Error fetching TensorBoard status',
                                    ttl: 10000
                                });
                            }
                        });

                }, 60000);
            };
            startPolling();

            angular.module('hopsWorksApp').directive('bindHtmlUnsafe', function($parse, $compile) {
                return function($scope, $element, $attrs) {
                    var compile = function(newHTML) {
                        newHTML = $compile(newHTML)($scope);
                        $element.html('').append(newHTML);
                    };
                    var htmlName = $attrs.bindHtmlUnsafe;
                    $scope.$watch(htmlName, function(newHTML) {
                        if (!newHTML)
                            return;
                        compile(newHTML);
                    });
                };
            });

        }
    ]);