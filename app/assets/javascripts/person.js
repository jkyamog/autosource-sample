var app = angular.module("app", ["ngResource"])
  .factory('Person', ["$resource", function($resource){
    return $resource('persons/:id', {id: '@id'}, {
    	update: {method: 'PUT', params: {id : '@id'}},
    	updateBroken: {method: 'PUT', params: {id : '@id'}, url: 'personsBroken/:id'},
    	updateWorking: {method: 'PUT', params: {id : '@id'}, url: 'personsWorking/:id'}
    });
  }])
  .controller("PersonCtrl", ["$scope", "Person", function($scope, Person) {

    $scope.createForm = {};
    $scope.persons = Person.query();
    
    var onError = function onError() {
    	alert("error on saving");
    }

    $scope.create = function() {
      var person = new Person({name: $scope.createForm.name, age: $scope.createForm.age});
      person.$save({}, function onSuccess(data){
        $scope.createForm = {};
        $scope.persons.push(data);
      }, onError);
    }

    $scope.remove = function(person) {
      person.$remove(function onSuccess() {
    	  $scope.persons.splice($scope.persons.indexOf(person), 1);
      }, onError)
    }

    $scope.update = function(person) {
      person.$update({}, undefined, onError);
    }

    $scope.updateBroken = function(person) {
        person.$updateBroken({}, undefined, onError);
    }

    $scope.updateWorking = function(person) {
        person.$updateWorking({}, undefined, onError);
    }
}]);
