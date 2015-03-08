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
    $rootScope.ws.send(JSON.stringify({
      "type": "input", 
      "persons": Game.input()
    }));

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
      "weapon1": 2,
      "weapon2": 2
    },
    "person2": {
      "id": 2,
      "name": "Person 2",
      "weapon1": 2,
      "weapon2": 2
    },
    "person3": {
      "id": 2,
      "name": "Person 3",
      "weapon1": 2,
      "weapon2": 2
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
              $scope.countdown = 40;
              $scope.gaming = true;
              Game.start(message.players);
            });
          break;
          case 'turn_ready':
            $scope.$apply(function(){
              $scope.countdown = 40;
              Game.turn(message.players);
            });
          break;
          case 'turn_start':
            $scope.$apply(function(){
              $scope.countdown = 40;
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

.constant('CHOOSING', 1)
.constant('RUNNING', 2)
.constant('WAITING', 3)

.constant('MOVE_INDICATOR', 5)
.constant('MAX_SIZE', 17)
.constant('MAX_DISTANCE', 120)
.constant('MAX_SPEED', 30)

.service('Game', function($rootScope, $window, Person, 
  CHOOSING, 
  RUNNING, 
  WAITING,
  MAX_SPEED) {

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
  this.mouseX = 0;
  this.mouseY = 0;
  this.state = null;
  this.requestAnimationFrame = null;

  this.create = function(id, persons, weapons, now) {
    this.id = id;
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
    this.state = null;
    this.bgImage = null;
    this.personsImages = {};
    this.active = null;
    this.mouseX = 0;
    this.mouseY = 0;
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

  this.turn = function(players) {
    
    var that = this;
    var hasAction = false;

    _.each(players, function(p) {

      var pl = _.find(that.players, function(o) {
        return o.user.id == p.user.id;
      });

      if (angular.isDefined(p)) {
        _.each(p.persons, function(person, key) {
          var curr = pl.persons[key];
          if (angular.isDefined(curr)) {
            curr.movement = null;
            curr.to = {x: person.x, y: person.y};
            hasAction = true;
          }
        });
      }
    });

    if (hasAction) {
      this.state = RUNNING;
    } else {
      this.waitForTurn();
    }
  };

  this.waitForTurn = function() {
    this.state = WAITING;
    $rootScope.$broadcast("choosing");
  };

  this.turnStart = function() {
    this.state = CHOOSING;
  };

  this.loaded = function() {
    var that = this;
    if (this.bgImage && _.allKeys(this.personsImages).length >= 3) {
      
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
        $('.game-scenario canvas').mousemove(that.movement);

        that.waitForTurn();
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
    if (that.state === CHOOSING) {
      var x = e.offsetX, y = e.offsetY;
      if(!angular.isDefined(e.offsetX)) {
        x = e.pageX-$('.game-scenario canvas').offset().left;
        y = e.pageY-$('.game-scenario canvas').offset().top;
      }

      if (that.active !== null) {

        var active = _.find(that.me().persons, function(p, key) {
          return key == that.active;
        });

        if (angular.isDefined(active)) {
          active.movement = angular.copy(active.moving);
          active.moving = null;
          that.active = null;
        }

      } else {

        var person = _.findKey(that.me().persons, function(p) {
          return p.clicked(x, y) && p.movement === null;
        });

        if (angular.isDefined(person)) {
          that.active = person;
        }
      }
    }
  };

  this.movement = function(e) {
    var that = $window.Game;
    var x = e.offsetX, y = e.offsetY;
    if(!angular.isDefined(e.offsetX)) {
      x = e.pageX-$('.game-scenario canvas').offset().left;
      y = e.pageY-$('.game-scenario canvas').offset().top;
    }

    that.mouseX = x;
    that.mouseY = y;
  };

  // Return the users actions at this turn
  this.input = function() {
    var persons = {};
    _.each(this.me().persons, function(person, key) {
      if (person.movement !== null) {
        persons[key] = angular.copy(person.movement);
      }
    });

    this.state = WAITING;
    return persons;
  };

  this.update = function (elapsed) {
    
    var hasAction = false;
    _.each(this.players, function(player) {
      _.each(player.persons, function(p, key) {
        if (p.to !== null) {

          var angle = Math.atan2(p.to.y - p.person.y, p.to.x - p.person.x);
          var speed = ((MAX_SPEED / 100.0) * p.person.speed) * elapsed;

          if (Math.abs(p.person.x - p.to.x) > 1) {
            
            p.person.x = p.person.x + (Math.cos(angle) * speed);
            hasAction = true;

          } else {
            p.person.x = p.to.x;
          }
          
          if (Math.abs(p.person.y - p.to.y) > 1) {
          
            p.person.y = p.person.y + (Math.sin(angle) * speed);
            hasAction = true;
          
          } else {
            p.person.y = p.to.y;
          }
        }
      });
    });

    if (!hasAction) {
      this.waitForTurn();
    }
  };

  this.render = function () {
    var that = this;

    this.context.drawImage(this.bgImage, 0, 0);
    
    _.each(this.players, function(player) {
      _.each(player.persons, function(person, key) {
        
        var active = player.user.id === $rootScope.user.id && key === that.active;
        person.draw(active);
        if (active && person.movement === null) {
          person.move(that.mouseX, that.mouseY);
        }
      });
    });
  };

  this.main = function () {
    var that = $window.Game;

    that.now = Date.now();
    var delta = that.now - that.then;

    if (that.state == RUNNING) {
      that.update(delta / 1000);
    }
    
    that.render();
    that.then = that.now;
    that.requestAnimationFrame.call($window, that.main);
  };

  return this;
})

.factory('Person', function (MOVE_INDICATOR, MAX_SIZE, MAX_DISTANCE) {
 
  function Person(person, image, context) {
    this.person = person;
    this.image = image;
    this.context = context;
    this.movement = null;
    this.moving = null;
    this.to = null;
  }

  Person.prototype = {
    draw: function(active) {

      this.context.save();

      if (this.movement !== null) {
        this.context.fillStyle = 'rgba(255, 200, 255, 0.5)';
        this.context.beginPath();
        this.context.arc(this.movement.x, this.movement.y, MOVE_INDICATOR, 0, Math.PI * 2, true);
        this.context.closePath();
        this.context.fill();
      }

      this.context.drawImage(this.image, this.rx(), this.ry());

      this.context.fillStyle = !active ? '#fff' : '#ccc';
      this.context.beginPath();
      this.context.arc(this.x(), this.y(), this.size(), 0, Math.PI * 2, true);
      this.context.closePath();
      this.context.fill();

      this.context.closePath();
      this.context.restore();
    },
    move: function(x, y) {

      var cx = this.x();
      var cy = this.y();
      var r = this.distance();
      var dx = x - cx;
      var dy = y - cy;
      var angle = Math.atan2(dy,dx);
      
      var xx = cx + r*Math.cos(angle);
      var yy = cy + r*Math.sin(angle);

      var dist = dx * dx + dy * dy;
      if (dist <= r * r) {
        xx = x;
        yy = y;
      }

      this.context.strokeStyle = 'rgba(255, 255, 255, 0.5)';
      this.context.fillStyle = 'rgba(255, 255, 255, 0.5)';
      
      this.context.beginPath();
      this.context.arc(cx,cy,r,0,Math.PI*2);
      this.context.closePath();
      this.context.stroke();

      this.context.beginPath();
      this.context.moveTo(cx,cy);
      this.context.lineTo(xx,yy);
      this.context.stroke();

      this.context.beginPath();
      this.context.arc(xx,yy,5,0,Math.PI*2);
      this.context.closePath();
      this.context.fill();
      
      this.moving = {x: xx, y: yy};
    },
    x: function() {
      return this.person.x + this.middle();
    },
    y: function() {
      return this.person.y + this.middle();
    },
    rx: function() {
      return this.person.x - this.middle();
    },
    ry: function() {
      return this.person.y - this.middle();
    },
    middle: function() {
      return this.size() / 2;
    },
    distance: function() {
      return (MAX_DISTANCE / 100) * this.person.distance;
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