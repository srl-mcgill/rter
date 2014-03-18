angular.module('singleItem', [
	'ng',   		//$timeout
	'ui',           //Map
	'ui.bootstrap'
])


.controller('TileSingleTweeItemCtrl', function($scope) {

})

.directive('tileSingletweetItem', function() {
	return {
		restrict: 'E',
		scope: {
			item: "="
		},
		templateUrl: '/template/items/singletweet/tile-singletweet-item.html',
		controller: 'TileSingleTweeItemCtrl',
		link: function(scope, element, attr) {

		}
	};
})

.controller('CloseupSingleTweeItemCtrl', function($scope, $http) {
	$http({method: 'GET', url: $scope.item.ContentURI, cache: false}).
      success(function(data, status) {
        $scope.tweet = data;
        console.log($scope.tweet);
        //for($scope.tweet.entites.urls)
        //var TweetCardHtml = angular.element(data.html);
        //$('#tweetcard').append(TweetCardHtml);
        //console.log($scope.displayTweet);

      }).
      error(function(data, status) {
         console.log("Twitter request failed", data, status);
        $scope.data = data || "Request failed";
        $scope.status = status;
    });
})

.directive('closeupSingletweetItem', function() {
	return {
		restrict: 'E',
		scope: {
			item: "="
		},
		templateUrl: '/template/items/singletweet/closeup-singletweet-item.html',
		controller: 'CloseupSingleTweeItemCtrl',
		link: function(scope, element, attr) {


		}
	};
});