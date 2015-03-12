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
		$scope.waiting = true;
		$scope.startTimer();
    Game.turn(preTurn.inputs);
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

  $scope.actived = function(player, person) {
    return player.user.id === $rootScope.user.id && person === $scope.active;
  };

  $scope.$on('loaded', function() {
    $scope.players = Game.players;
  });

  $scope.$on('actived', function() {
    $scope.active = Game.active;
  });

  $scope.$on('choosing', function() {
    Game.turnStart();
  });

  var createDebug = $.parseJSON('{"type":"game","id":"2-1","persons":[{"id":1,"name":"Small","life":50.0,"speed":100,"size":60,"distance":100,"x":0.0,"y":0.0,"weapons":{}},{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":0.0,"y":0.0,"weapons":{}},{"id":3,"name":"Big","life":100.0,"speed":50,"size":100,"distance":50,"x":0.0,"y":0.0,"weapons":{}}],"weapons":[{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100},{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":50,"size":50},{"id":3,"name":"Barrier","kind":3,"speed":0,"power":100,"size":0}],"now":1426015550717}');
  var startDebug = $.parseJSON('{"type":"game_ready","players":[{"user":{"id":1,"nickname":"test","password":"4297f44b13955235245b2497399d7a93"},"slot":1,"persons":{"person1":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":20.0,"y":20.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":50,"size":50},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}},"person2":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":20.0,"y":70.0,"weapons":{"weapon1":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}},"person3":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":70.0,"y":20.0,"weapons":{"weapon1":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}}}},{"user":{"id":2,"nickname":"test2","password":"4297f44b13955235245b2497399d7a93"},"slot":2,"persons":{"person1":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":480.0,"y":480.0,"weapons":{"weapon1":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}},"person2":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":480.0,"y":430.0,"weapons":{"weapon1":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}},"person3":{"id":2,"name":"Medium","life":70.0,"speed":70,"size":80,"distance":70,"x":430.0,"y":480.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":50,"size":50},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":100,"size":100}}}}}],"now":1426015176314}');
  var preTurn = $.parseJSON('{"type":"pre_turn","inputs":{"1":{},"2":{"movements":{"person1":{"x":480.0,"y":397.0},"person2":{"x":480.0,"y":346.0},"person3":{"x":346.0,"y":480.0}},"weapons":{"person1":{"weapon1":{"x":430.7432144511529,"y":411.95759353612715}},"person2":{"weapon1":{"x":415.9754625692678,"y":375.62299560677536}},"person3":{"weapon1":{"x":365.57410503720286,"y":426.0991274816202}}}}}}');

  $scope.ready = true;
  $scope.gaming = true;
  $scope.waiting = false;
  $scope.countdown = 40;
  $scope.stepTimer = null;

  Game.create(createDebug.id, 
      createDebug.persons,
      createDebug.weapons,
      new Date(createDebug.now));

  $scope.startTimer();
  Game.start(startDebug.players);
})

;