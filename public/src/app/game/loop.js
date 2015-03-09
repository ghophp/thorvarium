angular.module( 'thorvarium.game.loop', [
  'ui.router'
])

.constant('CHOOSING', 1)
.constant('RUNNING', 2)
.constant('WAITING', 3)

.constant('MAX_SIZE', 17)
.constant('MAX_DISTANCE', 120)
.constant('MAX_SPEED', 30)

.constant('SCREEN_GAP', 20)
.constant('SCREEN_WIDTH', 500)
.constant('SCREEN_HEIGHT', 500)

.service('Game', function($rootScope, $window, Person, 
  CHOOSING, 
  RUNNING, 
  WAITING,
  MAX_SPEED,
  SCREEN_GAP,
  SCREEN_WIDTH,
  SCREEN_HEIGHT) {

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

	this.serverState = null;

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
    this.requestAnimationFrame = null;
		this.serverState = null;
  };

  this.start = function(players) {

    var that = this;
    this.players = players;

    this.canvas = document.createElement("canvas");
    this.context = this.canvas.getContext("2d");
    this.canvas.width = SCREEN_WIDTH;
    this.canvas.height = SCREEN_HEIGHT;

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

  this.normalize = function(players) {
	var that = this;
	_.each(players, function(p) {

      var pl = _.find(that.players, function(o) {
        return o.user.id == p.user.id;
      });

      if (angular.isDefined(pl)) {
        _.each(p.persons, function(person, key) {
          var curr = pl.persons[key];
          if (angular.isDefined(curr)) {
            curr.person.life = person.life;
            curr.person.x = person.x;
            curr.person.y = person.y;
          }
        });
      }
    });
  };

  this.turn = function(inputs) {
    
    var that = this;
    var hasAction = false;

    console.log(inputs);
    _.each(inputs, function(i, iKey) {

      var pl = _.find(that.players, function(o) {
        return o.user.id == parseInt(iKey, 10);
      });

      if (angular.isDefined(pl)) {
        _.each(i.movements, function(movement, key) {
          var curr = pl.persons[key];
          if (angular.isDefined(curr)) {
            curr.movement = null;
            curr.to = {x: movement.x, y: movement.y};
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

		if (this.serverState !== null) {
			this.normalize(this.serverState);
		}

    this.state = WAITING;
    $rootScope.$broadcast("choosing");
  };

  this.turnStart = function() {
		this.serverState = null;
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

          if (active.movement === null) {
            active.movement = angular.copy(active.moving);
            active.moving = null;
          } else if (active.shot === null) {
            active.shot = _.extend(angular.copy(active.aiming), {slot: active.weapon});
            active.aiming = null;
          }

          that.active = null;
        }

      } else {

        var person = _.findKey(that.me().persons, function(p) {
          return p.clicked(x, y) && (p.movement === null || p.shot === null);
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

    if (x < SCREEN_GAP || x > SCREEN_WIDTH - SCREEN_GAP) {
      x = x < SCREEN_GAP ? 
        angular.copy(SCREEN_GAP) : 
        SCREEN_WIDTH - SCREEN_GAP;
    }

    if (y < SCREEN_GAP || y > SCREEN_HEIGHT - SCREEN_GAP) {
      y = y < SCREEN_GAP ? 
        angular.copy(SCREEN_GAP) : 
        SCREEN_HEIGHT - SCREEN_GAP;
    }

    that.mouseX = x;
    that.mouseY = y;
  };

  // Return the users actions at this turn
  this.input = function() {
    var persons = {};
    _.each(this.me().persons, function(person, key) {
      var actions = {};
      if (person.movement !== null) {
				actions = _.extend(actions, angular.copy(person.movement));
      }
      if (person.shot !== null) {
				actions = _.extend(actions, {'weapon': angular.copy(person.shot)});
      }

      if (_.keys(actions).length > 0) {
				persons[key] = actions;
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
        if (active) {
          if (person.movement === null) {
            person.move(that.mouseX, that.mouseY);  
          } else if (person.shot === null) {
            person.aim(that.mouseX, that.mouseY);  
          }
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

;