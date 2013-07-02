var markers = [];
var map;
function initialize() {
        
        var companies = data.companies;
        for (var i = 0; i < companies.length; i++) {
          var company = companies[i];
          var latLng = new google.maps.LatLng(company.lat,
              company.lng);
          var marker = new google.maps.Marker({
            position: latLng
          });
          var title = company.name.replace("&amp;","&");
          marker.setTitle(title);
          markers.push(marker);
        }
        var markerCluster = new MarkerClusterer(map, markers);
}
function centerMap(lat,lng){
	var center = new google.maps.LatLng(lat,lng);

        map = new google.maps.Map(document.getElementById('map'), {
          	zoom: 5,
          	center: center,
          	mapTypeControl:false,
           	panControl: false,
   			zoomControl: true,
    		zoomControlOptions: {
        	style: google.maps.ZoomControlStyle.MEDIUM,
        	position: google.maps.ControlPosition.RIGHT_CENTER
    		},
    		scaleControl: false,
    		streetViewControl: false,
   			mapTypeId: google.maps.MapTypeId.ROADMAP
        });
}
      
$("document").ready(function () {
	centerMap(23.12580705, 82.77897045);
	initialize();
    $("input").each(function() {
   		var elem = $(this);
		elem.data('oldVal', elem.val());
	  	elem.bind("propertychange keyup input paste", function(event){
      	if (elem.data('oldVal') != elem.val()) {
       		elem.data('oldVal', elem.val());
       		if (elem.val().trim() !== "")
			search(elem.val());
			else{
			map.setCenter(new google.maps.LatLng(23.12580705, 82.77897045));
			var markerCluster = new MarkerClusterer(map, markers);
			map.setZoom(5);
			$("div#results").html("");
			}
		}
   });
 });

	$("input").focusin(function() {
		if ($(this).attr("value").trim() === "Search for a company...")
		$(this).attr("value","");
	});
	$("input").focusout(function() {
		if ($(this).attr("value").trim() === ""){
			$(this).attr("value","Search for a company...");
			map.setCenter(new google.maps.LatLng(23.12580705, 82.77897045));
			var markerCluster = new MarkerClusterer(map, markers);
			map.setZoom(5);
			$("div#results").html("");
		}
	});
});
function search(value){
	var companies=data.companies;
	var found = false;
	var latlngbounds = new google.maps.LatLngBounds();
   	var result ="";
   	var resultmarkers=[];
   	for (var i=0;i<companies.length;i++){
		if (companies[i].name.substring(0, value.length).toUpperCase() === value.toUpperCase()){
				latlngbounds.extend(markers[i].position);
				resultmarkers.push(markers[i]);
				found=true;
				result+="<br><div>"+companies[i].name+"</div><br>";
		}
	}
	if (found){
		centerMap(23.12580705, 82.77897045);
		map.setCenter(latlngbounds.getCenter());
		map.fitBounds(latlngbounds);
		var markerCluster = new MarkerClusterer(map, resultmarkers);
	}
	else{
		map.setCenter(new google.maps.LatLng(23.12580705, 82.77897045));
		$("div#results").html("");
		map.setZoom(5);
		var markerCluster = new MarkerClusterer(map, markers);
	}
	$("div#results").html(result);
}
