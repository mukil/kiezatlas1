
// Array Remove - By John Resig (MIT Licensed)
Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

/**
 * This is some new KiezatlasJS citymap client designed to run acros all (browsers) screens.
 * @author  Malte Rei&szlig;ig (malte@mikromedia.de), Copyright (c) 2013
 * @license   GPLv3 (http://www.gnu.org/licenses/gpl-3.0.en.html)
 *
 * @requires  jQuery JavaScript Library v1.9.1, Copyright 2013, John Resig
              Dual licensed under the MIT or GPL Version 2 licenses.(http://jquery.org/license)
 * @requires  leaflet.js, Copyright (c) 2010-2012, CloudMade, Vladimir Agafonkin, All rights reserved.
 *            Used in version/with source code at https://github.com/mukil/Leaflet
 * @requires  jQueryMobile Library v1.3.1, Copyright 2013, John Resig
 *            Dual licensed under the MIT or GPL Version 2 licenses.(http://jquery.org/license)
 *
 * Implementation Notes:
 * - load*-Methods set data (and even return data if a handler is given)
 * - render* or show*-Methods depend on jquery, jquerymobile and a specific DOM/Layout/IDs
 * - get/set* Methods operate on the kiezatlas-object itself (and use its reference as a client-side model)
 *
 * @modified  30 August 2014
 */

