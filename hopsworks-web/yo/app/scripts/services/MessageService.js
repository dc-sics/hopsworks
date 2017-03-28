'use strict';

angular.module('hopsWorksApp')
    .factory('MessageService',['$http', function ($http) {
        return {
            getMessages: function () {
                return $http.get('/api/message/');
            },
            getTrash: function () {
                return $http.get('/api/message/deleted');
            },
            emptyTrash: function () {
                return $http.delete('/api/message/empty');
            },
            markAsRead: function (msgId) {
                return $http.put('/api/message/markAsRead/'+msgId);
            },
            getUnreadCount: function () {
                return $http.get('/api/message/countUnread');
            },
            reply: function (msgId, msg) {
                var regReq = {
                    method: 'POST',
                    url: '/api/message/reply/' + msgId,
                    headers: {
                        'Content-Type': 'text/plain'
                    },
                    data: msg
                };
                return $http(regReq);
            },
            moveToTrash: function (msgId) {
                return $http.put('/api/message/moveToTrash/' + msgId);
            },
            restoreFromTrash: function (msgId) {
                return $http.put('/api/message/restoreFromTrash/' + msgId);
            },
            deleteMessage: function (msgId) {
                return $http.delete('/api/message/' + msgId);
            }
        };

    }]);
