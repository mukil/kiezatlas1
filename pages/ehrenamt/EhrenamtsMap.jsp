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
    Object onProjectMap = session.getAttribute("onProjectMap");
	Object onEventMap = session.getAttribute("onEventMap");
	Integer critIndex = (Integer) session.getAttribute("critIndex");
    // note: this is just used as a base to construct our history-state-url
	// String basePath = "http://localhost:8080/kiezatlas/new-atlas";
	String basePath = "http://www.berlin.de/atlas-i9";
	String title = "" + map.getName() + " im Kiezatlas - berlin.de";

%>
<% startMaps(session, out); %>
<head>
  <meta http-equiv="Content-type" content="text/html; charset=ISO-8859-15"/>
  <!-- meta http-equiv="content-type" content="text/html charset=UTF-8"/-->
  <title> <%= title %> </title>

    <link rel="stylesheet" href="http://www.kiezatlas.de/maps/embed/014/libs/leaflet/dist/leaflet.css"/>
    <link rel="stylesheet" href="http://www.kiezatlas.de/maps/embed/014/css/sitestyle.css"/>

    <script src="http://www.kiezatlas.de/maps/embed/014/libs/ka-ehrenamt-2.0.js"></script>
    <script src="http://www.kiezatlas.de/maps/embed/014/libs/jquery-1.9.1.min.js"></script>
    <script src="http://www.kiezatlas.de/maps/embed/014/libs/leaflet/dist/leaflet.js"></script>


  <script type="text/javascript">

    // initialize client model
    var basePath = '<%= basePath %>';
    var mapTitle = '<%= map.getName() %>';
    var mapAlias = '<%= mapAlias %>';
    var mapId = '<%= map.getID() %>'; // unlike the topicId param from the requestURL
    // var baseLayer = '<%= baseLayer %>';
    var workspaceId = '<%= workspace.getID() %>';
    var onProjectMap = JSON.parse('<%= onProjectMap %>');
    var onEventMap = JSON.parse('<%= onEventMap %>');
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
    var workspaceHomepage = '<%= workspaceHomepage %>';
    var workspaceLogo = '<%= workspaceLogo %>';
    var workspaceImprint = '<%= workspaceImprint %>';

    $(document).ready(function(e) {

        // 1) Setup kiezatlas-JavaScript Object (these are basically just used for AJAX-History Management)
        kiezatlas.set_base_url(basePath)
        kiezatlas.set_map_web_alias(mapAlias)
        kiezatlas.set_map_topics(mapTopics)
        kiezatlas.set_map_topic_id(mapId)
        // important: the kiezatlas-webservice endpoint must be reachable via the same domain as this interface
        kiezatlas.set_ajax_service_endpoint("/atlas/rpc/")

        // 2) Initialize map related gui elements on-top of our DOM
        kiezatlas.render_mobile_city_map_view()

        // 3) Debug PathInfo for deep linking application-initialization
        /** console.log("Debug Deep Link Parameters:")
        console.log("Kiezatlas Topic ID: " + linkToTopicId)
        console.log("Ehrenamts Entry ID: " + linkTo)
        console.log("Selected Category IDs: " + catIds)
        console.log("Selected Criteria Index: " + crtCritIndex)
        console.log("Search Term: " + searchTerm) **/

        // 4) Initalized our basic page layout
        kiezatlas.render_page()

        // 5) Hook up page resize-handler to re-paint our layout
        $(window).resize(kiezatlas.render_leaflet_container)

        // 6) Hook up javascript history management
        if (window.history.pushState) {
            window.addEventListener("popstate", function(e) {
                // Note: state is null if a) this is the initial popstate event or
                // b) if back is pressed while the begin of history is reached.
                if (e.state) kiezatlas.pop_history(e.state)
            })
        }

        // 7) Initialize a specific view of our app
        if (searchTerm != 'null') {

            $('#simple-search').val(searchTerm)
            kiezatlas.do_search()

        } else if (linkTo != 'null' && linkTo !== "undefined") { // string "undefined"

            kiezatlas.select_and_show_in_map(linkTo, false)

        } else if (linkToTopicId != 'null' && linkToTopicId !== "undefined") { // string "undefined"

            kiezatlas.select_and_show_in_map(linkToTopicId, true)

        } else if (catIds.length > 0) {

            // Set criteria
            kiezatlas.set_selected_criteria(crtCritIndex)
            kiezatlas.render_criteria_list(false)
            // Clear map
            kiezatlas.hide_all_topics_in_map()
            kiezatlas.deselect_all_categories_of_current_criteria()
            // Toggle all given categories
            for (var catIdx = 0; catIdx < catIds.length; catIdx++) {
                var catId = catIds[catIdx]
                // pre-select the categories encoded in url
                kiezatlas.toggle_category(catId)
            }
        }

        // 8) Straighten up the layout (based on calculations)
        $(window).load(function (e) {

            kiezatlas.show_page_panel()

        })

    })

  </script>
</head>
  <body>

    <div id="kiezatlas">
        <div id="search-controls">
            <form action="javascript:kiezatlas.search_near_by_input();" id="near-by-form" accept-charset="UTF-8">
                <label for="near-by" id="near-by-label">In der N&auml;he von</label>
                <input id="near-by" type="text" placeholder="Stra&szlig;enname / Hnr."></input>
                <input id="do-nearby" type="submit" class="btn" value="Ok"></input>
            </form>
            <div id="street-alternatives"></div>
            <form action="javascript:kiezatlas.do_search();" id="search-form" accept-charset="ISO-8859-1">
                <input id="simple-search" type="text" placeholder="Name / Stichwort" ></input>
                <input id="do-search" type="submit" class="btn" value="Suchen"></input>
            </form>
        </div>
        <div id="overview">
            <div id="map"></div>
            <div id="show-all">Alles einblenden</div>
            <div id="reset">Karte zur&uuml;cksetzen</div>
        </div>
        <div id="details">
            <div id="branding-area" class="html5-header header">
                <img class="html5-figure main-image image"/>
            </div>
            <div id="criteria-list" class="html5-header header"></div>
            <div id="cross-links-area"></div>
            <div id="page-body" class="html5-section body"></div>
            <div id="print-sign">Drucken</div>
        </div>
        <div id="footer-area">
            <span id="imprint"><a href="" id="imprint-link">Impressum / Haftungshinweise</a></span><br/>
            <span id="powered-by">
                <b><a href="http://www.kiezatlas.de">Kiezatlas</a></b> is powered by
                <a href="http://www.deepamehta.de">DeepaMehta</a>
            </span>
        </div>
    </div>

  </body>
</html>
