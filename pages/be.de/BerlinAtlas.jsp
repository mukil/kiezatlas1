<%@ include file="../KiezAtlas.jsp" %>
<%
	BaseTopic map = (BaseTopic) session.getAttribute("map");
	BaseTopic workspace = (BaseTopic) session.getAttribute("workspace");
	String mapTopics = (String) session.getAttribute("mapTopics");
	String workspaceCriterias = (String) session.getAttribute("workspaceCriterias");
	String workspaceImprint = (String) session.getAttribute("workspaceImprint");
	String workspaceLogo = (String) session.getAttribute("workspaceLogo");
	String mapAlias = (String) session.getAttribute("mapAlias");
	String workspaceHomepage = (String) session.getAttribute("workspaceHomepage");
	String searchTerm = (String) session.getAttribute("searchTerm");
	String originId = (String) session.getAttribute("originId");
	String topicId = (String) session.getAttribute("topicId");
	String catIds = (String) session.getAttribute("categories");
	String baseLayer = (String) session.getAttribute("baseLayer");
	Integer critIndex = (Integer) session.getAttribute("critIndex");
	// String basePath = "http://localhost:8080/kiezatlas";
	String basePath = "http://www.berlin.de/atlas";
	// String basePath = "http://www.kiezatlas.de/maps/embed/012";
	String resourcePath = "http://www.kiezatlas.de/maps/embed/012";
	//
	String title = "" + map.getName() + " im Kiezatlas";