var kiezatlas = new function() {
    //
    this.base_url = ""
    this.map_web_alias = ""
    this.webservice_endpoint = ""
    // 
    this.map_topics = undefined
    this.selected_topic = undefined
    // 
    this.defaultMapCenter = undefined
    this.locationCircle = undefined // L.Circle () if already set once..
    this.mapLayer = undefined
    this.map = undefined // L.Map();
    //
    this.markers = undefined // array of L.Marker Objects
    this.selected_criteria = 0
    this.marker_group_ids = []
    this.alternative_items = [] // near-by search street-alternatives
    this.autocomplete_item = 0
    //
    this.LEVEL_OF_DETAIL_ZOOM = 15 // the map focus when a mapinternal infoWindow is rendered
    this.LEVEL_OF_STREET_ZOOM = 14 // the map focus when a mapinternal infoWindow is rendered
    this.LEVEL_OF_KIEZ_ZOOM = 13
    this.LEVEL_OF_DISTRICT_ZOOM = 12
    this.LEVEL_OF_CITY_ZOOM = 11
    //
    this.layer = undefined
    this.location_circle = undefined
    //
    this.historyApiSupported = window.history.pushState
    this.panorama = undefined // currently unused: gui helper flag
    this.isMobile = false
    this.pageVisible = false
    this.printView = false
    this.offerMobileWebApp = false

    var IMAGES_URL = "http://www.kiezatlas.de/client/images/"
    var ICONS_URL = "http://www.kiezatlas.de/client/icons/"


    // --
    // --- Leaflet (Map) and DOM-Setup related methods
    // --

    /** initializes an interactive citymap view
     *  fixme: fails if there is just 1 element in the geomap result */
    this.render_mobile_city_map_view = function () {
        kiezatlas.check_for_small_screen()
        // check if, and if not, initialize leaflet
        if (typeof kiezatlas.map === "undefined") {
            kiezatlas.set_map(new L.map('map'), { "trackResize": true, "zoomAnimation": false })
        }
        // update model
        // kiezatlas.loadCityObjectInfo(kiezatlas.get_map_topic_id())
        if (typeof kiezatlas.get_map_topics().topics === "undefined") {
            console.warn("Maps Topics must be set first. use kiezatlas.set_map_topics()")
        }
        kiezatlas.setup_leaflet_markers()
        kiezatlas.render_leaflet_container(true)
        // ask for users location
        // kiezatlas.ask_users_location()
    }

    /** called by deep init link and user-interaction */
    this.show_page_panel = function () {
        // 
        var page_width = $("#kiezatlas").width() / 3
        kiezatlas.set_page_panel_width(page_width)
        // 
        $("#overview").width($("#kiezatlas").width() - page_width)
        $("#details").show()
        kiezatlas.pageVisible = true
        //
        kiezatlas.render_leaflet_container()
    }

    this.hide_page_panel = function () {
        // 
        $("#details").hide()
        $("#overview").width($("#kiezatlas").width())
        kiezatlas.pageVisible = false
        // 
        kiezatlas.render_leaflet_container()
    }

    this.set_page_panel_width = function (page_width) {
        $("#details").width(page_width)
        $("#page-body").width(page_width - 5) // minus padding
        $("#print-sign").width(page_width)
        $("#footer-area").width(page_width)
    }

    this.set_leaflet_container_width = function (page_width) {
        if (kiezatlas.pageVisible) {
            $("#map").width($("#kiezatlas").width() - (page_width + 11))
        } else {
            $("#map").width($("#kiezatlas").width())
        }
    }

    /** called on init and resize */
    this.render_leaflet_container = function (reset) {

        if (!kiezatlas.printView) {
            // update gui
            $("#map").height($("#kiezatlas").height() - 19)
            $("#details").height($("#kiezatlas").height() - 19) // fixme: move to another method
            $("#page-body").height(
                $("#details").height() - $('#criteria-list').height() - 30
            )
            kiezatlas.map.invalidateSize()
            // 
            var page_width = $("#kiezatlas").width() / 3
            kiezatlas.set_leaflet_container_width(page_width)
            if (kiezatlas.pageVisible) kiezatlas.set_page_panel_width(page_width)
            // 
            if (reset) {
                kiezatlas.set_leaflet_map_to_current_bounds()
                kiezatlas.load_map_tiles()
                kiezatlas.map.setView(kiezatlas.get_current_bounds().getCenter(), kiezatlas.LEVEL_OF_CITY_ZOOM)
            }
        }

    }

    /**
     this.switch_mobile_city_map = function (mapId) {

        // kiezatlas.workspaceId = workspaceId;
        if (kiezatlas.markers != undefined) {
            kiezatlas.clear_markers();
        }

        jQuery("img.loading").css("display", "block");
        kiezatlas.load_geomap_objects(mapId, function () {
            // kiezatlas.setup_leaflet_markers();
            // kiezatlas.set_leaflet_map_to_current_bounds();
            // kiezatlas.ask_users_location();
        });

        // ### FIXMEs mvoe GUI related manipulations into guiSetup/renderFunctions
        kiezatlas.closeInfoContainer(); // close info and  show nav
        // initiate current citymap state
        // var newLink = "/?map=" + mapId;
        // kiezatlas.push_history({ "name": "loaded", "parameter": [ mapId, workspaceId ] }, newLink);
        // kiezatlas.hideKiezatlasControl();
    } **/

    this.load_map_tiles = function() {
        /** var cloudmadeUrl = 'http://{s}.tiles.mapbox.com/v3/kiezatlas.map-feifsq6f/{z}/{x}/{y}.png',
            cloudmadeAttribution = "Tiles &copy; <a href='http://mapbox.com/'>MapBox</a> | " +
              "Data &copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a> and contributors, CC-BY-SA",
            cloudmade = new L.tileLayer(cloudmadeUrl, {maxZoom: 18, attribution: cloudmadeAttribution}) 
        kiezatlas.map.addLayer(cloudmade)
        **/
        // osm: "http://b.tile.openstreetmap.de/tiles/osmde/"
        //        attribution: 'Tile server sponsored by STRATO / <b>Europe only</b> /
        //  <a href="http://www.openstreetmap.de/germanstyle.html">About style</a>',
        // TODO: render nice info message "Map tiles are loding ..."
        /** cloudmade.on('load', function(e) {
          // is just fired when panning the first time out of our viewport, but strangely not on initiali tile-loading
          console.log("tilelayer loaded.. could invaldate #maps size..")
        }); **/
        var osmLayer = new L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(kiezatlas.map)
        
    }

    this.on_bubble_click = function (e) {
        var topicId = e.id;
        // load geoobject container
        kiezatlas.do_info(topicId);
    }

    this.do_info = function(id) {
        kiezatlas.show_page_panel()
        // fetch the data, and render it
        kiezatlas.load_old_object_info(id, kiezatlas.render_page) // calls "set_selected_topic" in case of success
        // kiezatlas.push_history({"data": id}, "/atlas-i9/ehrenamt/p/" + id) // fixme: ajax-history management
    }

    this.setup_leaflet_markers = function() {
        /** currently not in use
        var KiezAtlasIcon = L.Icon.extend({
          options: {
            iconUrl: 'css/locationPointer.png',
            shadowUrl: null, iconSize: new L.Point(40, 40), shadowSize: null,
            iconAnchor: new L.Point(20, 14), popupAnchor: new L.Point(0, 4)
          }
        });
        var myIcon = new KiezAtlasIcon(); **/
        //
        if (kiezatlas.markers != undefined) {
            kiezatlas.clear_markers()
        }
        kiezatlas.markers = new Array() // helper to keep the refs to all markes once added..
        //
        for (var i = 0; i < kiezatlas.get_map_topics().topics.length; i++) {
            var full_topic = kiezatlas.get_map_topics().topics[i]

            var topicId = full_topic.id
            var marker = undefined
            var latlng = undefined
            if (typeof full_topic !== "undefined") {
                // debug: console.log(topicId + " = " + full_topic.name)
                var lng = full_topic.long
                var lat = full_topic.lat
                // sanity check..
                var skip = false
                if (lat == 0.0 || lng == 0.0) {
                    skip = true
                } else if (lng < -180.0 || lng > 180.0) {
                    skip = true
                } else if (lat < -90.0 || lat > 90.0) {
                    skip = true
                } else if (isNaN(lat) || isNaN(lng)) {
                    skip = true
                }
                if (!skip) {
                    latlng = new L.latLng(parseFloat(lat), parseFloat(lng))
                }
                // 0) Valid address topic
                if (latlng != undefined) {
                    // 1) Check if marker with this coordinates already exitss
                    var existingMarker = kiezatlas.get_marker_by_lat_lng(latlng)

                    if (existingMarker != null) {
                        // 2) If so; add our current, push our proprietary topicId to the marker-object
                        marker = existingMarker
                        marker.options.topics.push(topicId)
                    } else {
                        marker = new L.marker(latlng, {
                            'clickable': true ,  'topics': [topicId], 'labels': []
                        })
                    }
                    // reference each marker in kiezatlas.markers model
                    kiezatlas.markers.push(marker)
                }
            }
        }
        // console.log("map.setup => " + kiezatlas.markers.length + " leaflets for "
           // + kiezatlas.map_topics.topics.length + " loaded topics");
        if (kiezatlas.isMobile) $.mobile.loader("hide")
    }

    this.create_marker_popup_content = function (marker) {
        marker.bindPopup(kiezatlas.render_in_map_title(marker), {
            autoPan: true,
            maxWidth: 320,
            closeButton: true
        })
        // fixme: use leaflet 7 for fance gui-updates console.log(marker.getPopup().update())
    }

    this.render_in_map_title = function (marker) {
        var content = ""
        if (marker.options.labels.length > 1) {
            content += "<h5>An diesem Ort gibt es " +marker.options.labels.length+" M&ouml;glichkeiten:</h5>"
        }
        for (var k=0; k < marker.options.labels.length; k++) {
            var data = marker.options.labels[k].split("\t")
            var label = data[0]
            var topic_id = data[1]
            content += '<div id="' + topic_id+ '" onclick="kiezatlas.on_bubble_click(this)" class="topic-name item">'
            + '<b>' + label + '&nbsp;&rsaquo;&rsaquo;&rsaquo;</b></div>'
        }
        return content;
    }

    this.select_and_show_in_map = function(someId, isTopicId) {
        var topicId = someId
        if (!isTopicId) {
            topicId = kiezatlas.get_citymap_topic_by_origin_id(someId).id
        }
        // 
        kiezatlas.load_old_object_info(topicId, function(e) {
            kiezatlas.render_page()
            kiezatlas.focus_selected_topic_in_map()
        })
    }

    /** Precondition: topic must be set as selected */
    this.focus_selected_topic_in_map = function () {
        // 
        var topic = kiezatlas.get_selected_topic().result
        // 1) show topic in map 
        var marker = kiezatlas.add_topic_to_map(topic.name, topic.id)
        if (typeof marker === "undefined") {
            // zoom to it and pop up its name
            console.warn("Topic is not in any marker")
        } else {
            marker.openPopup()
            kiezatlas.map.setView(marker.getLatLng(), kiezatlas.LEVEL_OF_KIEZ_ZOOM);

        }
    }

    /** Precondition: topic must be set as selected */
    this.close_popup_of_selected_topic = function () {
        // 
        var topic = kiezatlas.get_selected_topic().result
        // 1) show topic in map 
        var marker = kiezatlas.get_marker_by_topic_id(topic.id)
        if (typeof marker === "undefined") {
            // zoom to it and pop up its name
            console.warn("Topic is not in any marker")
        } else {
            marker.closePopup()
        }
    }

    this.show_topics_in_map = function (items_to_show, fit_bounds) {
        // 1) add topics to map
        for (var i=0; i < items_to_show.length; i++) {
            var topic = items_to_show[i]
            kiezatlas.add_topic_to_map(topic.name, topic.id)
        }
        // 2) update viewport
        if (fit_bounds) kiezatlas.map.fitBounds(kiezatlas.get_current_bounds())
    }

    this.hide_topics_in_map = function (items_to_hide, skip_state) {
        // 
        for (var i=0; i < items_to_hide.length; i++) {
            var topic = items_to_hide[i]
            kiezatlas.remove_topic_from_map(topic.id)
        }
    }

    this.show_all_topics_in_map = function (fit_bounds) {
        // todo: just use with select_all_categories_of_current_criteria()
        kiezatlas.show_topics_in_map(kiezatlas.get_map_topics().topics, fit_bounds)
    }

    this.hide_all_topics_in_map = function () {
        kiezatlas.hide_topics_in_map(kiezatlas.get_map_topics().topics)
    }

    this.remove_topic_from_map = function (topic_id) {
        // note: actually we just remove the topic-label from the html-content-area of our marker
        var marker = kiezatlas.get_marker_by_topic_label_id(topic_id)
        if (typeof marker !== "undefined") {
            // 1) over-write label so topic is not show anymore as part of marker
            marker.closePopup()
            // 2) so we remove actually the label of the marker content (fixme: iterating again^^)
            for (var z=0; z < marker.options.labels.length; z++) {
                var marker_topic_label = marker.options.labels[z]
                if (topic_id === marker_topic_label.split("\t")[1]) {
                    marker.options.labels.remove(z) // maybe not use resigns impl
                }
            }
            // 3) re-render labels (=content shown in marker bubble)
            kiezatlas.create_marker_popup_content(marker)
            // 4) if no (topic) label is present anymore, we remove the marker completely
            if (marker.options.labels.length == 0) {
                kiezatlas.map.removeLayer(marker)
            }
        }/** else {
            // this happens if topics are in two categories and one gets de-selected first
            console.log("Topic is currently not show in any marker of this map!")
        } **/
    }

    this.add_topic_to_map = function (label, topic_id) {
        // note: actually we just add the topic-label to the html-content-area of our marker
        var marker = kiezatlas.get_marker_by_topic_id(topic_id)
        if (typeof marker !== "undefined") {
            // 0) check if topic was not previously added to marker, this happens when a topic is in two categories
            var already_present = false
            for (var z=0; z < marker.options.labels.length; z++) {
                var marker_topic_label = marker.options.labels[z]
                if (topic_id === marker_topic_label.split("\t")[1]) {
                    already_present = true
                }
            }
            // 1) so we add the label of our topic to the markers content (fixme: iterating again^^)
            if (!already_present) marker.options.labels.push(label + "\t" + topic_id)
            // 2) re-render labels (=content shown in marker bubble)
            kiezatlas.create_marker_popup_content(marker)
            // 3) if no marker was present on map, we add it to that
            if (!kiezatlas.map.hasLayer(marker)) {
                kiezatlas.map.addLayer(marker)
            }
            return marker
        } else {
            console.warn("Topic is not part of any marker in this map!")
        }
    }

    this.get_marker_by_topic_label_id = function (topic_id) {
        for (var i=0; i < kiezatlas.markers.length; i++) {
            var marker = kiezatlas.markers[i]
            for (var z=0; z < marker.options.labels.length; z++) {
                var marker_topic_label = marker.options.labels[z]
                if (topic_id === marker_topic_label.split("\t")[1]) {
                    return marker
                }
            }
        }
        return undefined
    }

    this.get_marker_by_topic_id = function (topic_id) {
        // 
        for (var i=0; i < kiezatlas.markers.length; i++) {
            var marker = kiezatlas.markers[i]
            for (var m=0; m < marker.options.topics.length; m++) {
                var marker_topic_id = marker.options.topics[m]
                if (topic_id === marker_topic_id) {
                    return marker
                }
            }
        }
        console.warn("Did not find topic_id in any marker")
        return undefined
    }

    this.get_citymap_topic_by_id = function (topic_id) {
        // 
        for (var i=0; i < kiezatlas.get_map_topics().topics.length; i++) {
            var object = kiezatlas.get_map_topics().topics[i]
            if (topic_id === object.id) {
                return object
            }
        }
        return undefined
    }

    this.get_citymap_topic_by_origin_id = function (topic_id) {
        // 
        for (var i=0; i < kiezatlas.get_map_topics().topics.length; i++) {
            var object = kiezatlas.get_map_topics().topics[i]
            if (topic_id === object.originId) {
                return object
            }
        }
        return undefined
    }

    this.get_origin_id_by_topic_id = function (topic_id) {
        // 
        for (var i=0; i < kiezatlas.get_map_topics().topics.length; i++) {
            var object = kiezatlas.get_map_topics().topics[i]
            if (topic_id === object.id) {
                return object.originId
            }
        }
        return undefined
    }



    // --
    // --- Kiezatlas Layout HTML Renderer Methods
    // --

    this.render_page = function (is_popped, html_message, search_response) {

        kiezatlas.set_selected_criteria(crtCritIndex) // relies on global var crtCritIndex

        kiezatlas.show_page_panel()
        kiezatlas.render_page_header()
        kiezatlas.setup_additional_map_handlers()

        var topic = kiezatlas.get_selected_topic()

        if (typeof topic !== "undefined") {

            kiezatlas.render_page_body(topic.result)
            if (!is_popped) kiezatlas.push_selected_detail_state() // render_page is also called by pop_history
            kiezatlas.show_page_print_button()

        } else if (typeof search_response !== "undefined") {

            kiezatlas.render_search_results_in_page(search_response.search_value, search_response.data)
            kiezatlas.hide_page_print_button()

        } else if (typeof html_message !== "undefined") {

            $('#page-body').html('<h3 class="info-label">Hinweis</h3><p>'+ html_message +'</p>')
            kiezatlas.hide_page_print_button()

        } else {

            kiezatlas.render_category_list()
            kiezatlas.show_all_topics_in_map(true)
            kiezatlas.select_all_categories_of_current_criteria()
            kiezatlas.hide_page_print_button()

            if (onEventMap) {
                kiezatlas.render_veranstaltungs_headline()
            }
            // kiezatlas.hide_topics_in_map(kiezatlas.get_map_topics().topics)

        }
    }

    this.setup_additional_map_handlers = function () {
        // 
        $('#show-all').unbind('click')
        $('#reset').unbind('click')
        // 
        $('#show-all').click(function(e) {
            kiezatlas.show_all_topics_in_map(true)
            kiezatlas.render_category_list()
            kiezatlas.select_all_categories_of_current_criteria()
        })
        $('#reset').click(function(e) {
            kiezatlas.deselect_all_categories_of_current_criteria()
            kiezatlas.hide_all_topics_in_map()
            kiezatlas.render_leaflet_container(true)
        })
    }

    this.render_veranstaltungs_headline = function () {
        $('<h3 class="event-label">Insgesamt ' +kiezatlas.get_map_topics().topics.length+ ' Veranstaltungen Heute</h3>')
            .insertBefore('#categories-table')
    }

    this.render_page_header = function () {
        /** console.log("")
        console.log("Debug City Map Meta Info:")
        console.log("City Map Name: " + mapTitle)
        console.log("City Map Topic ID: " + kiezatlas.get_map_topic_id())
        console.log("Workspace Homepage: " + workspaceHomepage)
        console.log("Workspace Logo: " + workspaceLogo)
        console.log("Workspace Imprint: " + workspaceImprint) **/
        kiezatlas.render_criteria_list()
        if (onProjectMap || onEventMap) {
            var $branding = $('#branding-area')
                if (onProjectMap) {
                    // 
                    $('li.citymap-navi.ehrenamt-projects').addClass('selected')
                    $('li.citymap-navi.ehrenamt-events.selected').removeClass('selected')
                } else if (onEventMap) {
                    // 
                    $('li.citymap-navi.ehrenamt-projects.selected').removeClass('selected')
                    $('li.citymap-navi.ehrenamt-events').addClass('selected')
                }
                // $branding.show()
            // var $logo = $('<img alt="Kiezatlas Logo" title="Gehe zur '+mapTitle+'-Homepage" '
                // + 'src="'+ IMAGES_URL + workspaceLogo +'"/>')
                // $logo.click(function(e) { window.location.href = workspaceHomepage})
                // $branding.html($logo)
            // var $criteria = $('#criteria-list')
        }
        var $crosslinks = $('#cross-links-area')

    }

    this.render_page_body = function (result) {
        var topic_copy = jQuery.extend(true, {}, result)
        // 
        var $body = $("#page-body")
            $body.empty()
            $body.removeClass('categories')
            $body.addClass('entry')
        // var $content = $('<div>')
        // 1) Image and Name
        /** no images on ehrenamt/berlin.de
        var img = kiezatlas.get_image_source(topic_copy);
        var akteur_img = kiezatlas.get_akteur_image_source(topic_copy)
        if (typeof img !== "undefined") {
            img = IMAGES_URL + img
            $content.append('<img src="'+img+'"/><br/>')
        } **/
        $body.append('<h3>' + topic_copy.name + '</h3>')
        // 2) Address Widget
        var city_name = kiezatlas.get_topic_city(topic_copy)
        var street = kiezatlas.get_topic_address(topic_copy)
        var postal_code = kiezatlas.get_topic_postal_code(topic_copy)
        var origin_id = kiezatlas.get_topic_origin_id(topic_copy)
        var image_link = ""
        // note: imported ehrenamt map datasets have no city property
        if (kiezatlas.get_map_topic_id() ==  "t-331302" || onEventMap) city_name = "Berlin"
        // 2.1) assemble berlin fahrinfo link
        image_link = create_berlin_fahrinfo_link(street, city_name, postal_code);
        // 2.2) Render cityname
        $body.append(''+ postal_code + ' ' + city_name + '<br/>')
        $body.append('' + street + '&nbsp;<br/>' + image_link)
        // 3) Cleanup data-container of unwanted attributes before rendering
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "LAT")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "LONG")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Locked Geometry")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Forum / Aktivierung")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Image")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Icon")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "YADE")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Stadt")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Address")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Name")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Description")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Timestamp")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "OriginId")
        topic_copy = kiezatlas.strip_fields_containing(topic_copy, "Akteur Logo")
        // 4) Render all remaining fields
        var $properties = $('<p class="info-area">')
        for (var i=0; i < topic_copy.properties.length; i++) {
            // 5) Render property label
            var $property_label = $('<div class="property-label">'+ topic_copy.properties[i].label +'</div>')
            // 6) Render property value of type SIMPLE
            if (topic_copy.properties[i].type == 0) {
                // ordinary rendering for DM Property Type Single Value
                var formattedValue = topic_copy.properties[i].value + ""
                if (formattedValue.indexOf("\r") != -1 || formattedValue.indexOf("\n") != -1) {
                    formattedValue = replaceLF(formattedValue)
                }
                if (formattedValue !== "" || formattedValue !== " ") {
                    $properties.append($property_label)
                    $properties.append('<div class="property-field">'+ formattedValue +'</div>')
                }
            // 7) Render property value of type MULTI
            } else {
                // $properties.append('<span class="property-field">')
                var $fields = $('<div class="property-fields">')
                for ( var k=0; k < topic_copy.properties[i].values.length; k++ ) {
                    stringValue = topic_copy.properties[i].values[k].name
                    var htmlValue = ""
                    if (stringValue.indexOf("http://") == 0) {
                        htmlValue = make_ehrenamt_webpage_link(stringValue, origin_id)
                    } else if (stringValue.indexOf("@") != -1) {
                        htmlValue = make_email_link(stringValue, stringValue)
                    } else {
                        htmlValue = stringValue
                    }
                    $properties.append($property_label)
                    $fields.append('<img style="border-style: none; vertical-align: middle;" '
                        + ' src="'+ ICONS_URL +''+ topic_copy.properties[i].values[k].icon +'"/>&nbsp;'
                        + htmlValue + '<br/>')
                }
                $properties.append($fields)
            }
        }
        $body.append($properties)
        // ..) Append page-info area to DOM        
        // $body.html($content)



        // --- Inner utility functions

        function replaceLF(value) {
            value = value.replace("\r\n", "<br/>")
            value = value.replace("\r", "<br/>")
            value = value.replace("\n", "<br/>")
            return value
        }

        function make_ehrenamt_webpage_link (url, topic_origin_id) {
	  var url = 'http://www.berlin.de/buergeraktiv/engagieren/buerger/ehrenamtssuche/index.cfm?'
		+ 'dateiname=ehrenamt_projektbeschreibung.cfm&anwender_id=5&projekt_id=' + topic_origin_id + '&cfide=0.662606781956'
          return '<a href="'+url+'" target="_blank">Link zur T&auml;tigkeitsbeschreibung'
                    + '<img src="//berlin.de/.img/ml/link_extern.gif" class="c7" alt="(externer Link)" border="0" height="11" width="12"/></a>';
        }

        function make_email_link (url, label) {
            return '<a href="mailto:'+url+'" target="_blank">'+label+'</a>';
        }

        function create_berlin_fahrinfo_link (street, city_name, code) {
            var target = street + "%20" + code + "%20" + city_name
            var url = "http://www.fahrinfo-berlin.de/fahrinfo/bin/query.exe/d?Z=" + target 
                + "&REQ0JourneyStopsZA1=2&start=1"
            var link = '<a href="'+ url + '" target="_blank" class="fahrinfo-link">'
                + '<img src=\"'+IMAGES_URL+'fahrinfo.gif" title="Der Fahrinfo-Link liefert Fahrzeiten in '
                + ' Zusammenarbeit mit www.fahrinfo-berlin.de" border="0" hspace="20"/></a>'
            return link
        }
    }

    this.render_criteria_list = function (push_state) {

        var $listing = $("#criteria-list")
            // onEventMap = (mapTitle.indexOf("Veranstaltungen Ehrenamt") != -1) ? true : false;
            // onProjectMap = (mapTitle.indexOf("Ehrenamt Berlin") != -1) ? true : false;
            /** var tabsHtml = "";
            if (onBerlinDe && onEventMap) {
              tabsHtml = '<div id="navigation-helper" '
                  + 'style="border-bottom: 1px dashed #e8e8e8; padding-left: 7px; padding-bottom: 8px; padding-top:3px; padding-right: 4px;">'
                  + '<a href="'+ baseUrl +'ehrenamt" title="Zum Einsatzstadtplan wechseln">Einsatzorte</a>&nbsp;|&nbsp;'
                  + 'Veranstaltungen Heute</div>';
            } else if (onBerlinDe && onProjectMap) {
              tabsHtml = '<div id="navigation-helper" '
                  + 'style="border-bottom: 1px dashed #e8e8e8; padding-left: 7px; padding-bottom: 8px; padding-top:3px; padding-right: 4px;">'
                  + 'Einsatzorte&nbsp;|&nbsp;'
                  + '<a href="'+ baseUrl +'veranstaltungen-ehrenamt" title="Zum Veranstaltungsstadtplan wechseln">Veranstaltungen Heute</a></div>';
            } **/
        var table = ''
            /** if (onBerlinDe && (onEventMap || onProjectMap)) {
              critLinkList += tabsHtml; // render special tab selection for inner ehrenamtsnetz navigation
            } **/
        table += '<table width="95%" id="criterias-table" cellpadding="0" border="0"><tbody>'
        // 
        table += '<tr valign="top">'; // TODO: onclick
        table += '<td rowspan="'+ (workspaceCriterias.result.length + 1) +'" align="left">'
        // rebuild upper part of the sideBar stub
        table += '<a id="homepage-link" href="'+ workspaceHomepage +'"><img alt="Kiezatlas Logo" title="Gehe zur '
            + mapTitle +'-Homepage" src="'+ IMAGES_URL + workspaceLogo +'"/>'
        table += '<td></td><td></td>'
        table += '</tr>'
        // 
        for (var i = 0; i < workspaceCriterias.result.length; i++) {
            var critName = [workspaceCriterias.result[i].critName]
            if (i == 0 && workspaceCriterias.result.length == 2) {
                table += '<tr valign="center">';
            } else {
                table += '<tr valign="top">';
            }
            if (kiezatlas.get_selected_criteria() == i) {
                table += '<td onclick="javascript:kiezatlas.render_category_list(' + i + ');" class="selected-criteria">'
                    + '<img src="http://www.berlin.de/_bde/css/list_bullet.png"/>&nbsp;' + critName + '</td>'
            } else {
                table += '<td onclick="javascript:kiezatlas.render_category_list(' + i + ', false);" class="criteria">' + critName + '</td>'
            }
            table += '<td></td></tr>';
        }
        table += '</tbody>';
        table += '</table>';
        // do append the concatenated html
        $listing.html(table);
        if (push_state) kiezatlas.push_categorical_state()
    }

    this.render_category_list = function (given_index, skip_state) {
        // 1) make sure no category from old criteria remains selected
        if (kiezatlas.marker_group_ids.length > 0) {
            kiezatlas.deselect_all_categories_of_current_criteria()
        }
        // 2) set new criteria-selection
        if (typeof given_index !== "undefined") {
            kiezatlas.set_selected_criteria(given_index)
            kiezatlas.render_criteria_list(!skip_state) // true=push_state
            // 2.1) clean up all markers currently visible on map
            // fixme: (when switching between criterias)
            kiezatlas.hide_all_topics_in_map()
        }
        if (!skip_state) {
            //  // permalink: cause users start counting from 1
            // updatePermaLink(baseUrl+mapAlias+"/criteria/"+(parseInt(criteria)+1), 
                // {name: "renderCritCatListing", parameter: parseInt(criteria)} );
        }
        var $body = $("#page-body")
        var $table = $('<table width="97%" cellpadding="2" cellspacing="0" id="categories-table">')
        if (workspaceCriterias.result.length > 0) {
            for (var i = 0; i < workspaceCriterias.result[kiezatlas.get_selected_criteria()].categories.length; i++) {
                // looping over all cats of a crit
                var icon = [workspaceCriterias.result['' + kiezatlas.get_selected_criteria() + ''].categories[i].catIcon];
                var cat_label = [workspaceCriterias.result['' + kiezatlas.get_selected_criteria() + ''].categories[i].catName];
                // var catId = new String([workspaceCriterias.result['' + kiezatlas.get_selected_criteria() + ''].categories[i].catId]);
                var category_id = workspaceCriterias.result['' + kiezatlas.get_selected_criteria() + ''].categories[i].catId;
                var cat_class = "cat-deselected";
                if (kiezatlas.is_cat_visible(category_id)) {
                    cat_class = "cat-selected";
                }
                //
                var html = '<tr id="catRow-'+ category_id +'" width="100%" class="'+ cat_class +'">'
                  + ' <td width="25px" class="cell-ikon" id="toggleHref-'+ category_id +'">'
                    + '<img src="'+ ICONS_URL +''+ icon +'" border="0" id="catIconRow-'+ category_id +'" '
                    + ' alt="'+ cat_label +'-Icon" text="Klicken zum Ein- und Ausblenden"/></td>'
                  + '<td valign="center" class="cell-label" id="catHref-'+ category_id +'">'+ cat_label +'</td></tr>'
                $table.append(html);
                // fixme: hover-style
                // mozilla alows onclick on a tableRow while webkit and others do neither allow onmouseclick nor onclick
                $("#toggleHref-"+ category_id, $table).attr('onclick', 'javascript:kiezatlas.toggle_category("'+ category_id +'");')
                $("#catHref-"+ category_id, $table).attr('onclick', 'javascript:kiezatlas.render_category_topics_in_page("'+ category_id +'", "'+ cat_label +'");')
                // registering ui effects
                // $("#toggleHref-"+catId).hover(inCatButton, outCatButton);
            }
            $body.html($table)
            $body.removeClass('entry')
            $body.addClass('categories')
            kiezatlas.hide_page_print_button()
        }
    }

    this.select_all_categories_of_current_criteria = function () {
        for (var i = 0; i < workspaceCriterias.result[kiezatlas.get_selected_criteria()].categories.length; i++) {
            var category_id = workspaceCriterias.result[kiezatlas.get_selected_criteria()].categories[i].catId
            kiezatlas.marker_group_ids.push(category_id)
            $("#catRow-"+ category_id).attr("class", "cat-selected");
        }
    }

    this.deselect_all_categories_of_current_criteria = function () {
        for (var m = 0; m < kiezatlas.marker_group_ids.length; m++) {
            // = null; // delete catId from the list of currently visible categories
            $("#catRow-"+ kiezatlas.marker_group_ids[m]).attr("class", "cat-deselected")
        }
        kiezatlas.marker_group_ids = []
    }

    this.remove_cat_id_from_marker_group_list = function (category_id) {
        for (var m = 0; m < kiezatlas.marker_group_ids.length; m++) {
            if (category_id === kiezatlas.marker_group_ids[m]) {
                kiezatlas.marker_group_ids.splice(m,1)
            }
        }
    }

    this.toggle_category = function (category_id) {
        // 
        var catSelected = kiezatlas.is_cat_visible(category_id)
        //
        if (!catSelected) {
            // catId was not selected, but is now
            // add catId to our little helper list
            kiezatlas.marker_group_ids.push(category_id)
            var topics = kiezatlas.get_all_topics_in_category(category_id)
            kiezatlas.show_topics_in_map(topics)
            jQuery("#catRow-"+ category_id).attr("class", "cat-selected");
            // showTopicFeatures(topics, category, skipHistoryUpdate)
            //log('<b>MarkerGroupIds before showing: ' + markerGroupIds.toString() + ' in which are: '+topics.length+'</b>');
        } else {
            // remove catId from our little helper List
            kiezatlas.remove_cat_id_from_marker_group_list(category_id)
            var topics = kiezatlas.get_all_topics_in_category(category_id)
            kiezatlas.hide_topics_in_map(topics)
            jQuery("#catRow-"+ category_id).attr("class", "cat-deselected");
            // hideTopicFeatures(topicsToToggle, skipHistoryUpdate);
            // if (debug) log('.toggleMarkerGroups.before hiding: ' + markerGroupIds.toString() + ', '+topics.length+'</b>');
        }
        kiezatlas.push_categorical_state()
    }

    this.render_category_topics_in_page = function (cat_id, cat_label) {
        // 1) clean up selection
        kiezatlas.hide_all_topics_in_map()
        kiezatlas.deselect_all_categories_of_current_criteria()
        // 2) build up new selection
        var items_to_show = kiezatlas.get_all_topics_in_category(cat_id);
        kiezatlas.show_topics_in_map(items_to_show)
        // 3) Render list
        var $content = $("#page-body")
            $content.empty()
            $content.append('<h3 class="category-label">' + cat_label + '</h3>')
        var $results = $('<ul class="result-list list">')
        for (var i=0; i < items_to_show.length; i++) {
            var topic = items_to_show[i]
            var $item = $('<li id="'+ topic.id +'">'+ topic.name +'</li>')
                $item.click(function(e) {
                    kiezatlas.load_old_object_info(e.target.id)
                    kiezatlas.focus_selected_topic_in_map(e.target.id)
                })
            // 
            $results.append($item)
        }
        // 4) Append results to DOM
        $content.append($results)
    }

    this.render_search_results_in_page = function (value, items_to_show) {
        // 1) clean up selection
        kiezatlas.hide_all_topics_in_map()
        kiezatlas.deselect_all_categories_of_current_criteria()
        // 2) build up new selection
        kiezatlas.show_topics_in_map(items_to_show)
        // 3) Render list
        var $content = $("#page-body")
            $content.empty()
            $content.append('<h3 class="search-label">Suche nach \"' + value + '\"</h3>')
        var $results = $('<ul class="result-list list">')
        for (var i=0; i < items_to_show.length; i++) {
            var topic = items_to_show[i]
            var $item = $('<li id="'+ topic.id +'">'+ topic.name +'</li>')
                $item.click(function(e) {
                    kiezatlas.load_old_object_info(e.target.id)
                    kiezatlas.focus_selected_topic_in_map(e.target.id)
                })
            // 
            $results.append($item)
        }
        // 4) Append results to DOM
        $content.append($results)
    }

    this.hide_page_print_button = function(e) {
        $('.nav li.icon-printer_32').hide()
    }

    this.show_page_print_button = function(e) {
        $('.nav li.icon-printer_32').show()
        $('.nav li.icon-printer_32').click(function(event) {
            // 
            event.preventDefault()
            kiezatlas.printView = true
            // 
            // kiezatlas.set_page_panel_width($('#kiezatlas').width() / 2)
            // kiezatlas.set_leaflet_container_width($('#kiezatlas').width() / 3)
            $('#kiezatlas').height(700)
            $("#details").height($("#kiezatlas").height() - 20) // fixme: move to another method
            $('#details').width(350)
            $('#page-body').width(360)
            $("#page-body").height($("#details").height() - $('#criteria-list').height() - 30)
            $("#overview").width(300)
            $("#map").height(300)
            $("#map").width(300)
            // 
            $('#page-body').css("overflow-y", "visible")
            $('#page-body').css("padding-top", "10")
            $("#map").css("top", 35)
            $("#footer-area").css("top", 40)
            $("#criteria-list").hide()
            $("#search-controls").hide()
            $("#show-all").hide()
            $("#reset").hide()
            // 
            kiezatlas.focus_selected_topic_in_map()
            kiezatlas.map.setZoom(kiezatlas.LEVEL_OF_DETAIL_ZOOM)
            kiezatlas.close_popup_of_selected_topic()
            // 
            kiezatlas.map.invalidateSize()
            // 
            var $back = $('<a class="print-back" style="position: absolute; right: 0px;">')
                $back.html("Zur&uuml;ck zur Detailansicht<br/>").click(function(e) {
                    kiezatlas.printView = false
                    // fixme:
                    $("#criteria-list").show()
                    $("#search-controls").show()
                })
                $back.insertBefore("#search-controls")
        })
    }



    // --
    // --- Kiezatlas Application Specific Methods
    // --

    this.get_all_topics_in_category = function (cat_id) {
        var topics = new Array();
        var all_topics = kiezatlas.get_map_topics().topics
        for (var i = 0; i < all_topics.length; i++) {
          for (var j = 0; j < all_topics[i].criterias.length; j++) {
            for (var k = 0; k < all_topics[i].criterias[j].categories.length; k++) {
              if (cat_id == all_topics[i].criterias[j].categories[k]) {
                  topics.push(all_topics[i]);
              }
            }
          }
        }
        return topics;
    }

    this.is_cat_visible = function (cat) {
        for (var i = 0; i < kiezatlas.marker_group_ids.length; i++) {
            if (kiezatlas.marker_group_ids[i] == cat) {
                return true;
            }
        }
        return false;
    }


    //
    // --- Topic Data Container Utilities
    //

    this.strip_fields_containing = function (topic, fieldName) {
        var newProps = new Array()
        for (var it=0; it < topic.properties.length; it++) {
          // resultHandler.append('<tr><td>'+topic.properties[i].label+'</td><td>'+topic.properties[i].value+'</td></tr>');
          if (topic.properties[it].name.indexOf(fieldName) == -1) {
            // log('fieldStrippin: ' + it);
            newProps.push(topic.properties[it])
          } else if (topic.properties[it].name.indexOf("Email") != -1) {
            // save Email Address Property being stripped by a command called "Address""
            newProps.push(topic.properties[it])
          } else {
            // flog('stripping Field ' + topic.properties[it].name);
          }
        }
        topic.properties = newProps
        return topic;
    }

    this.get_topic_address = function (topic) {
        for (var i=0; i < topic.properties.length; i++) {
          if (topic.properties[i].name == "Address / Street" && topic.properties[i].value != "") {
          // via related Address Topic
          return topic.properties[i].value
          } else if (topic.properties[i].name == "Straße" && topic.properties[i].value != "") {
          // via related Street PropertyField
          return topic.properties[i].value
          }
        }
        return ""
    }

    this.get_image_source = function (topic) {
        for (var i=0; i < topic.properties.length; i++) {
          if (topic.properties[i].name == "Image / File" && topic.properties[i].value != "") {
            return topic.properties[i].value
          }
        }
        return undefined
    }

    this.get_akteur_image_source = function (topic) {
        for (var i=0; i < topic.properties.length; i++) {
          if (topic.properties[i].name == "Akteur Logo" && topic.properties[i].value != "") {
            return topic.properties[i].value
          }
        }
        return undefined
    }

    this.get_topic_postal_code = function (topic) {
        for (var i=0; i < topic.properties.length; i++) {
          if (topic.properties[i].name == "Address / Postal Code") {
            return topic.properties[i].value; // + ' Berlin<br/>'
          }
        }
        return ""
    }

    this.get_topic_origin_id = function (topic) {
        for (var i=0; i < topic.properties.length; i++) {
          if (topic.properties[i].name == "OriginId") {
            return topic.properties[i].value; // + ' Berlin<br/>'
          }
        }
        return ""
    }

    this.get_topic_city = function (topic) {
        for (var at=0; at < topic.properties.length; at++) {
          // resultHandler.append('<tr><td>'+topic.properties[i].label+'</td><td>'+topic.properties[i].value+'</td></tr>');
          if (topic.properties[at].name === "Address / City") {
            return topic.properties[at].value; // + ' Berlin<br/>'
          } else if (topic.properties[at].name === "Stadt") {
            return topic.properties[at].value;
          }
        }
        return null
    }



    // --
    // --- Custom Methods accessing our REST- Service
    // --


    this.load_old_object_info = function (topicId, render_function) {
        var url = kiezatlas.get_ajax_service_endpoint()
        var body = '{"method": "getGeoObjectInfo", "params": ["' + topicId + '"]}';
        jQuery.ajax({
            type: "POST", url: url, data: body, async: false,
            beforeSend: function(xhr) {xhr.setRequestHeader("Content-Type", "application/json")},
            dataType: 'json',
            success: function(obj) {
                kiezatlas.set_selected_topic(obj)
                if (typeof render_function !== "undefined") render_function()
            }, // end of success handler
            error: function(x, s, e){
                console.warn("Kiezatlas AJAX Request load_old_object_info failed: " + s)
            }
        });
    }

    this.load_object_info = function (topicId, render_function) {
        var url = "/core/topic/" + topicId + "?fetch_composite=true";
        var response = undefined
        jQuery.ajax({
            type: "GET", async: false,
                url: url, dataType: 'json',
                beforeSend: function(xhr) {
                xhr.setRequestHeader("Content-Type", "application/json")
            },
            success: function(obj) {
                if (typeof render_function !== 'undefined') {
                    //
                    kiezatlas.set_map_topic(obj)
                    render_function(obj)
                }
                response = obj
            },
            error: function(x, s, e) {
                throw new Error('ERROR: detailed information on this point could not be loaded. please try again.' + x)
            }
        })
        return response
    }

    /** requests and sets all geobjects of the loaded map to kiezatlas.map_topics **/
    this.load_geomap_objects = function (mapId, handler) {
        var url = "/topicmap/" + mapId; // uses topicmap-service instead of /geomap-service
        // var body = '{"method": "getMapTopics", "params": ["' + mapId+ '" , "' + workspaceId + '"]}';
        jQuery.ajax({
            type: "GET", async: false,
                // data: body,
                url: url, dataType: 'json',
                beforeSend: function(xhr) {
                xhr.setRequestHeader("Content-Type", "application/json")
            },
            success: function(obj) {
                kiezatlas.set_map_topics(obj);
                if (handler != undefined) handler();
            },
            error: function(x, s, e) {
                throw new Error("Error while loading city-map. Message: " + JSON.stringify(x));
            }
        });
    }


    this.search_near_by_input = function () {
        var streetFocus = jQuery("#near-by").val() + ' Berlin'
            streetFocus = encodeURIComponent(streetFocus, "UTF-8")
        var locale = "de"; // default set to de if empty by proxy-servlet
        var body = '{"method": "geoCode", "params": ["'+ streetFocus +'", "...", "'+locale+'"]}'
        jQuery.ajax({
            type: "POST", url: kiezatlas.get_ajax_service_endpoint(),
            data: body, dataType: 'json',
            beforeSend: function(xhr) {
                xhr.setRequestHeader("Content-Type", "application/json")
                xhr.setRequestHeader("Charset", "UTF-8")
            },
            success: function(obj) {
                kiezatlas.alternative_items = null
                kiezatlas.alternative_items = obj.results
                kiezatlas.autocomplete_item = 0
                
                // Show all topics in map
                kiezatlas.show_all_topics_in_map(false)
                kiezatlas.select_all_categories_of_current_criteria()
                
                if (kiezatlas.alternative_items.length == 1) {
                    // select_current_item()
                    focus_current_item()
                    render_alternatives()
                    if (kiezatlas.alternative_items.length <= 1) {
                        $('#street-alternatives').hide()
                    }
                } else if (kiezatlas.alternative_items.length > 1) {
                    focus_current_item() // focus the first result
                    render_alternatives()
                }
            },
            error: function(x, s, e) {
                console.warn("ERROR", "x: " + x + " s: " + s + " e: " + e)
            }
        })

        function focus_current_item () {
            var item = kiezatlas.alternative_items[kiezatlas.autocomplete_item]
            if (typeof item !== "undefined") {
                kiezatlas.map.setView(new L.latLng(item.geometry.location.lat, item.geometry.location.lng), 
                    kiezatlas.LEVEL_OF_KIEZ_ZOOM)
                // update gui (search-input field) to contents of first result entry
                $("#near-by").val(item['formatted_address'])
                // Display location
                if (kiezatlas.location_circle != undefined) {
                    kiezatlas.map.removeLayer(kiezatlas.location_circle);
                }
                kiezatlas.location_circle = new L.circle(item.geometry.location, 200, {"stroke": true,
                    "clickable": false, "color": "#dae3f8", "fillOpacity": 0.6, "opacity": 0.8, "weight":10});
                kiezatlas.map.addLayer(kiezatlas.location_circle, {"clickable" : false});
            }
        }

        function render_alternatives () {
            // 
            var prev_location = kiezatlas.alternative_items[kiezatlas.autocomplete_item - 1]
            var next_location = kiezatlas.alternative_items[kiezatlas.autocomplete_item + 1]
            var $prev = ""
            var $next = ""
            if (typeof prev_location !== "undefined") {
                $prev = $('<span class="prev-location btn" title="'+ prev_location['formatted_address'] +'"><</span>')
                $prev.click(function(e) {
                    kiezatlas.autocomplete_item = kiezatlas.autocomplete_item - 1
                    focus_current_item()
                    render_alternatives()
                })
            } else {
                // empty prev button
                $prev = $('<span class="prev-location" title=""><</span>')
            }

            if (typeof next_location !== "undefined") {
                $next = $('<span class="next-location btn" title="'+ next_location['formatted_address'] +'">></span>')
                $next.click(function(e) {
                    kiezatlas.autocomplete_item = kiezatlas.autocomplete_item + 1
                    focus_current_item()
                    render_alternatives()
                })
            } else {
                // empty next button
                $next = $('<span class="next-location" title="">></span>')
            }
            // 
            $('#street-alternatives').html($prev).append($next)
                .append(kiezatlas.alternative_items.length + ' Standorte gefunden')
            $('#street-alternatives').show()
        }

        // if (!checkIfAllCategoriesSelected()) showAllMarker();
    }

    this.do_search = function (is_popped) {
        // 1) Get search term
        var value = jQuery("#simple-search").val()
        // 2) Push search state (if not popped)
        if (!is_popped) kiezatlas.push_search__state(value)
        // 3) Perform search request
        var body = '{"method": "searchGeoObjects", "params": ["'+ value +'", "'+ kiezatlas.get_map_topic_id() +'", "'+ workspaceId +'"]}'
        jQuery.ajax({
            type: "POST", url: kiezatlas.get_ajax_service_endpoint(),
            data: body, dataType: 'json', async: true,
            beforeSend: function(xhr) {
                xhr.setRequestHeader("Content-Type", "application/json")
                xhr.setRequestHeader("Charset", "UTF-8")
            },
            success: function(obj) {
                // 1) Invalidate current topic selection for page_renderer
                kiezatlas.set_selected_topic(undefined)
                // 2) Build up new response object for page_renderer
                var search_response = { "data": obj.result, "search_value": value }
                kiezatlas.render_page(false, undefined, search_response)
                if (obj.result.length == 0) {
                    kiezatlas.render_page(false, "Die Suche nach \""+ value +"\" f&uuml;hrte zu keinem Treffer.")
                }
            },
            error: function(x, s, e) {
                // No Results / Keine Ergebnisse
                kiezatlas.render_page(false, "Die Suche nach \""+ value +"\" f&uuml;hrte zu keinem Treffer.")
                console.warn("Search ERROR.." + e)
            }
        });
    }

    /** Handle browsers geo-location API */

    this.ask_users_location = function (options) {
        // set default options
        if (options == undefined) {
            options =  {"setView" : true, "maxZoom" : kiezatlas.LEVEL_OF_KIEZ_ZOOM};
        }
        // ask browser for location-info
        kiezatlas.map.locate(options);
        // ("img.loading").hide();
    }

    this.on_location_found = function(e) {
        var radius = e.accuracy
        if (kiezatlas.location_circle != undefined) {
            kiezatlas.map.removeLayer(kiezatlas.location_circle);
        }
        var $mapMessage = $("#message.notification")
        // $mapMessage.show("fast")
        $mapMessage.html('Ihr Smartphone hat Sie gerade automatisch lokalisiert.<br/>'
            + 'Dr&uuml;cken Sie hier um den Kartenausschnit zur&uuml;ckzusetzen.')
        $mapMessage.click(kiezatlas.set_leaflet_map_to_current_bounds)
        $mapMessage.fadeIn(1000)
        setTimeout(function(e) {
            $mapMessage.fadeOut(3000)
        }, 3000)
        kiezatlas.location_circle = new L.circle(e.latlng, radius, {"stroke": true, "clickable": false, "color":
            "#1d1d1d", "fillOpacity": 0.3, "opacity": 0.3, "weight":10})
        kiezatlas.map.addLayer(kiezatlas.location_circle, {"clickable" : true})
        kiezatlas.location_circle.bindPopup("You are within " + radius + " meters from this point")
        kiezatlas.map.setView(e.latlng, kiezatlas.LEVEL_OF_KIEZ_ZOOM)
    }

    this.on_location_error = function (e) {
        // TODO: doesnt matter
    }



    /** Application controler utility methods */

    this.pop_history = function (state) {
        // simulate the back and forth navigation...
        if (!this.historyApiSupported) {
            return
        } else {
            if (state.view === "DETAIL_PAGE") {
                kiezatlas.load_old_object_info(state.id, function(e) {
                    kiezatlas.focus_selected_topic_in_map()
                    kiezatlas.render_page(true)
                })
            } else if (state.view === "SEARCH") {
                kiezatlas.do_search(true)
            } else {
                // fixme: console.log(state) // not yet implemented
            }
        }
    }

    this.push_history = function (state, link) {
        //
        if (!this.historyApiSupported) {
            return;
        }
        // build history entry
        var history_entry = {state: state, url: link}
        // push history entry
        window.history.pushState(history_entry.state, null, history_entry.url)
    }

    this.push_selected_detail_state = function () {

        var selected_topic = kiezatlas.get_selected_topic().result
        var permalink_entry_id = selected_topic.id
        var link = ""
        if (onEventMap || onProjectMap) {
            var origin_id = kiezatlas.get_origin_id_by_topic_id(selected_topic.id)
            permalink_entry_id = origin_id
            link = kiezatlas.get_base_url() + "/" + kiezatlas.get_map_web_alias() + "/linkTo/" + permalink_entry_id
        } else {
            link = kiezatlas.get_base_url() + "/" + kiezatlas.get_map_web_alias() + "/p/" + permalink_entry_id
        }
        // 
        var data = { "id": selected_topic.id, "view": "DETAIL_PAGE" }
        // build history entry
        kiezatlas.push_history(data, link)
    }

    this.push_search__state = function (search_value) {

        var link = kiezatlas.get_base_url() + "/" + kiezatlas.get_map_web_alias() + "/search/" + search_value
        var data = { "value": search_value, "view": "SEARCH" }
        // build history entry
        kiezatlas.push_history(data, link)
    }

    this.push_categorical_state = function () {

        var link = kiezatlas.get_base_url() + "/" + kiezatlas.get_map_web_alias() + "/criteria/" 
            + (kiezatlas.get_selected_criteria() + 1) + "/categories/"
        // 
        for ( var mi = 0; mi < kiezatlas.marker_group_ids.length; mi++ ) {
            link += kiezatlas.marker_group_ids[mi] + "%2C";
        }
        // 
        var data = { 
            "categories": kiezatlas.marker_group_ids, "criteria": kiezatlas.get_selected_criteria(),
            "view": "CATEGORICAL"
        }
        // build history entry
        kiezatlas.push_history(data, link)
    }

    this.check_for_small_screen = function () {
        if (screen.width < 980) { // exchange with screen.width
            kiezatlas.offerMobileWebApp = true
            var $mobile_btn = $('<div class="mobile-switch"><a href="http://m.kiezatlas.de/ehrenamt/list/">Bitte wechseln Sie hier mit einem Klick zur Ansicht f&uuml;r kleine Bildschirme, der Ehrenamtsnetz Web-App.</a></div><br/><br/>')
            $($mobile_btn).insertBefore('#kiezatlas #search-controls') 
            $('#details').css('top', '113px')
            $('#kiezatlas').height('645px')
        }
    }



    /** City Map Utility Methods */

    /** returns a Leaflet.Marker object (for identifying and building clusters in the ui) */
    this.get_marker_by_lat_lng = function (latLng) {
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

    this.clear_markers = function  () {
        for (var i = 0; i < kiezatlas.markers.length; i++) {
            var m = kiezatlas.markers[i];
            try {
                kiezatlas.map.removeLayer(m);
            } catch (e) {
                console.log("Exception: " + e);
            }
        }
    }

    this.get_current_bounds = function () {
        var bounds = new L.LatLngBounds()
        for (var i = 0; i < kiezatlas.markers.length; i++) {
            var m = kiezatlas.markers[i]
            // if (kiezatlas.map.hasLayer(m)) {
                var lng = m._latlng.lng
                var lat = m._latlng.lat
                var skip = false
                if (lat == 0.0 || lng == 0.0) {
                    skip = true;
                } else if (lng < -180.0 || lng > 180.0) {
                    skip = true
                } else if (lat < -90.0 || lat > 90.0) {
                    skip = true
                } else if (isNaN(lat) || isNaN(lng)) {
                    skip = true
                }
                if (!skip) {
                    var point = new L.LatLng(parseFloat(lat), parseFloat(lng))
                    bounds.extend(point)
                }
            // }
        }
        return bounds
    }

    /** sorting desc by item.value */
    this.alphabetical_sort_desc = function (a, b) {
        var scoreA = a.value
        var scoreB = b.value
        if (scoreA < scoreB) // sort string descending
          return -1
        if (scoreA > scoreB)
          return 1
        return 0 //default return value (no sorting)
    }

    this.window_height = function () {
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

    this.window_width = function () {
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

    this.create_webpage_link = function (url, label) {
        urlMarkup = '<a href="' + url + '" target="_blank">' + label + '</a>';
            //  + '<img src="css/link_extern.gif" alt="(externer Link)" border="0" height="11" width="12"/>
        // else urlMarkup = '<a href="'+url+'" target="_blank">'+label+'</a>';
        return urlMarkup
    }

    this.create_email_link = function (url, label) {
        urlMarkup = '<a href="mailto:' + url + '" target="_blank">' + label + '</a>';
        return urlMarkup
    }



    /** Java-Style kiezatlas-Object Getters & Setters */

    this.set_map = function(mapObject)  {
        this.map = mapObject
        //
        this.map.options.touchZoom = true
        kiezatlas.map.on('locationfound', kiezatlas.on_location_found)
        // kiezatlas.map.on('locationerror', kiezatlas.on_location_error)
    }

    this.set_map_topics = function(results) {
        this.map_topics = results.result
    }

    this.get_map_topics = function() {
        return this.map_topics
    }

    this.set_base_url = function(host_app_path) {
        this.base_url = host_app_path
    }

    this.get_base_url = function() {
        return this.base_url
    }

    this.set_map_web_alias = function(web_alias) {
        this.map_web_alias = web_alias
    }

    this.get_map_web_alias = function() {
        return this.map_web_alias
    }

    this.set_ajax_service_endpoint = function(service_url) {
        this.webservice_endpoint = service_url
    }

    this.get_ajax_service_endpoint = function() {
        return this.webservice_endpoint
    }

    this.set_selected_criteria = function(nr) {
        this.selected_criteria = nr
    }

    this.get_selected_criteria = function() {
        return this.selected_criteria
    }

    this.set_selected_topic = function(topic) {
        this.selected_topic = topic
    }

    this.get_selected_topic = function() {
        return this.selected_topic
    }

    this.set_map_topic_id = function (city_map_topic_id) {
        this.city_map_id = city_map_topic_id
    }

    this.get_map_topic_id = function () {
        return this.city_map_id
    }

    this.set_leaflet_map_to_current_bounds = function () {
        kiezatlas.map.fitBounds(kiezatlas.get_current_bounds())
    }

}
