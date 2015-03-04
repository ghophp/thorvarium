angular.module( 'thorvarium.game', [
  'ui.router'
])

.config(function config( $stateProvider ) {
  $stateProvider.state( 'game', {
    url: '/game',
    views: {
      "main": {
        controller: 'GameCtrl',
        templateUrl: 'game/game.tpl.html'
      }
    },
    data:{ pageTitle: 'Chat' }
  });
})

.controller( 'GameCtrl', function GameController( $rootScope, $scope, Game ) {

  $scope.endGame = function(message) {
    Game.destroy();
    $rootScope.ws = null;
    $scope.go('/chat');
    alert(message);
  };

  $scope.send = function() {
    
    var slots = _.filter($scope.slots, function(slot) {
      return slot.id > 0 && slot.weapon1 > 0 && slot.weapon2 > 0;
    });

    if (slots.length >= 3) {
      $rootScope.ws.send(JSON.stringify({
        "type": "options", 
        "persons": angular.copy($scope.slots)
      }));
    }
  };

  $scope.slots = {
    "person1": {
      "id": 0,
      "name": "Person 1",
      "weapon1": 0,
      "weapon2": 0
    },
    "person2": {
      "id": 0,
      "name": "Person 2",
      "weapon1": 0,
      "weapon2": 0
    },
    "person3": {
      "id": 0,
      "name": "Person 3",
      "weapon1": 0,
      "weapon2": 0
    }
  };

  if ($rootScope.ws && Game.id) {

    $scope.persons = angular.copy(Game.persons);
    $scope.weapons = angular.copy(Game.weapons);

    $scope.ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      console.log('Received game message: ', message);

      if (angular.isDefined(message.type)) {

        switch(message.type) {
          case 'game_ready':
            
          break;
          case 'nothing_selected':
            $scope.$apply(function(){
              $scope.endGame('Seems that your adversary run away!');
            });
          break;
          case 'won':
            $scope.$apply(function(){
              $scope.endGame('Congratulations! You won the game...');
            });
          break;
          case 'lose':
            $scope.$apply(function(){
              $scope.endGame('Ohh! You lose the game...');
            });
          break;
        }
      }
    };

  } else {
    $scope.go('/chat');
  }
  
})

.service('Game', function() {

  this.id = null;
  this.players = [];
  this.persons = [];
  this.weapons = [];

  this.start = null;

  this.create = function(id, players, persons, weapons, start) {
    this.id = id;
    this.players = players;
    this.persons = persons;
    this.weapons = weapons;
    this.start = start;
  };

  this.destroy = function() {
    this.id = null;
    this.players = [];
    this.persons = [];
    this.weapons = [];
    this.start = null;
  };

  return this;
})


;