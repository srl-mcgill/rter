angular.module('streamingVideoV1Item', [
	//'tsunamijs.livethumbnail', //Live Thumbnails
	'edit-map',                //maps
	'disp-map'                 //maps
])

.controller('TileStreamingVideoV1ItemCtrl', function($scope) {
	$scope.video = {};

	$scope.livethumbConfig = {
		showtitle: false,
		autoplay: $scope.item.Live,
		selectable: false,
		skimmable: true,
		clickable: false,
		interval: 2,
		debuglive: false,
		width: 140,
		video: $scope.video
	};

	$scope.$watch('item', function() {
		if(!$scope.item) return;

		$scope.video.title = "";
		$scope.video.thumbnailUrl = $scope.item.ThumbnailURI;
		$scope.video.StartTime = $scope.item.StartTime;
		$scope.video.StopTime = $scope.item.StopTime;

		$scope.livethumbConfig.autoplay = $scope.item.Live;
	}, true);

	$scope.$on("clicked", function(e, video) {

	});
})

.directive('tileStreamingVideoV1Item', function() {
	return {
		restrict: 'E',
		scope: {
			item: "="
		},
		templateUrl: '/template/items/streamingVideoV1/tile-streamingVideoV1-item.html',
		controller: 'TileStreamingVideoV1ItemCtrl',
		link: function(scope, element, attr) {

		}
	};
})

.controller('CloseupStreamingVideoV1ItemCtrl', function($scope, $timeout, ItemCache, CloseupItemDialog) {
	$scope.videoConfig = {
		width: 480,
		height: 360
	};

	$scope.$on("playing", function(e, video) {
		//console.log("playing video " + $scope.item.ID);
		// This is a hacky workaround to fix a bug in tsunami that sometimes resizes the video
		var videoNode = $(".closeup-streamingVideoV1-item video").first();
		if(videoNode.attr("width") != $scope.videoConfig.width) {
			//console.log("fixed dimensions for video " + $scope.item.ID);
			videoNode.attr("width", $scope.videoConfig.width)
			videoNode.attr("height", $scope.videoConfig.height)
		}
	});

	$scope.$on("paused", function(e, video) {
		//console.log("paused video " + $scope.item.ID);
	});

	$scope.$on("live", function(e, video) {
		//console.log("live video " + $scope.item.ID);
	});

	$scope.$on("play", function(e, video) {
		//console.log("play video " + $scope.item.ID);
	});

	$scope.toggleLive = function() {
		$scope.item.Liveseek = false;
		$timeout(function() {
			$scope.item.Liveseek = true;
		}, 100);
	};
})

.directive('closeupStreamingVideoV1Item', function() {
	return {
		restrict: 'E',
		scope: {
			item: "=",
			dialog: "="
		},
		templateUrl: '/template/items/streamingVideoV1/closeup-streamingVideoV1-item.html',
		controller: 'CloseupStreamingVideoV1ItemCtrl',
		link: function(scope, element, attr) {

		}
	};
})

.directive('ngPoster', function() {
	return {
		priority: 99, // it needs to run after the attributes are interpolated
		link: function(scope, element, attr) {
			attr.$observe('ngPoster', function(value) {
				if (!value)
					return;

				attr.$set('poster', value);
			});
		}
	};
})

.directive('autoplayIf', function() {
	return {
		priority: 99, // it needs to run after the attributes are interpolated
		link: function(scope, element, attr) {
			attr.$observe('autoplayIf', function(value) {
				if (!value)
					return;

				attr.$set('autoplay', '');
			});
		}
	};
})

.directive('updateGeolocation', function($filter) {
	return {
		restrict: "A",
		link: function(scope, element, attr) {
			var startTime = new Date(scope.item.StartTime).getTime();
			var currentGeolocationIndex = 0;
			element.bind("timeupdate", function(event) {
				var currentDateTime = new Date(startTime + element[0].currentTime * 1000);
				currentGeolocationIndex = $filter('findGeolocationIndexAtTime')(scope.item.Geolocations, currentDateTime, currentGeolocationIndex);
				scope.$apply(function() {
					scope.item.Lat = scope.item.Geolocations[currentGeolocationIndex].Lat;
					scope.item.Lng = scope.item.Geolocations[currentGeolocationIndex].Lng;
					scope.item.Heading = scope.item.Geolocations[currentGeolocationIndex].Heading;
				});
			});
		}
	};
})

.filter('findGeolocationIndexAtTime', function() {
	return function(geolocations, currentDateTime, lastIndex) {
		// Run basic checks for standard playback case
		if(currentDateTime > geolocations[lastIndex].Timestamp
			|| lastIndex == 0) {
			if(lastIndex + 1 >= geolocations.length  // at last geolocation
				|| currentDateTime < geolocations[lastIndex + 1].Timestamp) {  // geolocation not changed
				return lastIndex;
			}
			else if(lastIndex + 2 >= geolocations.length  // next geolocation is last
				|| currentDateTime < geolocations[lastIndex + 2].Timestamp) {  // geolocation index incremented
				return lastIndex + 1;
			}
		}

		// Otherwise do linear search for index (not optimized!)
		for(var i = 0; i < geolocations.length; i++) {
			if(currentDateTime < geolocations[i].Timestamp) {
				return i == 0 ? i : i - 1;
			}
		}
		return geolocations.length - 1;
	}
});
