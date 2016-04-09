<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ehrenamtsnetz Web App (Listing)</title>
    <link rel="stylesheet" href="/kiezatlas/pages/vendor/jquerymobile/jquery.mobile.theme-1.3.0.min.css">
    <link rel="stylesheet" href="/kiezatlas/pages/vendor/jquerymobile/jquery.mobile-1.3.0.min.css">
    <link rel="stylesheet" href="/kiezatlas/pages/mobile/ehrenamt/list/css/customizations.css">
    <script src="/kiezatlas/pages/vendor/jquery-1.9.1.min.js"></script>
    <script src="/kiezatlas/pages/vendor/jquerymobile/jquery.mobile-1.3.0.min.js"></script>
    <style type="text/css">
        h1.my-title { margin: 10px 10px !important; }
        .ehrenamts-listing .ui-li .ui-btn-text a.ui-link-inherit { white-space: normal; }
    </style>
    <script type="text/javascript">
        $(document).ready(function(e) {

            var SERVICE_URL = "/kiezatlas/rpc/"

            var cityMapEhrenamtId = "t-331302"
            var workspaceEhrenamtId = "t-331306"
            // 
            var workspaceEventId = "t-453282"
            var cityMapEventId = "t-453286"

            loadCityMapTopics(cityMapEhrenamtId, workspaceEhrenamtId, true)
            loadCityMapTopics(cityMapEventId, workspaceEventId, false)

            function loadCityMapTopics(mapId, workspaceId, isAction) {
                var url = SERVICE_URL
                var body = '{"method": "getMapTopics", "params": ["' + mapId+ '" , "' + workspaceId + '"]}'
                jQuery.ajax({
                    type: "POST", async: true, url: url, data: body,
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Content-Type", "application/json")
                    },
                    dataType: 'json',
                    success: function (obj) {
                        // results.push()
                        obj.result.topics.sort(topicSort); // alphabetical ascending
                        for (i = 0; i < obj.result.topics.length; i++) {
                            var element = obj.result.topics[i]
                            var item = $('<li id="'+ element.id +'">')
                            var link = $('<a class="'+ element.id +'" href="#">'+ element.name +'</a>')
                            item.bind('vclick', function(e) {
                                var topicId = ""+ e.target.parentElement.parentElement.parentElement.id + "";
                                loadCityObjectInfo(topicId, function(result) {
                                    var originId = getOriginId(result)
                                    // create link for item
                                    if (isAction) {
                                        var link = "http://www.berlin.de/buergeraktiv/engagieren/buerger/ehrenamtssuche/index.cfm"
                                            + "?dateiname=ehrenamt_projektbeschreibung_mobil.cfm&anwender_id=5&projekt_id="
                                            + originId + "&cfide=0.662606781956"
                                    } else {
                                        var link = "http://www.berlin.de/land/kalender/?c=15&detail=" + originId
                                    }
                                    // navigate there..
                                    window.location.href = link;
                                    // $("a.topicId").append("<p>"+ result +"</p>")
                                })
                            })
                            if (isAction) { item.append(link).insertAfter("#actions")
                            } else { item.append(link).insertAfter("#events-today") }
                            // $("ul.ehrenamts-listing").append(item)
                            // $("#"+ element.id).append(link)
                                // +'<a href="#" data-theme="d" data-icon="info"></a></li>')
                        }
                        $( "ul.ehrenamts-listing" ).listview( "refresh");
                    }, // end of success handler
                    error: function (x, s, e) {
                        throw new Error("Error while loading city-map. Message: " + JSON.stringify(x))
                    }
                })
            }

            function loadCityObjectInfo (topicId, renderFunction) {
                var url = SERVICE_URL;
                var body = '{"method": "getGeoObjectInfo", "params": ["' + topicId + '"]}';// '{' + urlencode(streetFocus) + '}';
                jQuery.ajax({
                    type: "POST", url: url, data: body, dataType: 'json',
                    beforeSend: function (xhr) {
                        xhr.setRequestHeader("Content-Type", "application/json")
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

            function getOriginId(topic) {
                for (i=0; i<topic.properties.length; i++) {
                    var property = topic.properties[i]
                    if (property.name == "OriginId") return property.value
                }
            }

            // compare "a" and "b" in some fashion, and return -1, 0, or 1
            function topicSort(a, b) {
                var nameA = a.name
                var nameB = b.name
                if (nameA > nameB) // sort string descending
                    return -1
                if (nameA < nameB)
                    return 1
                return 0 //default return value (no sorting)
            }
        });
    </script>
</head>
<body>

    <div data-role="page" data-filter-placeholder="Suche ...">
      <div data-role="header" data-theme="b" data-position="fixed">
          <h1 class="my-title">Ehrenamtsnetz App</h1>
          <a target="_blank" href="http://m.kiezatlas.de/ehrenamt" data-icon="arrow-l">Karte</a>
          <!-- a href="#" data-icon="bars">&nbsp;</a-->
      </div>
      <div data-role="content" data-theme="d">
          <!-- data-inset="true" -->
          <ul data-role="listview" class="ehrenamts-listing" data-filter="true">
            <li data-role="list-divider" id="events-today">Veranstaltungen Heute</li>
            <li data-role="list-divider" id="actions">Aktivit&auml;ten / Einsatzm&ouml;glichkeiten</li>
          </ul>
      </div>
    </div>

    <img src="http://stats.kiezatlas.de/piwik.php?idsite=2&amp;rec=1&action_name=ehrenamt_list" style="border:0 display: none;" alt="" />

</bod>
</html>

