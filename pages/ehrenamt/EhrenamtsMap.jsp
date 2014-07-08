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
	String basePath = "http://www.berlin.de/atlas";
	String title = "" + map.getName() + " im Kiezatlas - berlin.de";

%>
<% startNewMaps(session, out); %>
<head>
    <meta http-equiv="Content-type" content="text/html; charset=ISO-8859-15"/>
    <title> <%= title %> </title>

    <link rel="stylesheet" href="http://www.kiezatlas.de/maps/embed/014/libs/leaflet7/leaflet.css"/>
    <link rel="stylesheet" href="http://www.kiezatlas.de/maps/embed/014/css/sitestyle.css"/>

    <script src="http://www.kiezatlas.de/maps/embed/014/libs/ka-ehrenamt-2.0.js"></script>
    <script src="http://www.kiezatlas.de/maps/embed/014/libs/jquery-1.9.1.min.js"></script>
    <script src="http://www.kiezatlas.de/maps/embed/014/libs/leaflet7/leaflet.js"></script>

    <script type="text/javascript">

        // initialize client model
        var basePath = '<%= basePath %>';
        var mapTitle = '<%= map.getName() %>';
        var mapAlias = '<%= mapAlias %>';
        var mapId = '<%= map.getID() %>'; // unlike the topicId param from the requestURL
        // var baseLayer = '<%= baseLayer %>';
        var workspaceId = '<%= workspace.getID() %>';
        var onProjectMap = $.parseJSON('<%= onProjectMap %>');
        var onEventMap = $.parseJSON('<%= onEventMap %>');
        // options
        var crtCritIndex = <%= critIndex %>;
        var cats = '<%= catIds %>';
        var catIds = cats.replace("null%2C", "").replace("null", "").split(",");
        var searchTerm = '<%= searchTerm %>';
        var linkToTopicId = '<%= topicId %>';
        var linkTo = '<%= originId %>';
        // body
        var mapTopics = <%= mapTopics %>;
        var workspaceCriterias = <%= workspaceCriterias %>;
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

            // 2) Initialize map related gui elements on-top of our DOM (and markers on js-client-side)
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

            kiezatlas.show_all_topics_in_map(true)
            kiezatlas.select_all_categories_of_current_criteria()

            // 5) Initialize a specific view of our app
            if (searchTerm != 'null') {

                // Clear map
                kiezatlas.hide_all_topics_in_map()
                kiezatlas.deselect_all_categories_of_current_criteria()

                // Show search results
                $('#simple-search').val(searchTerm)
                kiezatlas.do_search()

            } else if (linkTo != 'null' && typeof linkTo !== "undefined") { // string "undefined"

                // Clear map
                kiezatlas.hide_all_topics_in_map()
                kiezatlas.deselect_all_categories_of_current_criteria()

                // Show item
                kiezatlas.select_and_show_in_map(linkTo, false)

            } else if (linkToTopicId != 'null' && typeof linkToTopicId !== "undefined") { // string "undefined"

                // Clear map
                kiezatlas.hide_all_topics_in_map()
                kiezatlas.deselect_all_categories_of_current_criteria()

                // Show item
                kiezatlas.select_and_show_in_map(linkToTopicId, true)

            } else if (cats.indexOf("t-") != -1) {

                // Set criteria
                kiezatlas.set_selected_criteria(crtCritIndex)
                kiezatlas.render_criteria_list(false)

                // Clear map
                kiezatlas.hide_all_topics_in_map()
                kiezatlas.deselect_all_categories_of_current_criteria()

                // Show all topics in given categories
                for (var catIdx = 0; catIdx < catIds.length; catIdx++) {
                    var catId = catIds[catIdx]
                    // pre-select the categories encoded in url
                    kiezatlas.toggle_category(catId)
                }

            }

            // 6) Straighten up the layout (based on calculations)
            $(window).load(function (e) {
                kiezatlas.render_leaflet_container()
                kiezatlas.show_page_panel()
                // 7) Hook up javascript history management
                if (window.history.pushState) {
                    window.addEventListener("popstate", function(e) {
                        // Note: state is null if a) this is the initial popstate event or
                        // b) if back is pressed while the begin of history is reached.
                        if (e.state) kiezatlas.pop_history(e.state)
                    })
                }
                // 8) Hook up page resize-handler to re-paint our layout
                $(window).resize(kiezatlas.render_leaflet_container) // in some browsers this registration triggers?
            })

        })

    </script>
</head>
    <body>
        <div class="row html5-header content-header">
            <div class="span5">
                <div class="html5-section section-logo without-logo">
                    <div class="html5-section text">
                        <a href="<%= basePath %>ehrenamt">
                            <span class="institution">B&uuml;rgerschaftliches Engagement</span>
                            <span class="title"><%= map.getName() %></span>
                        </a>
                    </div>
                </div>
            </div>
            <div class="span7">
                <div class="html5-nav meta-navi"></div>
                <div class="html5-section search navbar">
                    <div>
                        <button class="btn btn-navbar collapsed" type="button" data-toggle="collapse" data-target=".search-collapse"></button>
                    </div>
                    <div class="nav-collapse search-collapse collapse">
                        <form action="javascript:kiezatlas.do_search();" id="search-form" accept-charset="ISO-8859-15">
                            <input id="simple-search" type="text" placeholder="Geben Sie ein Stichwort ein"
                                title="Hier koennen Sie einen Text eingeben, um in diesem Stadtplan nach Inhalten zu suchen"></input>
                            <input id="do-search" type="submit" class="btn" value="Suchen"></input>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        <div class="row">
            <div class="span12">
                <div class="content-navi-wrapper navbar">
                    <div class="html5-nav content-navi-top">
                        <div class="nav-collapse mainnav-collapse collapse">
                            <ul class="nav level1">
                                <li class="citymap-navi ehrenamt-projects"><a title="Gehe zur Ansicht aller ehrenamtlichen Einsatzorte im Ehrenamtsnetz"
                                    href="<%= basePath %>ehrenamt">Einsatzm&ouml;glichkeiten</a></li>
                                <li class="citymap-navi ehrenamt-events"><a title="Gehe zur Ansicht der heutigen Veranstaltungen im Ehrenamtsnetz"
                                    href="<%= basePath %>veranstaltungen-ehrenamt">Veranstaltungen Heute</a></li>
                            </ul>
                        </div>
                        <div class="beberlin"><a class="bb-logo"></a></div>
                    </div>
                </div>
            </div>
        </div>
        <div id="kiezatlas">
            <div id="search-controls">
                <form action="javascript:kiezatlas.search_near_by_input();" id="near-by-form" accept-charset="UTF-8">
                    <label for="near-by" id="near-by-label">In der N&auml;he von</label>
                    <input id="near-by" type="text" placeholder="Stra&szlig;enname / Hnr."></input>
                    <input id="do-nearby" type="submit" class="btn" value="Ok"></input>
                </form>
                <div id="street-alternatives"></div>
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
