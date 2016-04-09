<!DOCTYPE html>
<html>
<head>
  <title>Ehrenamtsnetz Web App (Karte)</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <!-- what follows shall be an iphone tweak, found via http://stackoverflow.com/questions/4068559 -->
  <meta name="apple-mobile-web-app-capable" content="no" />
  <meta names="apple-mobile-web-app-status-bar-style" content="black-translucent" />

  <meta property="og:type" content="website"/>
  <meta property="og:title" content="Berliner Ehrenamtsnetz-App"/>
  <meta property="og:image" content="http://m.kiezatlas.de/ehrenamt/fb_ehrenamts_screen.jpg"/>
  <meta property="og:url" content="http://m.kiezatlas.de/ehrenamt"/>
  <meta property="og:site_name" content="Berliner Ehrenamtsnetz-App"/>
  <meta property="og:description" content="Diese kostenlose App f&uuml;r Ihr Smartphone liefert Ihnen tagesaktuelle Infos zu ehrenamtlichen Veranstaltungen und Einsatzm&ouml;glichkeiten in Berlin."/>

  <link rel="stylesheet" href="/kiezatlas/pages/vendor/leaflet/dist/leaflet.css"/>
  <!--[if lte IE 8]><link rel="stylesheet" href="//www.kiezatlas.de/maps/statics/vendor/leaflet7/leaflet.ie.css" /><![endif]-->
  <link rel="stylesheet" href="/kiezatlas/pages/mobile/ehrenamt/css/sitestyle.css"/>
  <script src="/kiezatlas/pages/vendor/jquery-1.9.1.min.js"></script>
  <script src="/kiezatlas/pages/vendor/leaflet/dist/leaflet.js"></script>
  <script src="/kiezatlas/pages/mobile/ehrenamt/ka-mobile-ehrenamt-SNAPSHOT.js"></script>
</head>
<body>
  <div id="map" class="fullsize">loading citymap ...</div>
  <script>
    // 
    var cityMapEhrenamtId = "t-331302";
    var workspaceEhrenamtId = "t-331306";
    // 
    var workspaceEventId = "t-453282";
    var cityMapEventId = "t-453286";
    // 
    kiezatlas.setServiceUrl("http://www.kiezatlas.de/rpc/");
    kiezatlas.setIconsFolder("http://www.kiezatlas.de/client/icons/");
    kiezatlas.setImagesFolder("http://www.kiezatlas.de/client/images/");
    var map = new L.Map('map');
    kiezatlas.setMap(map);
    kiezatlas.renderSite();
    kiezatlas.executeBrowserSpecificCrap();
    kiezatlas.loadCityMap(cityMapEhrenamtId, workspaceEhrenamtId);
    kiezatlas.loadMapTiles();
  </script>
  <div id="navigation">
    <a id="go-list" class="list-button" href="/kiezatlas/mobile/ehrenamt/list/">
        <img src="/kiezatlas/pages/mobile/ehrenamt/go-list-button.png"/>
    </a>
    <a id="go-do" class="selected" href="javascript:kiezatlas.loadCityMap(cityMapEhrenamtId, workspaceEhrenamtId)">
      Aktivit&auml;ten
    </a>
    <a id="go-event" href="javascript:kiezatlas.loadCityMap(cityMapEventId, workspaceEventId)">
      Veranstaltungen Heute
    </a>
  </div>
  <div id="info-container" class="info-container-portrait">
    <div id="scroller"></div>
    <img id="close-button" width="15px" src="/kiezatlas/pages/mobile/ehrenamt/css/dialog-close15.png" onclick="javascript:kiezatlas.closeInfoContainer()">
    <!-- div id="down-button" onclick="javascript:kiezatlas.scrollInfoDown()">Scroll Down</div-->
    <img id="top-button" width="24px" src="/kiezatlas/pages/mobile/ehrenamt/css/topscroll.png" onclick="javascript:kiezatlas.myScroll._myResetPos()">
  </div>
  <div id="kiezatlas-control"></div>
  <img src="http://stats.kiezatlas.de/piwik.php?idsite=2&amp;rec=1&action_name=ehrenamt_front" style="border:0 display: none;" alt="" />
</body>
</html>
