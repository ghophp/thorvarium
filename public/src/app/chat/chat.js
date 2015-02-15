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

.controller( 'ChatCtrl', function ChatController( $scope ) {

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

  if (angular.isDefined($.cookie('auth'))) {

    $scope.ws = new WebSocket(wsUrl);
    $scope.ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      console.log('Received message: ', message);

      if (angular.isDefined(message.command)) {

        switch(message.command) {
          case 'members':

            $scope.$apply(function(){
              $scope.members = message.value;
            });

          break;
          case 'receive':
          break;
        }
      } else if (angular.isDefined(message.type)) {

        switch(message.type) {
          case 'message':

            var user = _.find($scope.members, function(x) {
              return x.id == message.user;
            });

            if (angular.isDefined(user)) {

              message.user = user;

              $scope.$apply(function(){
                $scope.messages.push(message);
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