'use strict';

angular.module('hopsWorksApp')
  .controller('MainCtrl', function ($scope) {
   
    $scope.todos = [
      'Item 1',
      'Item 2',
      'Item 3',
      'Item 4'
    ];

    $scope.addTodo = function () {
	  $scope.todos.push($scope.todo);
	  $scope.todo = '';
	};

	$scope.removeTodo = function (index){
		$scope.todos.splice(index, 1);
	};



  });
