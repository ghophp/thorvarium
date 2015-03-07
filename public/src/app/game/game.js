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

  $scope.turn = function() {
    console.log('turn ready');
  };

  $scope.ready = function() {
    
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

  $scope.ready = false;
  $scope.gaming = false;
  $scope.countdown = 40;

  $scope.stepTimer = null;

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
  
  /*
  var createDebug = $.parseJSON('{"type":"game","id":"1-2","players":[{"user":{"id":1,"nickname":"test","password":"4297f44b13955235245b2497399d7a93"},"persons":{}},{"user":{"id":2,"nickname":"test2","password":"4297f44b13955235245b2497399d7a93"},"persons":{}}],"persons":[{"id":1,"name":"Small","life":50,"speed":100,"size":60,"distance":100,"x":0.0,"y":0.0,"weapons":{}},{"id":2,"name":"Medium","life":70,"speed":70,"size":80,"distance":70,"x":0.0,"y":0.0,"weapons":{}},{"id":3,"name":"Big","life":100,"speed":50,"size":100,"distance":50,"x":0.0,"y":0.0,"weapons":{}}],"weapons":[{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100},{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},{"id":3,"name":"Barrier","kind":3,"speed":0,"power":100,"size":0}],"now":1425729423402}');
  var startDebug = $.parseJSON('{"type":"game_ready","players":[{"user":{"id":1,"nickname":"test","password":"4297f44b13955235245b2497399d7a93"},"persons":{"person1":{"id":1,"name":"Small","life":50,"speed":100,"size":60,"distance":90,"x":20.0,"y":20.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100}}},"person2":{"id":3,"name":"Medium","life":100,"speed":60,"size":100,"distance":50,"x":20.0,"y":70.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100}}},"person3":{"id":2,"name":"Medium","life":70,"speed":70,"size":80,"distance":70,"x":70.0,"y":20.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100}}}}},{"user":{"id":2,"nickname":"test2","password":"4297f44b13955235245b2497399d7a93"},"persons":{"person1":{"id":2,"name":"Medium","life":70,"speed":70,"size":80,"distance":70,"x":450.0,"y":450.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100}}},"person2":{"id":2,"name":"Medium","life":70,"speed":70,"size":80,"distance":70,"x":450.0,"y":400.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":1,"name":"Single Shot","kind":1,"speed":80,"power":50,"size":100}}},"person3":{"id":2,"name":"Medium","life":70,"speed":70,"size":80,"distance":70,"x":400.0,"y":450.0,"weapons":{"weapon1":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33},"weapon2":{"id":2,"name":"Triple Shot","kind":2,"speed":100,"power":25,"size":33}}}}}],"now":1425729434723}');

  $scope.ready = true;
  $scope.gaming = true;

  Game.create(createDebug.id, 
    createDebug.players, 
    createDebug.persons, 
    createDebug.weapons, 
    createDebug.now);

  $scope.countdown = 40;
  Game.start(startDebug.players);
  $scope.startTimer();
  */
})

.service('Game', function($rootScope, $window, Person) {

  this.id = null;
  this.players = [];
  this.persons = [];
  this.weapons = [];

  this.now = null;
  this.then = null;

  this.canvas = null;
  this.context = null;

  this.bgImage = null;
  this.personsImages = {};
  this.active = null;

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
    this.personsImages = {};
    this.active = null;
  };

  this.start = function(players) {

    var that = this;
    this.players = players;

    this.canvas = document.createElement("canvas");
    this.context = this.canvas.getContext("2d");
    this.canvas.width = 500;
    this.canvas.height = 500;

    this.requestAnimationFrame = $window.requestAnimationFrame || 
      $window.webkitRequestAnimationFrame || 
      $window.msRequestAnimationFrame || 
      $window.mozRequestAnimationFrame;

    this.then = Date.now();
    $window.Game = this;

    var bgImage = new Image();
    bgImage.onload = function () {
      that.bgImage = bgImage;
      that.loaded.call(that);
    };
    bgImage.src = assetsUrl + "/images/background.png";

    _.each(this.persons, function(p){
      var pImage = new Image();
      pImage.onload = function () {
        that.personsImages[p.id] = pImage;
        that.loaded.call(that);
      };
      pImage.src = assetsUrl + "/images/person" + p.id + ".png";
    });
  };

  this.loaded = function() {
    var that = this;
    if (this.bgImage && 
      _.allKeys(this.personsImages).length >= 3) {
      
      this.players = _.map(this.players, function(p){
        p.persons = _.mapObject(p.persons, function(person, key){
          return new Person(person, that.personsImages[person.id], that.context);
        });

        return p;
      });
      
      // Let's play this game!
      $($window).ready(function() {

        $('.game-scenario').append(that.canvas);
        $('.game-scenario canvas').click(that.interaction);

        that.main.call($window);
      });  
    }
  };

  this.me = function() {
    return _.find(this.players, function(p){
      return p.user.id == $rootScope.user.id;
    });
  };

  this.interaction = function(e) {

    var that = $window.Game;
    var x = e.offsetX, y = e.offsetY;
    
    var person = _.findKey(that.me().persons, function(p) {
      return p.clicked(x, y);
    });

    if (angular.isDefined(person)) {
      that.active = person;
    }
  };

  this.update = function (modifier) {
    
  };

  this.render = function () {
    var that = this;

    this.context.drawImage(this.bgImage, 0, 0);
    
    _.each(this.players, function(player) {
      _.each(player.persons, function(person, key) {
        person.draw(player.user.id === $rootScope.user.id && key === that.active); 
      });
    });
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

.factory('Person', function () {
 
  function Person(person, image, context) {
    this.person = person;
    this.image = image;
    this.context = context;
  }

  var MAX_SIZE = 17;

  Person.prototype = {
    draw: function(active) {

      this.context.save();

      this.context.drawImage(this.image, this.person.x, this.person.y);

      this.context.fillStyle = !active ? '#fff' : '#ccc';
      this.context.beginPath();
      this.context.arc(this.x(), this.y(), this.size(), 0, Math.PI * 2, true);
      this.context.closePath();
      this.context.fill();

      this.context.closePath();
      this.context.restore();
    },
    x: function() {
      return this.person.x + this.middle();
    },
    y: function() {
      return this.person.y + this.middle();
    },
    middle: function() {
      return this.size() / 2;
    },
    size: function() {
      return (MAX_SIZE / 100) * this.person.size;
    },
    clicked: function(x, y) {
      
      var middle = this.middle();
      var radius = this.size();

      var dx = x - this.x(),
          dy = y - this.y(),
          dist = dx * dx + dy * dy;

      return dist <= radius * radius;
    }
  };

  return Person;
})

;