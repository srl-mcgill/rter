angular.module('twitterItem',  [
	'ng',   		//$timeout
	'ui',           //Map
	'ui.bootstrap'
])

.controller('FormTwitterItemCtrl', function($scope, $resource) {

	//Setting defaults
	$scope.item.StartTime = new Date();
	$scope.item.StopTime = $scope.item.StartTime;

	$scope.item.HasHeading = false;
	$scope.item.Live = false;
	

	$scope.extra = {};
	$scope.extra.ResultType = "recent";
	
	if($scope.item.ContentToken !== undefined){
		$scope.extra.SearchTerm = $scope.item.ContentToken; 
	}	


	$scope.mapCenter = new google.maps.LatLng(45.50745, -73.5793);

	$scope.mapOptions = {
		center: $scope.mapCenter,
		zoom: 9,
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};

	$scope.centerAt = function(location) {
		var latlng = new google.maps.LatLng(location.coords.latitude, location.coords.longitude);
		$scope.map.setCenter(latlng);
		$scope.mapCenter = latlng;
	};

	$scope.setMarker = function($event) {
		
		if($scope.marker === undefined) {
			$scope.marker = new google.maps.Marker({
				map: $scope.map,
				position: $event.latLng
			});
			//making the circle
			$scope.item.Radius = 10*1000; //10km in meters
			$scope.circle = new google.maps.Circle({
				map: $scope.map,
				center: $scope.marker.getPosition(),
				radius: $scope.item.Radius,
				editable: true,
				draggable: false,
				fillColor: "#FF0000",
				fillOpacity: 0.3,
				strokeColor: "#FF0000",
			    strokeOpacity: 0.8,
			    strokeWeight: 2

			});
			google.maps.event.addListener($scope.circle, 'radius_changed', function() {
				$scope.item.Radius = $scope.circle.getRadius();
				$scope.buildURL();
			});
			google.maps.event.addListener($scope.circle, 'center_changed', function() {
			  	$scope.marker.setPosition($scope.circle.getCenter());
			  	$scope.buildURL();
			});
		} else {
			$scope.marker.setPosition($event.latLng);
			$scope.circle.setCenter($event.latLng);
		}

		$scope.item.Lat = $event.latLng.lat();
		$scope.item.Lng = $event.latLng.lng();
	};	
})
                                                                                                                                     
.directive('formTwitterItem', function($timeout) {
	return {
		restrict: 'E',
		scope: {
			item: "=",
			form: "="
		},
		templateUrl: '/template/items/twitter/form-twitter-item.html',
		controller: 'FormTwitterItemCtrl',
		link: function(scope, element, attr) {

				if(scope.item.Lat !== undefined && scope.item.Lng !== undefined) {
					var latLng = new google.maps.LatLng(scope.item.Lat, scope.item.Lng);
					scope.marker = new google.maps.Marker({
						map: scope.map,
						position: latLng
					});
					scope.mapCenter = latLng;
					if(scope.item.Radius === undefined) {
						console.log("radius Undefined and is being defaulted to 10km");
						scope.item.Radius = 10*1000;
					}
					scope.circle = new google.maps.Circle({
						map: scope.map,
						center: scope.marker.getPosition(),
						radius: scope.item.Radius,
						editable: true,
						draggable: false,
						fillColor: "#FF0000",
						fillOpacity: 0.3,
						strokeColor: "#FF0000",
					    strokeOpacity: 0.8,
					    strokeWeight: 2

					});
				} else {
					navigator.geolocation.getCurrentPosition(scope.centerAt);
				}
				
				
				scope.buildURL = function(){
						
					if(scope.extra.SearchTerm == undefined) {
						console.log("Error:  Search Query not set");
					}

					var searchURL = "/twitter/1.1/search/tweets.json?count=40&include_entities=1"	
										+ "&q=" + scope.extra.SearchTerm 
										+ "&result_type=" + scope.extra.ResultType

					if(!(scope.item.Lat == undefined)){
						searchURL = searchURL + "&geocode="+scope.item.Lat+","+scope.item.Lng+","
						if(!(isNaN(scope.item.Radius))){
							searchURL = searchURL +(scope.item.Radius/1000)+"km";
						}else{
							searchURL = searchURL +"10km";
						}
								
					}
					
					scope.item.ContentToken = scope.extra.SearchTerm; 				
					scope.item.ContentURI = encodeURI(searchURL);
				};


				$timeout( //FIXME: Another map hack to render hidden maps
					function() {
						google.maps.event.trigger(scope.map, "resize");
						scope.map.setCenter(scope.mapCenter);
					},
					0
				);		
				
				scope.$watch('extra.SearchTerm', function(newVal, oldVal){
					if(!(newVal == undefined))	scope.buildURL();
				});
				scope.$watch('extra.ResultType', function(newVal, oldVal){
					if(!(newVal == undefined))	scope.buildURL();
				}, true);
				scope.$watch('item.Lat', function(newVal, oldVal){
					if(!(newVal == undefined))	scope.buildURL();
				}, true);

		}
	};
})