%>
<% startMaps(session, out); %>
<head>
  <!-- meta http-equiv="Content-type" content="text/html; charset=ISO-8859-15"/-->
  <meta http-equiv="content-type" content="text/html charset=UTF-8"/>
  <title> <%= title %> </title>
  <!--[if gte IE 7]>
    <style type="text/css">@import url(<%= resourcePath %>/landmaps-ie.css);</style>
  <![endif]-->
  <link rel="stylesheet" href="<%= resourcePath %>/landmaps.css" type="text/css"> <!-- fix: needs absolute url for deployment -->
  <link rel="stylesheet" href="<%= resourcePath %>/theme/default/style.css" type="text/css"> <!-- fix: needs absolute url for deployment -->
  <link rel="stylesheet" href="<%= resourcePath %>/theme/default/google.css" type="text/css"> <!-- fix: needs absolute url for deployment -->
  <script type="text/javascript" src="<%= resourcePath %>/OpenLayers.js"></script> <!-- fix: needs absolute url for deployment -->
  <script type="text/javascript" src="<%= resourcePath %>/CustomLayerSwitcher.js"></script> <!-- fix: needs absolute url for deployment -->
  <script type="text/javascript" src="<%= resourcePath %>/jquery.min.js"></script> <!-- fix: needs absolute url for deployment -->
  <script type="text/javascript" src="<%= resourcePath %>/kiezatlas.js"></script> <!-- fix: needs absolute url for deployment -->
  <script type="text/javascript">
    var basePath = '<%= basePath %>';
    var mapTitle = '<%= map.getName() %>';
    var mapAlias = '<%= mapAlias %>';
    var topicId = '<%= map.getID() %>'; // unlike the topicId param from the requestURL
    var baseLayer = '<%= baseLayer %>';
    var workspaceId = '<%= workspace.getID() %>';
    // options
    var crtCritIndex = <%= critIndex %>;
    var cats = '<%= catIds %>';
    var catIds = cats.split(",");
    var searchTerm = '<%= searchTerm %>';
    var linkToTopicId = '<%= topicId %>';
    var linkTo = '<%= originId %>';
    // body
    var mapTopics = <%= mapTopics %>; // null; //eval('(' + <?php echo json_encode($mapTopics) ?> + ')');
    var workspaceCriterias = <%= workspaceCriterias %>; // eval('(' + <?php echo json_encode($workspaceCriterias) ?> + ')');
    kiezatlas.setCityMapId(topicId);
    kiezatlas.setWorkspaceCriterias(workspaceCriterias);
    // var workspaceInfos = null;
    var workspaceHomepage = '<%= workspaceHomepage %>';
    var workspaceLogo = '<%= workspaceLogo %>';
    var workspaceImprint = '<%= workspaceImprint %>';
    var myLayerSwitcher = null;
    var myNewLayer = null;
    // var loadWorkspaceInfos = null;
    var districtLayer;
    var gMarkers = [];
    var allFeatures = [];
    var hotspots = [];
    var districtNames = [];
    var markerGroupIds = new Array();
    var helpVisible = false;
    // var slimWidth = false;
    // var totalWidth = 1000; // 953
    var map = "";
    var bounds = "";
    var markerLayer = "";
    // gui elements
    var sideBarVisible = true;
    //
    var debug = false;
    if (debug) {
      var debug_window = window.open('','','width=400,height=600,scrollbars=1');
    }
    // 212: ABQIAAAAfPcn9RYcEecc-d1iHvHCIRTIIy1nlKSG8dPHOYwqi5UhuTLB2hT534VlXBVGCIqcQXQ-a4z45J0w6A
    // var gKey = 'ABQIAAAAyg-5-YjVJ1InfpWX9gsTuxRa7xhKv6UmZ1sBua05bF3F2fwOehRUiEzUjBmCh76NaeOoCu841j1qnQ';
    var gKey = 'ABQIAAAADev2ctFkze28KEcta5b4WBSQDgFJvORzMhuwLQZ9zEDMQLdVUhTWXHB2vS0W0TdlEbDiH_qzhBEZ5A';
    //
    jQuery(document).ready(function() {
      onBerlinDe = true;
      // baseUrl = "http://www.berlin.de/atlas/";
      // SERVICE_URL = baseUrl + "rpc/";
      // register resize
      jQuery(window).resize(function() {
        handleResize();
      });
      if (onBerlinDe & workspaceCriterias.result.length > 4) districtNames = workspaceCriterias.result[4].categories;
      // ### the following two are used in kiezatlas.js..
      var onEventMap = (mapTitle.indexOf("Veranstaltungen Ehrenamt Berlin") != -1) ? true : false;
      var onProjectMap = (mapTitle.indexOf("Ehrenamt Berlin") != -1) ? true : false;
      // check if a special criteria was set through an entry url
      if (crtCritIndex >= workspaceCriterias.result.length) {
        crtCritIndex = 0;// workspaceCriterias.result.length;
      }
      // cityMap setup
      bounds = calculateInitialBounds(mapTopics);
      // after the dom is loaded we can init our parts of the app
      jQuery(window).load(function() {
        // if (console != undefined) console.log("testLog...");
        document.namespaces; // some curios ie workaround for openlayers
        setWorkspaceInfos(workspaceHomepage, workspaceLogo, workspaceImprint, mapTitle);
        setCityMapNameWs(mapTitle, workspaceId);
        // setCityMapNameWs(mapTitle, workspaceId);
        handleResize(); // do the layout
        // setup mapObject and layers
        openLayersInit(bounds, baseLayer);
        // ...
        /// renderCritCatListing(crtCritIndex);
        renderCritCatListing(crtCritIndex); //, baseUrl, mapAlias, workspaceCriterias, crtCritIndex, mapTitle, onBerlinDe);
        // create an array of OpenLayoers.Marker based on mapTopics
        gMarkers = setupOpenMarkers(mapTopics);
        // initialize Features and their control
        initLayerAllFeatures(gMarkers, map);
        initBerlinDistrictsLayer();
        // inputFieldBehaviour();
        // check if a special projectId was given through the entry url
        reSetMarkers(myNewLayer);
        if (searchTerm != 'null') {
          searchRequest(searchTerm);
        } else if (linkTo != 'null') {
          selectAndShowInMap(linkTo, false);
        } else if (linkToTopicId != 'null') {
          selectAndShowInMap(linkToTopicId, true);
        } else if (catIds.length > 0) {
          for (var catIdx = 0; catIdx < catIds.length; catIdx++) {
            var catId = catIds[catIdx];
            // pre-select the categories encoded in url
            toggleMarkerGroups(catId);
          }
        }
        // show all markers at the beginning for these two scenarios..
        if (onProjectMap || onEventMap) {
          showAllMarker();
        }
        map.events.register("zoomend", map, redrawAfterZoomOperation);
        // map.maxExtent = bounds;
        map.raiseLayer(myNewLayer);
        if (jQuery.browser.msie) handleResize(); // fix the layout for ie..
      });
    });
  </script>
