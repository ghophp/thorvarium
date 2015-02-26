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

  if ($scope.ws && Game.id) {

    $scope.ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      console.log('Received message: ', message);

      if (angular.isDefined(message.type)) {

        switch(message.type) {
          case 'won':
            Game.destroy();
            $scope.go('/chat');        
          break;
          case 'lose':
            Game.destroy();
            $scope.go('/chat');
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