.controller('TileTwitterItemCtrl', function($scope) {

})

.directive('tileTwitterItem', function() {
	return {
		restrict: 'E',
		scope: {
			item: "="
		},
		templateUrl: '/template/items/twitter/tile-twitter-item.html',
		controller: 'TileTwitterItemCtrl',
		link: function(scope, element, attr) {
			
			
		}
	};
})
.controller('embedTweetCardCtrl', function($scope) {

})

.directive('embedTweetCard', function() {
	return {
		restrict: 'E',
		scope: {
			tweet: "="
		},
		templateUrl: '/template/items/twitter/twitter-card.html',
		controller: 'embedTweetCardCtrl',
		link: function(scope, element, attr) {
			
			
		}
	};
})

.controller('CloseupTwitterItemCtrl', function($scope, $http, ItemCache, CloseupItemDialog) {
	 $http({method: 'GET', url:$scope.item.ContentURI, cache: false})
      .success(function(data, status) {
        $scope.searchResult = data;
      })
      .error(function(data, status, headers) {
         alert("Error in Loading Tweets" + data, status, headers);
        $scope.data = data || "Request failed";
        $scope.status = status;


    });
    
    


	$scope.test = function(tweet, $event) {
			
			var newItem = {} ;
			newItem.Type = "singletweet";
			//newItem.ContentURI = '/twitter/1/statuses/oembed.json?id='+tweet.id_str;
			newItem.ContentURI = "/twitter/1.1/statuses/show.json?id=" + tweet.id_str

			var tokenText = tweet.text;
			var urlRegex = /((([A-Za-z]{3,9}:(?:\/\/)?)(?:[-;:&=\+\$,\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\+\$,\w]+@)[A-Za-z0-9.-]+)((?:\/[\+~%\/.\w-_]*)?\??(?:[-\+=&;%@.\w_]*)#?(?:[\w]*))?)/;
			console.log(tokenText.replace(urlRegex, "'url'"));
			newItem.ContentToken = tokenText.replace(urlRegex, "&ldquo;url&rdquo;");
			newItem.StartTime = new Date();
			newItem.StopTime = newItem.StartTime;

			newItem.HasHeading = false;
			newItem.HasGeo = false;
			newItem.Live = false;

			if(tweet.geo != null)
			{
				newItem.Lat = tweet.geo.coordinates[0];
				newItem.Lng = tweet.geo.coordinates[1];					
			}
			console.log(newItem);
			
			if(tweet.entities.media !== undefined)
			{
					//console.log(tweet.entities.media[0].media_url);
					newItem.ThumbnailURI = tweet.entities.media[0].media_url;
			}
			else if(tweet.entities.urls.length > 0)
			{
				if(!(tweet.entities.urls[0].expanded_url.search("instagram")< 0))
				{
					//console.log(tweet.entities.urls[0].expanded_url);
					newItem.ThumbnailURI = tweet.entities.urls[0].expanded_url;
				}
			}
			ItemCache.create(
			newItem,
			function() {
				if($scope.dialog !== undefined) {
					$scope.dialog.close();
				}
			},
			function(e) {
				console.log(e);
			});
	};  
})

.directive('closeupTwitterItem', function() {
	return {
		restrict: 'E',
		scope: {
			item: "="
		},
		templateUrl: '/template/items/twitter/closeup-twitter-item.html',
		controller: 'CloseupTwitterItemCtrl',
		link: function(scope, element, attr) {
			

		}
	};
});