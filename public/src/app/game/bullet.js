angular.module( 'thorvarium.game.bullet', [
  'ui.router'
])

.factory('Bullet', function () {
 
  function Bullet(player, person, weapon, angle, speed, power, size, x, y, context) {
    this.player = player;
    this.person = person;
    this.weapon = weapon;
    this.angle = angle;
    this.speed = speed;
    this.power = power;
    this.size = size;
    this.x = x;
    this.y = y;
    this.context = context;
  }

  Bullet.prototype = {
    draw: function() {

      this.context.save();

      this.context.fillStyle = '#A88181';
      this.context.beginPath();
      this.context.arc(this.x, this.y, this.size, 0, Math.PI * 2, true);
      this.context.closePath();
      this.context.fill();

      this.context.closePath();
      this.context.restore();
    },
    collided: function(p) {
      var dx = p.person.x - this.x;
      var dy = p.person.y - this.y;
      var rs = this.size + p.size();
      return dx*dx+dy*dy <= rs*rs;
    },
    middle: function() {
      return this.size / 2;
    }    
  };

  return Bullet;
})

;