'use strict';

angular.module('hopsWorksApp')
        .factory('TourService', ['$timeout',
            function ($timeout) {

                var tourService = this;

                tourService.kafkaSchemaName = "DemoAvroSchema";
                tourService.kafkaTopicName = "DemoKafkaTopic";
                tourService.kafkaProjectPrefix = "demo_kafka";
                tourService.sparkProjectPrefix = "demo_spark";
                tourService.tensorflowProjectPrefix = "demo_tensorflow";
                tourService.tensorflowRunTip = "";
                tourService.informAndTips = true;
                tourService.tipsOnly = false;
                tourService.informOnly = false;
                tourService.showNothing = false;

                tourService.toursInfoStep = 0;
                tourService.currentStep_TourOne = -1;
                tourService.currentStep_TourTwo = -1;
                tourService.currentStep_TourThree = -1;
                tourService.currentStep_TourFour = -1;
                tourService.currentStep_TourFive = -1;
                tourService.currentStep_TourSix = -1;
                tourService.currentStep_TourSeven = -1;
                tourService.alive_TourOne = 15;
                tourService.createdJobName = null;
                tourService.activeTour = null;
                tourService.kafkaJobCreationState = "producer";
                tourService.counter = 0;

                tourService.getTensorFlowJobTip = function(){
                  if(tourService.activeTour === "tensorflow"){
                    tourService.tensorflowRunTip = "An inference job was created for you! Go on and run it after the model is finished.";
                  }
                  return tourService.tensorflowRunTip;
                };

                tourService.setActiveTour = function (tourName) {
                    tourService.activeTour = tourName;
                };

                tourService.setInformAndTipsState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setTipsOnlyState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = true;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.setInformOnly = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = true;
                  tourService.showNothing = false;
                };

                tourService.setShowNothingState = function () {
                  tourService.informAndTips = false;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = true;
                };
                
                tourService.setDefaultTourState = function () {
                  tourService.informAndTips = true;
                  tourService.tipsOnly = false;
                  tourService.informOnly = false;
                  tourService.showNothing = false;
                };

                tourService.printDebug = function () {
                  console.log("Counter: " + tourService.counter);
                  tourService.counter++;
                  console.log(">> kafka state: " + tourService
                  .kafkaJobCreationState);
                  console.log(">> TourSix: " + tourService.currentStep_TourSix);
                  console.log(">> TourSeven: " + tourService
                  .currentStep_TourSeven);

                  return true;
                };

                tourService.getKafkaGuideJarPath = function (projectName) {
                  return "hdfs:///Projects/" + projectName +
                   "/TestJob/hops-spark.jar";
                };

                tourService.resetTours = function () {

                    tourService.toursInfoStep = 0;
                    tourService.currentStep_TourOne = -1;
                    tourService.alive_TourOne = 15;
                    tourService.currentStep_TourTwo = -1;
                    tourService.currentStep_TourThree = -1;
                    tourService.currentStep_TourFour = -1;
                    tourService.currentStep_TourFive = -1;
                    tourService.currentStep_TourSix = -1;
                    tourService.currentStep_TourSeven = -1;
                    tourService.createdJobName = null;
                };

                tourService.KillTourOneSoon = function ()
                {
                    $timeout(function () {
                        tourService.alive_TourOne--;
                    }, 1000).then(function () {
                        if (tourService.alive_TourOne === 0)
                        {
                            tourService.currentStep_TourOne = -1;
                            tourService.alive_TourOne = 15;
                        } else
                        {
                            tourService.KillTourOneSoon();
                        }
                    });
                };
                return tourService;
            }
        ]);
