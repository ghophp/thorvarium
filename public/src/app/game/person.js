angular.module( 'thorvarium.game.person', [
  'ui.router'
])

.constant('MOVE_INDICATOR', 5)
.constant('SHOT_INDICATOR', 5)

.factory('Person', function (MOVE_INDICATOR, SHOT_INDICATOR, MAX_SIZE, MAX_DISTANCE) {
 
  function Person(person, image, context) {
    this.person = person;
    this.image = image;
    this.context = context;
    
    this.movement = null;
    this.moving = null;

    this.shot = null;
    this.aiming = null;

    this.to = null;

    // active weapon - TODO: make dynamic
    this.weapon = 'weapon1';
  }

  Person.prototype = {
    draw: function(active) {

      this.context.save();

      if (this.movement !== null) {

        this.context.strokeStyle = 'rgba(255, 200, 255, 0.5)';
        this.context.beginPath();
        this.context.moveTo(this.movement.x, this.movement.y);
        this.context.lineTo(this.x(),this.y());
        this.context.stroke();

        this.context.fillStyle = 'rgba(255, 200, 255, 0.5)';
        this.context.beginPath();
        this.context.arc(this.movement.x, this.movement.y, MOVE_INDICATOR, 0, Math.PI * 2, true);
        this.context.closePath();
        this.context.fill();
      }

      if (this.shot !== null) {

        this.context.strokeStyle = 'rgba(200, 200, 255, 0.5)';
        this.context.beginPath();
        this.context.moveTo(this.shot.x, this.shot.y);
        this.context.lineTo(this.x(),this.y());
        this.context.stroke();

        this.context.fillStyle = 'rgba(200, 200, 255, 0.5)';
        this.context.beginPath();
        this.context.arc(this.shot.x, this.shot.y, SHOT_INDICATOR, 0, Math.PI * 2, true);
        this.context.closePath();
        this.context.fill();
      }

      /* Need to thing better way to post this image over the circle
      this.context.drawImage(this.image, this.rx(), this.ry());
      */

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
    aim: function(x, y) {

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
      
      this.aiming = {x: xx, y: yy};
    },
    x: function() {
      return this.person.x;
    },
    y: function() {
      return this.person.y;
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