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

.controller( 'GameCtrl', function GameController( $rootScope, $scope, $timeout, Game, RUNNING ) {

  $scope.startTimer = function() {
    $scope.countdown = 40;
    if ($scope.stepTimer !== null) {
      $timeout.cancel($scope.stepTimer);
    }
    $scope.stepTimer = $timeout($scope.onTimeout, 1000);
  };

  $scope.onTimeout = function() {
    if($scope.countdown ===  0) {
        $timeout.cancel($scope.stepTimer);
        return;
    }

    $scope.countdown--;
    $scope.stepTimer = $timeout($scope.onTimeout, 1000);
  };

  $scope.stopTimer = function() {
    $scope.countdown = 40;
    $timeout.cancel($scope.stepTimer);
  };

  $scope.endGame = function(message) {
    Game.destroy();
    $rootScope.ws = null;
    $scope.go('/chat');
    alert(message);
  };

  $scope.turn = function() {
    var turnData = JSON.stringify({
      "type": "input", 
      "persons": Game.input()
    });

    $rootScope.ws.send(turnData);
    $scope.waiting = true;
  };

  $scope.options = function() {
    
    var slots = _.filter($scope.slots, function(slot) {
      return slot.id > 0 && slot.weapon1 > 0 && slot.weapon2 > 0;
    });

    if (slots.length >= 3) {
      
      $rootScope.ws.send(JSON.stringify({
        "type": "options", 
        "persons": angular.copy($scope.slots)
      }));

      $scope.ready = true;

    } else {
      alert('Please select your persons and weapons...');
    }
  };

  $scope.$on('choosing', function() {
    $rootScope.ws.send(JSON.stringify({
      "type": "ready_to_turn"
    }));
  });

  $scope.ready = false;
  $scope.gaming = false;
  $scope.waiting = false;
  $scope.countdown = 40;

  $scope.stepTimer = null;

  $scope.slots = {
    "person1": {
      "id": 2,
      "name": "Person 1",
      "weapon1": 1,
      "weapon2": 1
    },
    "person2": {
      "id": 2,
      "name": "Person 2",
      "weapon1": 1,
      "weapon2": 1
    },
    "person3": {
      "id": 2,
      "name": "Person 3",
      "weapon1": 1,
      "weapon2": 1
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
            $scope.$apply(function(){
              $scope.startTimer();
              $scope.gaming = true;
              Game.start(message.players);
            });
          break;
          case 'pre_turn':
            $scope.$apply(function(){
              $scope.startTimer();
              Game.turn(message.inputs);
            });
          break;
          case 'after_turn':
            $scope.$apply(function() {
              if (Game.state == RUNNING) {
                Game.serverState = message.players;
              } else {
                Game.normalize(message.players);
              }
            });
          break;
          case 'turn_start':
            $scope.$apply(function() {
              $scope.startTimer();
              $scope.waiting = false;
              Game.turnStart(); 
            });
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

    $scope.startTimer();

  } else {
    $scope.go('/chat');
  }
})

;