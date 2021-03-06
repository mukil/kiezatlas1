<!DOCTYPE html>
<html>
<head>
    <title>kiezatlas.de Mobil 1.0</title>

    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no, maximum-scale=1.0"/>
    <meta name="apple-mobile-web-app-capable" content="no" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black" />
    <meta property="og:type" content="website"/>
    <meta property="og:title" content="Berliner KiezAtlas Mobil"/>
    <meta property="og:image" content="http://m.kiezatlas.de/"/>
    <meta property="og:url" content="http://m.kiezatlas.de/"/>
    <meta property="og:site_name" content="kiezatlas.de Mobil 1.0"/>
    <meta property="og:description" content="Diese kostenlose App f&uuml;r Ihr Smartphone liefert Ihnen alle 
        Stadtpl&auml;ne aus dem Berliner KiezAtlas auf Ihr mobiles Endger&auml;t."/>

    <link rel="stylesheet" href="/pages/vendor/jquerymobile/jquery.mobile.theme-1.3.0.min.css">
    <link rel="stylesheet" href="/pages/vendor/jquerymobile/jquery.mobile-1.3.0.min.css">
    <link rel="stylesheet" href="/pages/vendor/leaflet/dist/leaflet.css"/>
    <link rel="stylesheet" href="/pages/mobile/css/mobile-sitestyle.css"/>
    <script src="/pages/mobile/ka-mobile-SNAPSHOT.js"></script>
    <script src="/pages/vendor/jquery-1.9.1.min.js"></script>
    <script src="/pages/vendor/leaflet/dist/leaflet.js"></script>
    <script src="/pages/vendor/jquerymobile/jquery.mobile-1.3.0.min.js"></script>
    <script type="text/javascript">
        $(document).ready(function(e) {

            $.mobile.transitionFallbacks.slideout = "none"

            /** This is basically our primitive routing implementation of the webapp **/
            $(document).on("pagechange", function(event, data) {
                var toPage = ""
                var fromPage = ""
                toPage = data.toPage[0].id
                if (data.options.fromPage != undefined) {
                    fromPage = data.options.fromPage[0].id
                }
                if (toPage.indexOf("citymaps-page") != -1) {

                    kiezatlas.setServiceUrl("http://www.kiezatlas.de/rpc/");
                    kiezatlas.setIconsFolder("http://www.kiezatlas.de/client/icons/");
                    kiezatlas.setImagesFolder("http://www.kiezatlas.de/client/images/");
                    if (fromPage.indexOf("info") != -1) {
                        // do nothing, in particular do not initialize our citymap again
                        kiezatlas.renderLeafletContainer(false)
                    } else if (fromPage.indexOf("citymaps-page") == -1) {
                        kiezatlas.renderMobileCityMapView()
                    }

                } else if (toPage.indexOf("citymaps-list") != -1) {

                    if (kiezatlas.markers != undefined) kiezatlas.clearMarkers() // clean up on the way back(tothelist)
                    kiezatlas.setMobileViewTitle("kiezatlas.de Mobil");
                    if (kiezatlas.publishedMaps == undefined || kiezatlas.publishedMaps.length == 0) {
                        kiezatlas.renderMobileCityMapList(kiezatlas.publishedMaps)
                    } else {
                        kiezatlas.renderMobileCityMapList(kiezatlas.publishedMaps)
                    }

                }
            })

            /** Handling application initialization by deep-linking **/
            if (window.location.href.indexOf("#citymaps-page") == -1 && 
                window.location.href.indexOf("#infoo") == -1) {
                // this is just here because initially, no "pagechange" is fired by jqm
                // kiezatlas.loadPublishedMobileCityMaps()
                kiezatlas.renderMobileCityMapList(kiezatlas.publishedMaps)
            } else {
                kiezatlas.cityMapId = kiezatlas.getURLParameter("mapId")
                kiezatlas.workspaceId = kiezatlas.getURLParameter("wsId")
                // set URL params, "pagechange" event will do the rest (init citymaps-page view)
            }

        });
    </script>
</head>
<body>

    <div id="citymaps-list" data-role="page" data-filter-placeholder="Suche ...">
        <div data-role="header" data-theme="c">
              <h1 class="my-title">kiezatlas.de Mobil</h1>
              <!-- a href="#" data-icon="bars">&nbsp;</a-->
        </div>
        <div data-role="content" data-theme="c">
            <!-- data-inset="true" -->
            <ul data-role="listview" data-theme="c" class="citymap-listing" data-filter="true">
                <li data-role="list-divider" data-theme="e" id="all-citymaps">Lade Stadtpl&auml;ne...</li>
            </ul>
        </div>
    </div>

    <div id="citymaps-page" data-role="page">

        <div data-role="header" data-theme="c">
            <!--a target="_blank" href="http://m.kiezatlas.de/ehrenamt" data-icon="home">&nbsp;</a-->
            <!-- a class="back" href="#citymaps-list" data-transition="flow" data-icon="back">&nbsp;</a-->
            <h1 class="my-title">Lade Stadtplanansicht..</h1>
            <!-- a href="#" data-icon="bars">&nbsp;</a-->
            <a class="back" style="display: none;" href="#infoo" data-transition="slidefade"></a>
        </div>

        <div id="message" class="notification">Testnachricht..</div>

        <div id="map" class="fullsize"></div>

        <!--div id="details" data-role="panel" data-position="right">
            <a href="#my-header" data-rel="close">Close panel</a>
        </div-->

    </div>

    <div id="infoo" data-role="page">

        <div data-role="header" data-theme="c">
            <!--a target="_blank" href="http://m.kiezatlas.de/ehrenamt" data-icon="home">&nbsp;</a-->
            <!-- a class="back" href="#citymaps-page" data-transition="slidefade" data-icon="back">&nbsp;</a-->
            <h1 class="my-title">Detailansicht</h1>
            <!-- a href="#" data-icon="bars">&nbsp;</a-->
        </div>

        <div id="infoo-area" data-role="content">
            <p>
                Bitte gehen Sie &uuml;ber die <a href="#citymaps-list">Stadtplanansichten</a> zu den jew. Detailinfos. 
                Unsere Anwendung unterst&uuml;tzt aktuell noch nicht das sog. "Deep-Linking" (direktes ansteuern) 
                von einzelnen Datens&auml;tzen in allen Stadtpl&auml;nen.<br/><br/>
                Bitte entschuldigen Sie die Umst&auml;nde. <br/><br/> Ihr KiezAtlas-Team
            </p>
        </div-->

    </div>

    <img src="http://stats.kiezatlas.de/piwik.php?idsite=2&amp;rec=1&action_name=kiezatlas_mobil_front" style="border:0 display: none;" alt="" />

</bod>
</html>

