angular.module('streamingVideoV1Item', [
	//'tsunamijs.livethumbnail', //Live Thumbnails
	'edit-map',                //maps
	'disp-map'                 //maps
])

.filter('convertDatesToTimes', function() {
	return function(input) {
		for(var i = 0; i < input.length; i++) {
			if(input[i].Timestamp !== undefined) {
				input[i].Timestamp = (new Date(input[i].Timestamp)).getTime();
			}
		}
		return input;
	};
})

.filter('findCurrentGeolocation', function() {
	// TODO: make non-naive - we can assume playback is constant in most cases
	return function(geolocations, currentTime) {
		for(var i = 0; i < geolocations.length; i++) {
			if(geolocations[i].Timestamp !== undefined) {
				//console.log(currentTime + " < " + geolocations[i].Timestamp + " = " + (currentTime < geolocations[i].Timestamp ? "true" : "false"));
				if(currentTime < geolocations[i].Timestamp) {
					return i == 0 ? geolocations[i] : geolocations[i - 1];
				}
			}
		}
		return geolocations[geolocations.length - 1]
	}
})

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
		console.log("playing video " + $scope.item.ID);
		// This is a hacky workaround to fix a bug in tsunami that sometimes resizes the video
		var videoNode = $(".closeup-streamingVideoV1-item video").first();
		console.log(videoNode);
		if(videoNode.attr("width") != $scope.videoConfig.width) {
			console.log("fixed dimensions for video " + $scope.item.ID);
			videoNode.attr("width", $scope.videoConfig.width)
			videoNode.attr("height", $scope.videoConfig.height)
		}
	});

	$scope.$on("paused", function(e, video) {
		console.log("paused video " + $scope.item.ID);
	});

	$scope.$on("live", function(e, video) {
		console.log("live video " + $scope.item.ID);
	});

	$scope.$on("play", function(e, video) {
		console.log("play video " + $scope.item.ID);
	});

	$scope.toggleLive = function() {
		$scope.item.Liveseek = false;
		$timeout(function() {
			$scope.item.Liveseek = true;
		}, 100);
	};

	/*
	console.log("CloseupStreamingVideoV1ItemCtrl");

	$("body").on("play", ".closeup-streamingVideoV1-item video", function(e) {
		console.log("Event: play", e);
	});

	$scope.$watch('item', function() {
		console.log("CloseupStreamingVideoV1ItemCtrl: item changed", $scope.item);

	});

	var videoNode = $(".closeup-streamingVideoV1-item video").first();
	videoNode.on("play", function(e) {
		console.log("Event: play", e);
	});
	
	$(".closeup-streamingVideoV1-item").delegate("video", "play", function(e) {
		console.log("Event: play", e);
	});
	*/
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

.directive('reportPosition', function($filter) {
	return function($scope, $element) {
		var startTime = new Date($scope.item.StartTime);
		// TODO: make filter non-destructive
		$filter('convertDatesToTimes')($scope.item.Geolocations);
		$scope.item.Geolocations = $filter('orderBy')($scope.item.Geolocations, 'Timestamp');
		$element.bind("timeupdate", function(event) {
			var currentTime = startTime.getTime() + $element[0].currentTime * 1000;
			var currentGeolocation = $filter('findCurrentGeolocation')($scope.item.Geolocations, currentTime);
			$scope.$apply(function() {
				$scope.item.Lat = currentGeolocation.Lat;
				$scope.item.Lng = currentGeolocation.Lng;
				$scope.item.Heading = currentGeolocation.Heading;
			});
		});
	};
});
