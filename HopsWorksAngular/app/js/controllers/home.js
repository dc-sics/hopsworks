angular.module('hopsWorksApp')
  .controller('HomeCtrl', function ($scope) {
      
      
      $scope.projects = [
                            {name: "HumanGenome", "private": true},
                            {name: "DNACalc", private: false},
                            {name: "FinanceDepartment", private: true},
                            {name: "StatisticsHops", private: false}
                        ];
      
      
      
      

  });