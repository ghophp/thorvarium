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

.controller( 'ChatCtrl', function ChatController( $rootScope, $scope ) {

  $scope.ws = null;
  $scope.message = '';
  $scope.members = [];
  $scope.messages = [];

  $scope.send = function() {
    if ($scope.message && $scope.ws) {
      $scope.ws.send(JSON.stringify({type: 'message', content: $scope.message}));
      $scope.message = '';
    }
  };

  $scope.invite = function(user) {
    if(confirm('You want to invite '+user.nickname+' to play?')) {
      $scope.ws.send(JSON.stringify({type: 'invitation', user: user.id}));
    }
  };

  $scope.accept = function(invitation) {
    if(confirm('You want to start the game with '+invitation.user.nickname+'?')) {
      console.log('accept game');      
    }
  };

  if (angular.isDefined($.cookie('auth'))) {

    $scope.ws = new WebSocket(wsUrl);
    $scope.ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      console.log('Received message: ', message);

      if (angular.isDefined(message.command)) {

        switch(message.command) {
          case 'members':

            $scope.$apply(function(){
              $scope.members = _.filter(message.value, function(m) {
                return m.id != $rootScope.user.id;
              });
            });

          break;
        }
      } else if (angular.isDefined(message.type)) {

        switch(message.type) {
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
              return x.id == message.user;
            });

            if (angular.isDefined(inviter)) {

              message.user = inviter;

              $scope.$apply(function(){
                $scope.messages.push(message);
                $('.board').scrollTop($('.board').height());
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