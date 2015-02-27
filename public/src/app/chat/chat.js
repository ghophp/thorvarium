angular.module( 'thorvarium.chat', [
  'ui.router'
])

.config(function config( $stateProvider ) {
  $stateProvider.state( 'chat', {
    url: '/chat',
    views: {
      "main": {
        controller: 'ChatCtrl',
        templateUrl: 'chat/chat.tpl.html'
      }
    },
    data:{ pageTitle: 'Chat' }
  });
})

.controller( 'ChatCtrl', function ChatController( $rootScope, $scope, Game ) {

  $scope.message = '';
  $scope.members = [];
  $scope.messages = [];

  $scope.send = function() {
    if ($scope.message && $rootScope.ws) {
      $rootScope.ws.send(JSON.stringify({type: 'message', content: $scope.message}));
      $scope.message = '';
    }
  };

  $scope.invite = function(user) {
    if(confirm('You want to invite '+user.nickname+' to play?')) {
      $rootScope.ws.send(JSON.stringify({type: 'invitation', to: user.id}));
    }
  };

  $scope.accept = function(invitation) {
    if(confirm('You want to start the game with '+invitation.from.nickname+'?')) {
      $rootScope.ws.send(JSON.stringify({type: 'accept', from: invitation.from.id}));
    }
  };

  if (angular.isDefined($.cookie('auth'))) {

    $rootScope.ws = $rootScope.ws ? $rootScope.ws : new WebSocket(wsUrl);
    $rootScope.ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      console.log('Received message: ', message);

      if (angular.isDefined(message.type)) {

        switch(message.type) {
          case 'members':

            $scope.$apply(function(){
              $scope.members = _.filter(message.value, function(m) {
                return m.id != $rootScope.user.id;
              });
            });

          break;
          case 'message':

            var user = message.user == $scope.user.id ? angular.copy($scope.user) : 
            _.find($scope.members, function(x) {
              return x.id == message.user;
            });

            if (angular.isDefined(user)) {

              message.user = user;

              $scope.$apply(function(){
                $scope.messages.push(message);
                $('.board').scrollTop($('.board').height());
              });
            }

          break;
          case 'invitation':

            var inviter = _.find($scope.members, function(x) {
              return x.id == message.from;
            });

            if (angular.isDefined(inviter)) {

              message.from = inviter;

              $scope.$apply(function(){
                $scope.messages.push(message);
                $('.board').scrollTop($('.board').height());
              });
            }

          break;
          case 'game':

            if (angular.isDefined(message.id)) {

              var players = _.filter($scope.members, function(x) {
                return typeof _.find(message.players, function(i) { return i.id == x.id; }) !== 'undefined';
              });

              $scope.$apply(function(){
                
                Game.create(message.id, 
                  players,
                  message.persons,
                  message.weapons,
                  new Date(message.now));

                $scope.go('/game');
              });
            }            

            break;
        }
      }
    };

  } else {
    $scope.go('/login');
  }

});