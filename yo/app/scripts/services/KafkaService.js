'use strict';
/*
 * Service responsible for communicating with Kafka for a project.
 */
angular.module('hopsWorksApp')

        .factory('KafkaService', ['$http', function ($http) {
            var service = {
              /**
               * Request a list of all available topics for this project.
               * @param {int} projectId
               */
              getTopics: function (projectId) {
                return $http.get('/api/project/' + projectId + '/kafka');
              },
              createTopic: function (projectId, topicName) {
                return $http.get('/api/project/' + projectId + '/kafka/create/' + topicName);
              },
              removeTopic: function (projectId, topicName) {
                return $http.get('/api/project/' + projectId + '/kafka/remove/' + topicName);
              },
              getTopicDetails: function (projectId, topicName) {
                return $http.get('/api/project/' + projectId + '/kafka/details/' + topicName);
              }
            };
            return service;
          }]);