</head>
  <body>
    <div id="kiezatlas" style="visibility: hidden;">
      <div id="kaheader">
		    <div id="focusInput">
                <form id="autoLocateInputField">
                    <label for="streetNameField">In der N&auml;he von</label>
                    <input id="streetNameField" disabled="true" type="text" size="18" placeholder="Stra&szlig;enname / Hnr."/>
                    <a class="go-focus">OK</a>
                    <label for="streetNameField">im</label>
                    <span id="mapName"></span>
                </form>
		    </div>
        <div id="cityMapDialog" style="visibility: hidden;"></div>
		    <div id="searchInput">
		      <form id="searchForm" action="javascript:searchRequest()">
			    <label for="searchInputField">Suche</label>
			    <input id="searchInputField" type="text" placeholder="Name / Stichwort" size="18"/>
          <a href="javascript:searchRequest()" class="go-search">OK</a>
		      </form>
		    </div>
		    <div id="headerButtons">
          <img id="fullWindowButton" title="Volle Fenstergr&ouml;&szlig;e nutzen" onclick="javascript:toggleFullWindow()" width="22" height="22"
               src="http://www.kiezatlas.de/maps/embed/img/Fullscreen.png" alt="Volle Fenstergr&ouml;&szlig;e nutzen"/>
			    <img id="resizeButton" title="Seitenleiste ausblenden" onclick="javascript:handleSideBar()" src="http://www.kiezatlas.de/pages/be.de/img/go-last.png" width="22" height="22" alt="Seitenleiste ausblenden">
		    <!-- </div> -->
		    </div>
      </div>
      <div id="map"></div>
	    <div id="focusAlternatives"></div>
      <div id="mapControl">&nbsp;
        <a href="javascript:showAllMarker();" id="toggleMarkerHref">
          Alles einblenden
        </a>
		    <a href="javascript:updateVisibleBounds(null, true, null, true);" id="resetMarkerHref">
          Karte zur&uuml;cksetzen
        </a>
        <span id="moreLabel">
          &nbsp;Mehr..
          <img src="http://www.kiezatlas.de/client/images/dropdown-btn.png" title="Mehr.." alt="Button: Mehr..">
        </span>
        <div id="mapSwitcher" style="position: absolute; visibility: hidden;"></div>
      </div>
      <div id="memu" style="visibility:hidden;"></div>
      <div id="navPanel"></div>
      <div id="sideBar">
		    <div id="sideBarCriterias"></div>
		    <div id="sideBarCategories"></div>
        <div id="progContainer"></div>
      </div>
      <div id="kafooter">
        <a href="http://www.berlin.de/buergeraktiv/">Impressum</a> und <a href="http://ehrenamt.index.de">Haftungshinweise</a><br/>
        <b> powered by <a href="http://www.kiezatlas.de">Kiezatlas</a></b>
      </div>
      <div id="sideBarControl"></div> <!-- onclick="javascript:handleSideBar();" onclick="javascript:showDialog(false)"  -->
      <div id="dialogMessage" style="visibility: hidden;" title="Dialog schlie&szlig;en">
          <div id="closeDialog" onclick="javascript:showDialog(false)">(Dialog schlie&szlig;en)</div>
          <b id="modalTitle" class="redTitle"></b>
          <p id="modalMessage"></p>
      </div>
    </div>
  </body>
</html>
