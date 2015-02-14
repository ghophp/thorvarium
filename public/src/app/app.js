angular.module( 'thorvarium', [
  'templates-app',
  'templates-common',
  'thorvarium.home',
  'thorvarium.login',
  'thorvarium.chat',
  'ui.router'
])

.config( function myAppConfig ( $stateProvider, $urlRouterProvider ) {
  $urlRouterProvider.otherwise( '/home' );
})

.run( function run () {
})

.controller( 'AppCtrl', function AppCtrl ( $scope, $location ) {
  
  $scope.go = function ( path ) {
    $location.path( path );
  };

  $scope.errorHandler = function(error) {
        
    error = angular.isDefined(error.responseText) ? $.parseJSON(error.responseText) : {};
    if (angular.isDefined(error.cause)) {

      if (error.cause === 'user_not_found') {
        alert('User not found!');
      } else if (error.cause === 'invalid_params') {
        alert('Invalid params!');
      }

    } else {
      alert('Some error occured, please try again!');
    }
  };
  
  $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState, fromParams){
    if (angular.isDefined( toState.data.pageTitle)) {
      $scope.pageTitle = toState.data.pageTitle + ' | Thorvarium';
    }
  });

})

;

