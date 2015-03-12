angular.module( 'thorvarium.game.loop', [
  'ui.router'
])

.constant('CHOOSING', 1)
.constant('RUNNING', 2)
.constant('WAITING', 3)

.constant('MAX_SIZE', 17.0)
.constant('MAX_DISTANCE', 120.0)
.constant('MAX_SPEED', 40.0)

.constant('SCREEN_GAP', 20.0)
.constant('SCREEN_WIDTH', 500)
.constant('SCREEN_HEIGHT', 500)

.constant('MAX_BULLET_SPEED', 200.0)
.constant('MAX_BULLET_POWER', 25.0)
.constant('MAX_BULLET_SIZE', 5.0)

.constant('SLOTS', {
    PERSON_SLOT1: 'person1',
    PERSON_SLOT2: 'person2',
    PERSON_SLOT3: 'person3',    
    WEAPON_SLOT1: 'weapon1',
    WEAPON_SLOT2: 'weapon2'
})

.constant('COLORS', (function(){
  var slots = {
    PERSON_SLOT1: 'person1',
    PERSON_SLOT2: 'person2',
    PERSON_SLOT3: 'person3',    
    WEAPON_SLOT1: 'weapon1',
    WEAPON_SLOT2: 'weapon2'
  };

  var p1 = $.parseJSON('{"'+
    slots.PERSON_SLOT1 + '": "#EDC8A3","' +
    slots.PERSON_SLOT2 + '": "#796151","' +
    slots.PERSON_SLOT3 + '": "#CD877A"'+
  '}');

  var p2 = $.parseJSON('{"'+
    slots.PERSON_SLOT1 + '": "#F0E9E3","' +
    slots.PERSON_SLOT2 + '": "#28C6D9","' +
    slots.PERSON_SLOT3 + '": "#EB2873"'+
  '}');

  return {1: p1, 2: p2};
})())

.service('Game', function($rootScope, $window, Person, Bullet, 
  CHOOSING, 
  RUNNING, 
  WAITING,
  MAX_SPEED,
  SCREEN_GAP,
  SCREEN_WIDTH,
  SCREEN_HEIGHT,
  MAX_BULLET_SPEED,
  MAX_BULLET_POWER,
  MAX_BULLET_SIZE,
  COLORS) {

  this.id = null;
  this.players = [];
  this.persons = [];
  this.weapons = [];
  this.bullets = [];
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
    this.bullets = [];
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
    that.bullets = [];
    _.each(players, function(p) {

      var pl = _.find(that.players, function(o) {
        return o.user.id == p.user.id;
      });

      if (angular.isDefined(pl)) {
        _.each(p.persons, function(person, key) {
          var curr = pl.persons[key];
          if (angular.isDefined(curr)) {

            if (curr.person.life != person.life) {
              console.log('diff', curr.person.life, person.life);
              curr.person.life = person.life;
            }

            curr.person.x = person.x;
            curr.person.y = person.y;
            curr.to = null;
            console.log(curr.person);
          }
        });
      }
    });
  };

  this.turn = function(inputs) {
    
    var that = this;
    var hasAction = false;

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
        _.each(i.weapons, function(weapon, key) {
          
          var curr = pl.persons[key];
          curr.shot = null;

          if (angular.isDefined(curr)) {
            _.each(weapon, function(w, wk) {
              
              var currw = curr.person.weapons[wk];
              if (angular.isDefined(currw)) {
                
                var angle = Math.atan2(w.y - curr.person.y, w.x - curr.person.x);
                var speed = (MAX_BULLET_SPEED / 100.0) * currw.speed;
                var power = (MAX_BULLET_POWER / 100.0) * currw.power;
                var size = (MAX_BULLET_SIZE / 100.0) * currw.size;

                that.bullets.push(new Bullet(
                  pl,
                  curr,
                  currw,
                  angle,
                  speed,
                  power,
                  size,
                  curr.person.x,
                  curr.person.y,
                  that.context));
              }
            });          
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
          return new Person(
            person,
            key,
            angular.copy(COLORS[p.slot][key]),
            that.personsImages[person.id],
            that.context);
        });

        return p;
      });

      // Let's play this game!
      $($window).ready(function() {

        $('.game-scenario').append(that.canvas);
        $('.game-scenario canvas').click(that.interaction);
        $('.game-scenario canvas').mousemove(that.movement);

        $rootScope.$broadcast("loaded");

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

  this.setActive = function(active) {
    this.active = active;
    $rootScope.$broadcast('actived');
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
            that.setActive(null);
          }
        }

      } else {

        var person = _.findKey(that.me().persons, function(p) {
          return p.person.life > 0 &&
            p.clicked(x, y) &&
            (p.movement === null || p.shot === null);
        });

        if (angular.isDefined(person)) {
          that.setActive(person);
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
    
    var that = this;
    var hasAction = false;
    
    var collided = [];
    _.each(this.players, function(player) {
      _.each(player.persons, function(p, key) {
        _.each(that.bullets, function(b) {
          if (b.person !== p && b.collided(p)) {
            p.person.life -= b.power;
            collided.push(b);
          }
        });
      });
    });
    
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

    this.bullets = _.difference(this.bullets, collided);
    _.each(this.bullets, function(b) {
      var speed = b.speed * elapsed;
      var half = (b.size / 2);
      if (b.x + half > 0 && b.x - half < SCREEN_WIDTH && 
        b.y + half > 0 && b.y - half < SCREEN_HEIGHT) {

        b.x = b.x + (Math.cos(b.angle) * speed);
        b.y = b.y + (Math.sin(b.angle) * speed);
        hasAction = true;
      }
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

    _.each(this.bullets, function(bullet) {
      bullet.draw();
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