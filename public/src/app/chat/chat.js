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

  $scope.message = '';

  $scope.send = function() {
    if ($scope.message) {
      console.log('send: ' + $scope.message);
    }
  };

  if (angular.isDefined($.cookie('auth'))) {

    var ws = new WebSocket(wsUrl);
    ws.onmessage = function(message) {
      
      message = $.parseJSON(message.data);
      if (angular.isDefined(message.command)) {

        switch(message.command) {
          case 'members':

          break;
          case 'receive':
          break;
        }
      } else if (angular.isDefined(message.type)) {

        switch(message.type) {
          case 'message':
            console.log(message.value);
          break;
        }
      }
    };

  } else {
    $scope.go('/login');
  }

});