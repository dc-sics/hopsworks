'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('InfluxDBCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval',
                     '$routeParams', '$route', 'InfluxDBService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, InfluxDBService) {

                var self = this;
                self.projectId = $routeParams.projectID;
                self.appId = "";
                self.startTime = -1; // startTime, endTime, now will be filled by init
                self.endTime = -1;
                self.now;
                self.interval = 3000;
                self.charts = [];
                self.values = [];

                var counter = 1;
                var total_used = [];
                var heap_used = [];

                // graph initialization
                $scope.options = {
                            chart: {
                                type: 'lineChart',
                                height: 450,
                                margin : {
                                    top: 20,
                                    right: 20,
                                    bottom: 40,
                                    left: 55
                                },
                                x: function(d){ return d.x; },
                                y: function(d){ return d.y; },
                                useInteractiveGuideline: true,
                                dispatch: {
                                    stateChange: function(e){ console.log("stateChange"); },
                                    changeState: function(e){ console.log("changeState"); },
                                    tooltipShow: function(e){ console.log("tooltipShow"); },
                                    tooltipHide: function(e){ console.log("tooltipHide"); }
                                },
                                xAxis: {
                                    axisLabel: 'Time (ms)'
                                },
                                yAxis: {
                                    axisLabel: 'Voltage (v)',
                                    tickFormat: function(d){
                                        return d3.format('.02f')(d);
                                    },
                                    axisLabelDistance: -10
                                },
                                callback: function(chart){
                                    console.log("!!! lineChart callback !!!");
                                }
                            },
                            title: {
                                enable: true,
                                text: 'Title for Line Chart'
                            },
                            subtitle: {
                                enable: true,
                                text: 'Subtitle for simple line chart. Lorem ipsum dolor sit amet, at eam blandit sadipscing, vim adhuc sanctus disputando ex, cu usu affert alienum urbanitas.',
                                css: {
                                    'text-align': 'center',
                                    'margin': '10px 13px 0px 7px'
                                }
                            },
                            caption: {
                                enable: true,
                                html: '<b>Figure 1.</b> Lorem ipsum dolor sit amet, at eam blandit sadipscing, <span style="text-decoration: underline;">vim adhuc sanctus disputando ex</span>, cu usu affert alienum urbanitas. <i>Cum in purto erat, mea ne nominavi persecuti reformidans.</i> Docendi blandit abhorreant ea has, minim tantas alterum pro eu. <span style="color: darkred;">Exerci graeci ad vix, elit tacimates ea duo</span>. Id mel eruditi fuisset. Stet vidit patrioque in pro, eum ex veri verterem abhorreant, id unum oportere intellegam nec<sup>[1, <a href="https://github.com/krispo/angular-nvd3" target="_blank">2</a>, 3]</sup>.',
                                css: {
                                    'text-align': 'justify',
                                    'margin': '10px 13px 0px 7px'
                                }
                            }
                        };

                $scope.data = [];

                // http://jsbin.com/yevopawiwe/edit?html,js,output
                var updateData = function() {
                    // Fetch data from influx
                    InfluxDBService.getSparkMetrics(self.projectId, self.appId, self.startTime).then(
                        function(success) {
                            if (success.status === 200) {
                                var info = success.data;
                                self.startTime = +info.lastMeasurementTimestamp;
                                updateGraph(info);
                            } // dont do anything if response 204, no results
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching spark metrics.', ttl: 15000});
                        }
                    );
                };

                var updateGraph = function(data) {
                    total_used.push({x: counter, y: Math.random(1)});
                    heap_used.push({x: counter, y: Math.random(1)});

                    if (total_used.length > 20) {
                        total_used.shift();
                        heap_used.shift();
                    }

                    $scope.data = [
                        {
                            values: total_used,
                            key: 'Total used',
                            color: '#ff7f0e'
                        },
                        {
                            values: heap_used,
                            key: 'Heap used',
                            color: '#7777ff',
                            area: true
                        }
                    ];

                    counter++;
                };

                var init = function() {
                    self.appId = InfluxDBService.getAppId();

                    JobService.getAppInfo(self.projectId, self.appId).then(
                        function(success) {
                            var info = success.data;
                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;
                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                // Every X seconds retrieve the new data
                self.poller = $interval(function () {
                    updateData();
                }, self.interval);

                /**
                 * Close the poller if the controller is destroyed.
                 */
                $scope.$on('$destroy', function () {
                  $interval.cancel(self.poller);
                });
           }]);