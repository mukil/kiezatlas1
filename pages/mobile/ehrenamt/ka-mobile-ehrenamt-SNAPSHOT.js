/** 
 * This is some KiezatlasJS citymap client designed to run on all major mobile screens.
 * @author  Malte Rei&szlig;ig (rma@mikromedia.de), Copyright (c) 2012
 * @license   GPLv3 (http://www.gnu.org/licenses/gpl-3.0.en.html)
 * 
 * @requires  jQuery JavaScript Library v1.5.2, Copyright 2011, John Resig
              Dual licensed under the MIT or GPL Version 2 licenses.(http://jquery.org/license)
 * @requires  leaflet.js, Copyright (c) 2010-2012, CloudMade, Vladimir Agafonkin, All rights reserved.
 *            Used in version/with source code at https://github.com/mukil/Leaflet
 * @requires  iscroll.js v4.1.9 ~ Copyright (c) 2011 Matteo Spinelli, http://cubiq.org
 *            Released under MIT license, (http://cubiq.org/license)
* 
 * @modified  13.Feb 2013, Ehrenamtsnetz-App v0.4.5
 */

var SERVER_URL = "http://m.kiezatlas.de";
var SERVICE_URL = "/kiezatlas/rpc/"
var BASE_PATH = "/kiezatlas/"
  // var SERVER_URL = "http://localhost:8080/kiezatlas";
var ICONS_URL = "http://www.kiezatlas.de/client/icons/"; // to be used by all icons if not relative to this folder
var IMAGES_URL = "http://www.kiezatlas.de/client/images/";
// ui-state
// var autocomplete_item = 0;
// var alternative_items = [];
// var lastStreetName = "";
// permalink-states
var baseUrl = SERVER_URL + "/ehrenamt/";
  // baseUrl = "http://localhost/smartatlas/";
var permaLink = "";
var linkParams = [];
// debug-state
// var debugUI = false;

