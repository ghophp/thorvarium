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

.controller( 'GameCtrl', function GameController( $rootScope, $scope, $timeout, Game ) {

  $scope.ready = false;
  $scope.gaming = false;
  $scope.countdown = 40;

  $scope.stepTimer = null;

  $scope.startTimer = function() {
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

  $scope.send = function() {
    
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
            $scope.$apply(function(){
              
              $scope.stopTimer();
              $scope.gaming = true;

              Game.start();
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

  Game.start();  
})

.service('Game', function($window) {

  this.id = null;
  this.players = [];
  this.persons = [];
  this.weapons = [];

  this.now = null;
  this.then = null;

  this.canvas = null;
  this.context = null;

  this.bgImage = null;
  this.personsImages = [];

  this.requestAnimationFrame = null;

  this.create = function(id, players, persons, weapons, now) {
    this.id = id;
    this.players = players;
    this.persons = persons;
    this.weapons = weapons;
    this.now = now;
  };

  this.destroy = function() {
    this.id = null;
    this.players = [];
    this.persons = [];
    this.weapons = [];
    this.now = null;
    
    this.canvas = null;
    this.context = null;

    this.bgImage = null;
    this.personsImages = [];

    this.keysDown = {};
  };

  this.start = function() {

    var that = this;

    this.canvas = document.createElement("canvas");
    this.context = this.canvas.getContext("2d");
    this.canvas.width = 512;
    this.canvas.height = 480;

    var bgImage = new Image();
    bgImage.onload = function () {
      that.bgImage = bgImage;
    };
    bgImage.src = assetsUrl + "/images/background.png";

    _.each(this.persons, function(p){

      var pImage = new Image();
      pImage.onload = function () {
        that.personsImages.push(pImage);
      };
      pImage.src = assetsUrl + "/images/person" + p.id + ".png";

    });

    this.requestAnimationFrame = $window.requestAnimationFrame || 
      $window.webkitRequestAnimationFrame || 
      $window.msRequestAnimationFrame || 
      $window.mozRequestAnimationFrame;

    // Let's play this game!
    this.then = Date.now();
    $window.Game = this;
    
    $($window).ready(function(){
      $('.game-scenario').append(that.canvas);
      that.main.call($window);
    });
  };

  this.update = function (modifier) {
    
  };

  this.render = function () {
    if (this.bgImage) {
      this.context.drawImage(this.bgImage, 0, 0);
    }

    if (this.personsImages.length >= 3) {
      
    }

    if (this.heroImage) {
      this.context.drawImage(this.heroImage, this.hero.x, this.hero.y);
    }
  };

  this.main = function () {

    var that = $window.Game;

    that.now = Date.now();
    var delta = that.now - that.then;

    that.update(delta / 1000);
    that.render();

    that.then = that.now;

    // Request to do this again ASAP
    that.requestAnimationFrame.call($window, that.main);
  };

  return this;
})


;