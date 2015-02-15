angular.module( 'thorvarium.login', [
  'ui.router',
  'angular-md5'
])

.config(function config( $stateProvider ) {
  $stateProvider.state( 'login', {
    url: '/login',
    views: {
      "main": {
        controller: 'LoginCtrl',
        templateUrl: 'login/login.tpl.html'
      }
    },
    data:{ pageTitle: 'Login' }
  });
})

.controller( 'LoginCtrl', function LoginController( $rootScope, $scope, md5 ) {

  $scope.nickname = '';
  $scope.password = '';

  $scope.signin = function() {
    if ($scope.nickname && $scope.password) {
      
      var data = {nickname: $scope.nickname, password: md5.createHash($scope.password) };
      $.post('/login', data, function(result) {
        
        if (typeof result.uuid !== 'undefined') {
          
          $rootScope.user = result.user;

          $.cookie('auth', result.uuid, { expires: 7, path: '/' });
          $.cookie('user', JSON.stringify($scope.user), { expires: 7, path: '/' });

          $scope.$apply(function() {
            $scope.go('/chat');
          });

        } else {
          $scope.errorHandler();
        }

      }).fail($scope.errorHandler);
    }
  };

});