var kiezatlas = new function() {
  // 
  this.mapTopics = undefined;
  this.workspaceCriterias = undefined;
  this.cityMapId = undefined;
  this.workspaceId = undefined;
  this.defaultMapCenter = undefined;
  this.markersBounds = undefined; // L.LatLngBounds Object
  this.locationCircle = undefined; // L.Circle () if already set once..
  // yet unused: this.selectedCriteria = 0;
  // this.selectedTopic = undefined;
  this.mapLayer = undefined;
  this.map = undefined; // L.Map();
  // 
  this.serverUrl = undefined;
  this.serviceUrl = undefined;
  this.iconsFolder = undefined;
  this.imagesFolder = undefined;
  this.markers = undefined; // arrray of L.Marker Objects
  // 
  this.LEVEL_OF_DETAIL_ZOOM = 15; // the map focus when a mapinternal infoWindow is rendered
  this.LEVEL_OF_STREET_ZOOM = 14; // the map focus when a mapinternal infoWindow is rendered
  this.LEVEL_OF_KIEZ_ZOOM = 13;
  this.LEVEL_OF_DISTRICT_ZOOM = 12;
  this.LEVEL_OF_CITY_ZOOM = 11;
  //
  this.layer = undefined;
  //
  this.historyApiSupported = window.history.pushState;
  this.panoramaOrientation = undefined; // gui helper flag
  this.myScroll = undefined; // iScroll reference
  
  this.renderSite = function () {
    // add button for explicit geolocation-moves
    var locateButton = '<a class="leaflet-control-zoom-loc" href="#" title="Your Location"></a>';
    var moreButton = '<a id="go-more" title="Bezirksauswahl" alt="Bezirksauswahl"'
      + 'href="#"></a>';
    // register handlers
    kiezatlas.map.on('locationfound', kiezatlas.onLocationFound);
    kiezatlas.map.on('locationerror', kiezatlas.onLocationError);
    jQuery(locateButton).insertBefore(".leaflet-control-zoom-in");
    jQuery(moreButton).insertBefore(".leaflet-control-zoom-loc");
    jQuery(".leaflet-control-zoom-loc").click(kiezatlas.getUsersLocation);
    // 
    jQuery('a#go-more').click(kiezatlas.showKiezatlasControl);
    jQuery(window).resize(kiezatlas.handleOrientationChange);
    jQuery(document).ready(kiezatlas.hideAddressBarThroughScrolling);
  }
  
  this.executeBrowserSpecificCrap = function () {
    // ### 
    var head = document.getElementsByTagName('head')[0];
    var style = document.createElement('style');
    if (navigator.userAgent.indexOf('Fennec') != -1) {
      style.innerHTML = '@media only screen { #top-button { display: none !important; } '
        + '#go-more { background-position: 30px 10px; } }';
    } else if (navigator.userAgent.indexOf('WebKit') != -1 && navigator.userAgent.indexOf('Android') != -1) {
      style.innerHTML = '@media only screen { #go-more { background-position: 30px 12px !important;} }';
    } else if (navigator.userAgent.indexOf('Opera') != -1) {
      style.innerHTML = '@media only screen { #go-more { background-position: 30px 12px !important;} }';
    }
    head.appendChild(style);
  }

  this.loadMapTiles = function() {
    var cloudmadeUrl = 'http://{s}.tiles.mapbox.com/v3/kiezatlas.map-feifsq6f/{z}/{x}/{y}.png',
        cloudmadeAttribution = "Tiles &copy; <a href='http://mapbox.com/'>MapBox</a> | " +
          "Data &copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a> and contributors, CC-BY-SA",
        cloudmade = new L.TileLayer(cloudmadeUrl, {maxZoom: 18, attribution: cloudmadeAttribution});
      // mapbox: "http://a.tiles.mapbox.com/v3/kiezatlas.map-feifsq6f/${z}/${x}/${y}.png",
      //            attribution: "Tiles &copy; <a href='http://mapbox.com/'>MapBox</a> | " +
      //            "Data &copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a> " +
      //            "and contributors, CC-BY-SA",
      // osm: "http://b.tile.openstreetmap.de/tiles/osmde/"
      //        attribution: 'Tile server sponsored by STRATO / <b>Europe only</b> /
      //  <a href="http://www.openstreetmap.de/germanstyle.html">About style</a>',
      // TODO: render nice info message "Map tiles are loding ..."
      cloudmade.on('loading', function(e) {
          // is never fired... console.log('start loading tiles...' + e)
      });
      cloudmade.on('loaded', function(e) {
          // is never fired.. console.log('loaded cloudmade layer...')
      });
      cloudmade.on('load', function(e) {
          // is just fired when panning the first time out of our viewport, but strangely not on initiali tile-loading
          // console.log("tilelayer loaded..")
      });
      cloudmade.on('tileload', function (e) { 
          // console.log('loading tile ..')
          // is often fired, but do we know how often for our current viewport? not yet, but maybe.
      });
      kiezatlas.map.addLayer(cloudmade);
  }

  this.loadCityMap = function (mapId, workspaceId) {
    kiezatlas.cityMapId = mapId;
    kiezatlas.workspaceId = workspaceId;
    if (kiezatlas.markers != undefined) {
      kiezatlas.clearMarkers();
    }
    jQuery("img.loading").css("display", "block");
    // console.log("showing loading bar => " + jQuery("img.loading").css("display"));
    kiezatlas.loadCityMapTopics(mapId, workspaceId, function () {
      kiezatlas.setupLeafletMarkers();
      kiezatlas.setToCurrentBounds();
      kiezatlas.getUsersLocation();
    });
    // ### FIXMEs mvoe GUI related manipulations into guiSetup/renderFunctions
    kiezatlas.closeInfoContainer(); // close info and  show nav
    // 
    if (cityMapEhrenamtId != undefined && cityMapEventId != undefined) {
      if (kiezatlas.cityMapId == cityMapEhrenamtId) {
        jQuery("#go-do").addClass("selected");
        jQuery("#go-event").removeClass("selected");
      } else if (kiezatlas.cityMapId == cityMapEventId) {
        jQuery("#go-event").addClass("selected");
        jQuery("#go-do").removeClass("selected");
      }
    }
    // initiate current citymap state
    // var newLink = baseUrl + "?map=" + mapId;
    // kiezatlas.pushHistory({ "name": "loaded", "parameter": [ mapId, workspaceId ] }, newLink);
    kiezatlas.hideKiezatlasControl();
  }

  this.loadCityMapTopics = function (mapId, workspaceId, handler) {
    var url = SERVICE_URL
    var body = '{"method": "getMapTopics", "params": ["' + mapId+ '" , "' + workspaceId + '"]}'
    jQuery.ajax({
      type: "POST", async: true, url: url, data: body,
      beforeSend: function(xhr) { 
        xhr.setRequestHeader("Content-Type", "application/json") 
      },
      success: function(obj) {
        kiezatlas.setMapTopics(obj);
        handler();
      },
      error: function(x, s, e) {
        throw new Error("Error while loading city-map. Message: " + JSON.stringify(x));
      }
    });
  }

  this.info = function(id) {
    kiezatlas.loadCityObjectInfo(id, kiezatlas.renderInfo);
    // ### assuming sucessesfull loadCityObjectInfo-call and non-rotating topic ids in cityMap ..
    // var newLink = baseUrl + "map/" + kiezatlas.cityMapId + "/p/" + id;
    // kiezatlas.pushHistory({ "name" : "info", "parameter" : id }, newLink);
  }

  this.loadCityObjectInfo = function (topicId, renderFunction) {
    var url = SERVICE_URL;
    var body = '{"method": "getGeoObjectInfo", "params": ["' + topicId + '"]}';// '{' + urlencode(streetFocus) + '}';
    kiezatlas.showDetailsProgressBar();
    // 
    jQuery.ajax({
      type: "POST", url: url, data: body, dataType: 'json',
      beforeSend: function(xhr) { 
        xhr.setRequestHeader("Content-Type", "application/json");
      },
      success: function(obj) {
        // console.log('loaded \"' + obj.result.name + '\"');
        renderFunction(obj.result);
      },
      error: function(x, s, e) {
        throw new Error('ERROR: detailed information on this point could not be loaded. please try again. ' + x);
      }
    });
  }
  
  this.showDetailsProgressBar = function() {
    jQuery('#scroller').empty();
    var img = '<div id="loading-area"><img src="'+BASE_PATH+'pages/mobile/ehrenamt/css/pacman.gif" class="loading"></div>';
    kiezatlas.showInfoContainer();
    jQuery('#info-container').append(img);
  }
  
  this.hideDetailsProgressBar = function() {
    jQuery("#loading-area").remove();
  }

  this.renderInfo = function (topic) {
    // console.log(topic);
    jQuery("#close-button").show();
    jQuery("#down-button").show();
    // 
    var infoHeader = '<div id="info-table">';
    infoHeader += '<h3 class="title">' + topic.name + '</h3></div>';
    // infoHeader += '<a href="#" onclick="javascript:kiezatlas.scrollInfoDown()">||</a>';
    var infoItem = "";
    // 
    var street = kiezatlas.getTopicAddress(topic);
    var postalCode = kiezatlas.getTopicPostalCode(topic);
    var cityName = kiezatlas.getTopicCity(topic);
    var latLng = kiezatlas.getLatLng(topic);
    var webpageLink = kiezatlas.getWebpageLink(topic);
    var personInfo = kiezatlas.getPersonInfo(topic);
    // 
    if (cityName == undefined) cityName = "Berlin"; // fixing the imported dataset from ehrenamtsnetz
    if (cityName == " Berlin" || cityName == "Berlin") { // ### FIXME sloppy
      infoItem = '<div id="info-item"><span class="content">' + street + ', ' + postalCode + ' ' 
        + cityName + '</span><br/>';
    } else {
      infoItem = '<div id="info-item">'
        + '<span class="content">' + street + ', ' + postalCode + ' ' + cityName + '</span><br/>';
    }

    // var propertyList = ''; //<table width="100%" cellpadding="2" border="0"><tbody>';
    infoItem += personInfo;
    infoItem += "</div>";
    var target = street + "%20" + postalCode + "%20" + cityName;
    var publicTransportURL = "http://www.fahrinfo-berlin.de/fahrinfo/bin/query.exe/d?Z=" 
      + target + "&REQ0JourneyStopsZA1=2&start=1";
    var imageLink = '<a href="' + publicTransportURL + '" target="_blank">'
      + '<img class="fahrinfo" src=\"' + kiezatlas.imagesFolder + 'fahrinfo.gif" border="0" hspace="20"/></a>';
    infoItem += imageLink;
    infoItem += webpageLink;
    infoItem += '<div class="bottom-line"></div>';
    // 
    jQuery("#scroller").html(infoHeader);
    jQuery("#scroller").append(infoItem);
    //
    // kiezatlas.showInfoContainer();
    kiezatlas.hideDetailsProgressBar();
    // 
    if (kiezatlas.myScroll != undefined) {
      kiezatlas.myScroll.destroy();
      kiezatlas.myScroll = null;
    }
    // 
    /** kiezatlas.myScroll = new iScroll('info-container', { 
      "momentum": true, "hScrollbar": false, "vScrollbar": true, 
      "hideScrollbar" : false, 
      "justScrolled": function () { jQuery("#top-button").show(); },
      "justReset": function () { jQuery("#top-button").hide(); }
    });**/
    // 
    // jQuery("#info-container .title").click(kiezatlas.toggleInfoItem);
    kiezatlas.map.panTo(latLng);
  }

  this.showInfoContainer = function () {
    kiezatlas.closeNotificationDialog();
    // 
    jQuery("#map").animate({ 'margin-top': 0 }, 200, 'linear', function(e){
      jQuery("#navigation").hide();
    });
    jQuery("#info-container").hide();
    jQuery("#top-button").hide();
    // general layout change
    jQuery(".leaflet-control-zoom.leaflet-control").css("margin-top", 0);
    // does calculation, and switchin gof css classes for the current layout
    kiezatlas.handleOrientationChange();
    // 
    // jQuery("#map").removeClass("fullsize");
    jQuery("#map.portrait-height").animate({ height: '60%' }, 200);
    kiezatlas.map.invalidateSize();
    // 
    jQuery("#info-container").click(function (event) { event.stopPropagation(); });
    jQuery("#info-container").animate({ opacity: '1.0' }, 200, 'linear');
    jQuery("#info-container").show();
    kiezatlas.hideAddressBarThroughScrolling();
  }

  this.closeInfoContainer = function () {
    jQuery("#navigation").show()
    jQuery("#map").animate({ 'margin-top': 45 }, 300, 'linear', function(e){});
    jQuery(".leaflet-control-zoom.leaflet-control").css("margin-top", 15);
    // does calculation, and switchin gof css classes for the current layout
    kiezatlas.handleOrientationChange();
    // change map size accordingly..
    // jQuery("#map").addClass("fullsize");
    kiezatlas.map.invalidateSize();
    jQuery("#map.portrait-height").animate({ height: '100%' }, 200, 'linear');
    // 
    jQuery("#info-container").animate({ opacity: '0.0' }, 300, function(e){});
    jQuery("#info-container").hide('slow');
    kiezatlas.hideAddressBarThroughScrolling();
  }
  
  this.scrollInfoDown = function () {
    kiezatlas.myScroll.scrollTo(0, 75, 200, true);
    // jQuery("#top-button").show();
  }
  
  this.scrollInfoTop = function () {
    kiezatlas.myScroll.scrollTo(0, 0, 200, true);
    // jQuery("#top-button").hide();
  }

  this.toggleInfoItem = function (e) {
    var itemId = e.currentTarget.offsetParent.children[2].id;
    jQuery('#'+itemId).toggle();
  }

  this.setupLeafletMarkers = function() {
    // 
    var KiezAtlasIcon = L.Icon.extend({
      options: {
        iconUrl: BASE_PATH+'pages/mobile/ehrenamt/css/locationPointer.png',
        shadowUrl: null, iconSize: new L.Point(40, 40), shadowSize: null,
        iconAnchor: new L.Point(20, 14), popupAnchor: new L.Point(0, 4)
      }
    });
    var myIcon = new KiezAtlasIcon();
    // 
    kiezatlas.markers = new Array(); // helper to keep the refs to all markes once added..
    // 
    for (var i = 0; i < kiezatlas.mapTopics.result.topics.length; i++) {
      // get info locally
      var topic = kiezatlas.mapTopics.result.topics[i];
      var topicId = topic.id;
      var marker = undefined;
      var latlng = undefined;
      var lng = topic.long;
      var lat = topic.lat;
      // sanity check..
      var skip = false;
      if (lat == 0.0 || lng == 0.0) {
        skip = true;
      } else if (lng < -180.0 || lng > 180.0) {
        skip = true;
      } else if (lat < -90.0 || lat > 90.0) {
        skip = true;
      } else if (isNaN(lat) || isNaN(lng)) {
        skip = true;
      }
      if (!skip) {
        latlng = new L.LatLng(parseFloat(lat), parseFloat(lng));
      }
      // 
      if (latlng != undefined) {
        var existingMarker = kiezatlas.getMarkerByLatLng(latlng);
        if (existingMarker != null) {
          marker = existingMarker;
          // add our current, proprietary topicId to the marker-object
          marker.options.topicId.push(topicId);
          var previousContent = marker._popup._content;
          marker.bindPopup(kiezatlas.renderTitle(topic) + previousContent);
        } else {
          marker = new L.Marker(latlng, { 'clickable': true , 'topicId': [topicId] }); // icon: myIcon
          marker.bindPopup(kiezatlas.renderTitle(topic));
        }
        // add marker to map object
        kiezatlas.map.addLayer(marker);
        // reference each marker in kiezatlas.markers model
        kiezatlas.markers.push(marker);
        // 
        marker.on('click', function (e) {
          // 
          this._popup.options.autoPan = true;
          this._popup.options.maxWidth = 160;
          this._popup.options.closeButton = true;
          this.openPopup();
          // 
          // bubbles click handler consumed by onBubbleClick
          // jQuery(".leaflet-popup-content-wrapper").click(kiezatlas.onBubbleClick);
        }, marker);
      }
    }
    // console.log("map.setup => " + kiezatlas.markers.length + 
      // " leaflets for " + kiezatlas.mapTopics.result.topics.length
      // + " loaded topics");
  }
  
  this.renderTitle = function (object) {
    return '<div id="' + object.id + '" onclick="kiezatlas.onBubbleClick(this)" class="topic-name item">'
      + '<b id="' + object.id + '">' + object.name + '&nbsp;&rsaquo;&rsaquo;</b></div>';
  }
  
  this.onBubbleClick = function (e) {
    var topicId = e.id;
    // load geoobject container
    kiezatlas.info(topicId);
  }
  // ### make this helper fit for clusters, multiple topics represented as one marker.
  /* this.focusMarkerByTopicId = function (id) {
    // console.log(kiezatlas.markers[0]);
    for (var i = 0; i < kiezatlas.markers.length; i++) {
      var m = kiezatlas.markers[i];
      if (m.options.topicId[0] == id) {
        kiezatlas.map.setView(m.getLatLng(), kiezatlas.LEVEL_OF_DETAIL_ZOOM);
      }
    }
  } **/
  
  this.getMarkerByLatLng = function (latLng) {
    //
    for (var i = 0; i < kiezatlas.markers.length; i++) {
      var marker = kiezatlas.markers[i];
      if (marker._latlng.equals(latLng)) {
        return marker;
      }
    }
    // 
    return null;
  }
  
  this.clearMarkers = function  () {
    for (var i = 0; i < kiezatlas.markers.length; i++) {
      var m = kiezatlas.markers[i];
      try {
        kiezatlas.map.removeLayer(m);
      } catch (e) {
        console.log("Exception: " + e);
      }
    }
  }
  
  this.getMarkersBounds = function () {
    var bounds = new L.LatLngBounds();
    for (var i = 0; i < kiezatlas.markers.length; i++) {
      var m = kiezatlas.markers[i];
      var lng = m._latlng.lng;
      var lat = m._latlng.lat;
      var skip = false;
      if (lat == 0.0 || lng == 0.0) {
        skip = true;
      } else if (lng < -180.0 || lng > 180.0) {
        skip = true;
      } else if (lat < -90.0 || lat > 90.0) {
        skip = true;
      } else if (isNaN(lat) || isNaN(lng)) {
        skip = true;
      }
      if (!skip) {
        var point = new L.LatLng(parseFloat(lat), parseFloat(lng));
        bounds.extend(point);
      }
    }
    return bounds;
  }
  
  this.getUsersLocation = function (options) {
    // set default options
    if (options == undefined) {
      options =  { "setView" : true, "maxZoom" : kiezatlas.LEVEL_OF_KIEZ_ZOOM };
    }
    // ask browser for location-info
    kiezatlas.map.locate(options);
    jQuery("img.loading").hide();
  }
  
  this.onLocationFound = function(e) {
    // the result of this event/function (setView) comes sometimes out of a sudden, much too late from users perspective
    // console.log("onlocationfound..")
    var radius = e.accuracy / 2;
    if (kiezatlas.locationCircle != undefined) {
      kiezatlas.map.removeLayer(kiezatlas.locationCircle);
    }
    kiezatlas.locationCircle = new L.Circle(e.latlng, radius, { "stroke": true, "clickable": false, "color": "#1d1d1d", 
      "fillOpacity": 0.3, "opacity": 0.3, "weight":2}); // show double sized circle..
    kiezatlas.map.addLayer(kiezatlas.locationCircle, { "clickable" : true });
    kiezatlas.locationCircle.bindPopup("You are within " + radius + " meters from this point");
    kiezatlas.map.setView(e.latlng, kiezatlas.LEVEL_OF_KIEZ_ZOOM);
    // kiezatlas.map.panTo(e.latlng);
    jQuery("img.loading").hide();
  }
  
  this.onLocationError = function (e) {
    kiezatlas.closeNotificationDialog();
    // 
    var notificationDialog = '<div id="message" onclick="kiezatlas.closeNotificationDialog()" class="notification">'
      + '<div id="content">Die Abfrage ihres aktuellen Standorts wurde erfolgreich zur&uuml;ckgewiesen. '
        + 'Mit dem folgenden Link bieten wir ihnen die M&ouml;glichkeit '
        + '<a href="javascript:kiezatlas.showKiezatlasControl()">einen der 12 Berliner Stadtbezirke direkt '
        + 'anzuw&auml;hlen.</a>'
      + '</div></div>';
    // jQuery("#map").append(notificationDialog);
  }
  
  this.closeNotificationDialog = function () {
    jQuery("#message").remove();
  }

  this.showKiezatlasControl = function () {
    // 
    jQuery('a#go-more').click(kiezatlas.hideKiezatlasControl);
    // quick hack providing a berlin district-selection for a city wide map
    var kacontrol = jQuery("#kiezatlas-control");
    // 
    var selectList = '<ul class="ka-select">';
      selectList += '<li id="chbw" class="district-button">Charlottenburg-Wilmersdorf</li>';
      selectList += '<li id="fkreuz" class="district-button">Friedrichshain-Kreuzberg</li>';
      selectList += '<li id="lichtenberg" class="district-button">Lichtenberg (Hohensch&ouml;nhausen)</li>';
      selectList += '<li id="marzahn" class="district-button">Marzahn-Hellersdorf</li>';
      selectList += '<li id="mitte" class="district-button">Mitte (Tiergarten, Wedding)</li>';
      selectList += '<li id="neukoelln" class="district-button">Neuk&ouml;lln</li>';
      selectList += '<li id="pankow" class="district-button">Pankow (Prenzlauer Berg, Wei&szlig;ensee)</li>';
      selectList += '<li id="reinickendorf" class="district-button">Reinickendorf</li>';
      selectList += '<li id="spandau" class="district-button">Spandau</li>';
      selectList += '<li id="steglitz" class="district-button">Steglitz-Zehlendorf</li>';
      selectList += '<li id="tempelhof" class="district-button">Tempelhof-Sch&ouml;neberg</li>';
      selectList += '<li id="treptow" class="district-button">Treptow-K&ouml;penick</li>';
    selectList += '</ul>';
    kacontrol.html(selectList);
    kacontrol.show("fast");
    kacontrol.click(function (event) {
      event.stopPropagation();
      kiezatlas.hideKiezatlasControl();
    });
    // 
    jQuery(".district-button").click(kiezatlas.focusDistrict);
    kiezatlas.closeInfoContainer();
  }
  
  this.focusDistrict = function(e) {
    var districtId = e.target.id;
    // 
    var mittePoint = new L.LatLng(52.520979, 13.407097);
    var fkPoint = new L.LatLng(52.501814, 13.432503);
    var pankowPoint = new L.LatLng(52.552257, 13.43008);
    var chbWilmPoint = new L.LatLng(52.503916, 13.283157);
    var spandauPoint = new L.LatLng(52.53734, 13.168449);
    var stglitZehPoint = new L.LatLng(52.436781, 13.24913);
    var tplHofSchPoint = new L.LatLng(52.469629, 13.383026);
    var nkPoint = new L.LatLng(52.443478, 13.446198);
    var trepKopPoint = new L.LatLng(52.453522, 13.56945);
    var mHelPoint = new L.LatLng(52.534417, 13.565331);
    var lichtenberPoint = new L.LatLng(52.540473, 13.502846);
    var reinickenPoint = new L.LatLng(52.592424, 13.326569);
    // 
    if (districtId == "mitte") {
      kiezatlas.map.setView(mittePoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "pankow") {
      kiezatlas.map.setView(pankowPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "fkreuz") {
      kiezatlas.map.setView(fkPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "chbw") {
      kiezatlas.map.setView(chbWilmPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "spandau") {
      kiezatlas.map.setView(spandauPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "steglitz") {
      kiezatlas.map.setView(stglitZehPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "tempelhof") {
      kiezatlas.map.setView(tplHofSchPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "neukoelln") {
      kiezatlas.map.setView(nkPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "treptow") {
      kiezatlas.map.setView(trepKopPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "marzahn") {
      kiezatlas.map.setView(mHelPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "lichtenberg") {
      kiezatlas.map.setView(lichtenberPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    } else if (districtId == "reinickendorf") {
      kiezatlas.map.setView(reinickenPoint, kiezatlas.LEVEL_OF_DISTRICT_ZOOM);
    }
    kiezatlas.hideKiezatlasControl();
  }

  this.hideKiezatlasControl = function () {
    jQuery('a#go-more').click(kiezatlas.showKiezatlasControl);
    jQuery("#kiezatlas-control").hide();
  }

  this.hideAddressBarThroughScrolling = function () {
    if (navigator.userAgent.match(/Android/i)) {
      window.scrollTo(0,0); // reset in case prev not scrolled  
      var nPageH = $(document).height();
      var nViewH = window.outerHeight;
      if (nViewH > nPageH) {
        nViewH = nViewH / window.devicePixelRatio;
        $('body').css('height', nViewH + 'px');
      }
      window.scrollTo(0,1);
    }
  }

  this.setMap = function(mapObject)  {
    this.map = mapObject;
    //
    this.map.options.touchZoom = true;
  }

  this.setMapTopics = function(topics) {
    this.mapTopics = topics;
  }

  this.setCityMapId = function (topicId) {
    this.cityMapId = topicId;
  }

  this.setSelectedTopic = function(topic) {
    this.selectedTopic = topic;
  }

  this.setLayer = function(markerLayer) {
    this.layer= markerLayer;
  }

  this.setMapLayer = function(mapLayer) {
    this.mapLayer= mapLayer;
  }

  this.setSelectedCriteria= function(id) {
    this.selectedCriteria = id;
  }

  this.setWorkspaceCriterias = function(crits) {
    this.workspaceCriterias = crits;
  }
  
  this.setServerUrl = function(url) {
    this.serverUrl = url;
  }

  this.setServiceUrl = function(url) {
    this.serviceUrl = url;
  }
  
  this.setIconsFolder = function(path) {
    this.iconsFolder = path;
  }
  
  this.setImagesFolder = function(path) {
    this.imagesFolder = path;
  }
  
  this.setToCurrentBounds = function () {
    kiezatlas.markersBounds = kiezatlas.getMarkersBounds();
    kiezatlas.map.fitBounds(kiezatlas.markersBounds);
  }

  this.popHistory = function (state) {
    // simulate the back and forth navigation...
    if (!this.historyApiSupported) {
      return;
    } else {
      // 
      if (state.name == "loaded") {
        kiezatlas.loadCityMap(state.parameter[0], state.parameter[1]);
      } else if (state.name == "info") {
        kiezatlas.info(state.parameter);
      }
      // console.log(state);
    }
  }

  this.pushHistory = function (state, link) {
    // 
    if (!this.historyApiSupported) {
      return;
    }
    // build history entry
    var history_entry = {state: state, url: link};
    // console.log(history_entry);
    // push history entry
    window.history.pushState(history_entry.state, null, history_entry.url);
  }

  this.handleOrientationChange = function(width) {
    var fHeight = kiezatlas.windowHeight();
    var fWidth = kiezatlas.windowWidth();
    if (fHeight > fWidth) { // portrait
      // modify css of info-container
      jQuery("#info-container").removeClass("info-container-panorama");
      jQuery("#info-container").removeClass("info-container-panorama-big");
      jQuery("#info-container").addClass("info-container-portrait");
      // map
      jQuery("#map").removeClass("panorama-width-big");
      jQuery("#map").removeClass("panorama-width");
      jQuery("#map").addClass("portrait-height");
      // 
      // view flag for different interaction logic
      kiezatlas.panoramaOrientation = false;
    } else if (fHeight < fWidth) { // panorama
      // modify css of info-container and map-container
      jQuery("#info-container").removeClass("info-container-portrait");
      if (fWidth > 810) {
        jQuery("#info-container").addClass("info-container-panorama-big");
        jQuery("#map").addClass("panorama-width-big");
      } else {
        jQuery("#info-container").addClass("info-container-panorama");
        jQuery("#map").addClass("panorama-width");
      }
      jQuery("#map").removeClass("portrait-height");
      // view flag for different interaction logic
      kiezatlas.panoramaOrientation = true;
    }
  }
  
  this.windowHeight = function () {
    if (self.innerHeight) {
      return self.innerHeight;
    }
    if (document.documentElement && document.documentElement.clientHeight) {
      return jQuery.clientHeight;
    }
    if (document.body) {
      return document.body.clientHeight;
    }
    return 0;
  }
  
  this.windowWidth = function () {
    if (self.innerWidth) {
      return self.innerWidth;
    }
    if (document.documentElement && document.documentElement.clientWidth) {
      return jQuery.clientWidth;
    }
    if (document.body) {
      return document.body.clientWidth;
    }
    return 0;
  }
  
  // Info-Container utility method
  this.stripFieldsContaining = function (topic, fieldName) {
    var newProps = new Array();
    for (var it=0; it < topic.properties.length; it++) {
      if (topic.properties[it].name.indexOf(fieldName) == -1) {
        newProps.push(topic.properties[it]);
      } else if (topic.properties[it].name.indexOf("Email") != -1) {
        // save Email Address Property being stripped by a command called "Address""
        newProps.push(topic.properties[it]);
      } else {
        //
      }
    }
    topic.properties = newProps;
    return topic;
  }
  
  this.getTopicAddress = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].name == "Address / Street" && topic.properties[i].value != "") {
      // via related Address Topic
      return topic.properties[i].value;
      } else if (topic.properties[i].name == "Straße" && topic.properties[i].value != "") {
      // via related Street PropertyField
      return topic.properties[i].value;
      }
    }
    return "";
  }
  
  this.getImageSource = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].name == "Image / File" && topic.properties[i].value != "") {
        return topic.properties[i].value;
      }
    }
    return "undefined";
  }

  this.getTopicPostalCode = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].name == "Address / Postal Code") {
        return topic.properties[i].value; // + ' Berlin<br/>';
      }
    }
    return "";
  }

  this.getPersonInfo = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].type == 1) {
        // DM Property Type Multi Value
        for ( var k=0; k < topic.properties[i].values.length; k++ ) {
          value = topic.properties[i].values[k].name;
          if (topic.properties[i].name == "Person") {
            return 'AnsprechpartnerIn: <b>' + value + '</b>';
          }
        }
      }
    }
  }

  this.getWebpageLink = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].type == 1) {
        // DM Property Type Multi Value
        for ( var k=0; k < topic.properties[i].values.length; k++ ) {
          value = topic.properties[i].values[k].name;
          if (topic.properties[i].name == "Webpage") {
	          var originId = kiezatlas.getTopicOriginId(topic)
            value = kiezatlas.makeEhrenamtsLink(value, originId);
            return '<div class="webpage-button">' + value + '</div>';
          }
        }
      }
    }
  }

  this.getTopicOriginId = function (topic) {
    for (var i=0; i < topic.properties.length; i++) {
      if (topic.properties[i].name == "OriginId") {
        return topic.properties[i].value; // + ' Berlin<br/>';
      }
    }
    return "";
  }

  this.getTopicCity = function (topic) {
    for (var at=0; at < topic.properties.length; at++) {
      // resultHandler.append('<tr><td>'+topic.properties[i].label+'</td><td>'+topic.properties[i].value+'</td></tr>');
      if (topic.properties[at].name == "Address / City") {
        return topic.properties[at].value; // + ' Berlin<br/>';
      } else if (topic.properties[at].name == "Stadt") {
        return topic.properties[at].value;
      }
    }
    return null;
  }
  
  this.getLatLng = function (topic) {
    var lat = undefined;
    var lng = undefined;
    // 
    for (var it=0; it < topic.properties.length; it++) {
      // resultHandler.append('<tr><td>'+topic.properties[i].label+'</td><td>'+topic.properties[i].value+'</td></tr>');
      if (topic.properties[it].name == "LAT") {
        lat = topic.properties[it].value; // + ' Berlin<br/>';
      } else if (topic.properties[it].name == "LONG") {
        lng = topic.properties[it].value;
      }
    }
    return new L.LatLng(lat, lng);
  }
  
  this.makeEhrenamtsLink = function (url, originId) {
    // quick fix to correct automaticyll imported links to new external mobile-website
    /** var indexForMobileURL = url.indexOf("g.cfm");
    var firstPartURL = url.substr(0, indexForMobileURL + 1);
    var secondPartURL = url.substr(indexForMobileURL + 1);
    var newMobileURL = firstPartURL + "_mobil" + secondPartURL; **/
    var url = 'http://www.berlin.de/buergeraktiv/engagieren/buerger/ehrenamtssuche/index.cfm?'
      + 'dateiname=ehrenamt_projektbeschreibung_mobil.cfm&anwender_id=5&projekt_id=' + originId + '&cfide=0.662606781956'
    urlMarkup = '<a href="' + url + '" target="_blank">.. weiter zur T&auml;tigkeitsbeschreibung</a>';
      //  + '<img src="css/link_extern.gif" alt="(externer Link)" border="0" height="11" width="12"/>
    // else urlMarkup = '<a href="'+url+'" target="_blank">'+label+'</a>';
    return urlMarkup
  }
  
  this.makeWebpageLink = function (url, label) {
    // quick fix to correct automaticyll imported links to new external mobile-website
    var indexForMobileURL = url.indexOf("g.cfm");
    var firstPartURL = url.substr(0, indexForMobileURL + 1);
    var secondPartURL = url.substr(indexForMobileURL + 1);
    var newMobileURL = firstPartURL + "_mobil" + secondPartURL;
    urlMarkup = '<a href="' + newMobileURL + '" target="_blank">' + label + '</a>';
      //  + '<img src="css/link_extern.gif" alt="(externer Link)" border="0" height="11" width="12"/>
    // else urlMarkup = '<a href="'+url+'" target="_blank">'+label+'</a>';
    return urlMarkup
  }

  this.makeEmailLink = function (url, label) {
    urlMarkup = '<a href="mailto:' + url + '" target="_blank">' + label + '</a>';
    return urlMarkup
  }

  this.getCategory = function(categoryId) {
    var category = {};
    for (var i = 0; i < this.workspaceCriterias.result[this.selectedCriteria].categories.length; i++) {
      // looping over all cats of a crit
      var id = this.workspaceCriterias.result['' + this.selectedCriteria + ''].categories[i].catId;
      if (id == categoryId) {
        category.icon = this.workspaceCriterias.result['' + this.selectedCriteria + ''].categories[i].catIcon;
        category.name = this.workspaceCriterias.result['' + this.selectedCriteria + ''].categories[i].catName;
        return category;
      }
    }
    return null;
  }

}

