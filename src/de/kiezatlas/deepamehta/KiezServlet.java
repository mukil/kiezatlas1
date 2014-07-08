package de.kiezatlas.deepamehta;

import de.deepamehta.AmbiguousSemanticException;
import de.deepamehta.BaseTopic;
import de.deepamehta.service.*;
import de.deepamehta.service.web.JSONRPCServlet;
import de.deepamehta.topics.LiveTopic;
import de.deepamehta.topics.TypeTopic;

import de.kiezatlas.deepamehta.topics.CityMapTopic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KiezServlet extends JSONRPCServlet implements KiezAtlas {

    // --- 3 Hooks overriden
    protected String performPostRequest(String remoteMethod, String params,
            Session session, CorporateDirectives directives) {
        String result = "";
        if (remoteMethod.equals("getMapTopics")) {
            result = getGeoMapTopics(params, session, directives);
        } else if (remoteMethod.equals("getGeoObjectInfo")) {
            result = getGeoObjectInfo(params);
        } else if (remoteMethod.equals("getWorkspaceCriterias")) {
            result = getWorkspaceCriterias(params);
        } else if (remoteMethod.equals("getWorkspaceInfos")) {
            result = getWorkspaceInfos(params);
        } else if (remoteMethod.equals("getCityMaps")) {
            result = getCityMaps(params);
        } else if (remoteMethod.equals("getMobileCityMaps")) {
            result = getMobileCityMaps(params);
        } else if (remoteMethod.equals("searchGeoObjects")) {
            result = searchTopics(params, directives);
        } else if (remoteMethod.equals("geoCode")) {
            result = getGeoCode(params);
        } else if (remoteMethod.equals("oldGeoCode")) {
            result = getOldGeoCode(params);
        }
        return result;
    }

    protected String performAction(String topicId, String params, Session session, CorporateDirectives directives) {
        session.setAttribute("info", "<h3>Willkommen zu dem Kiezatlas Dienst unter "
                + as.getCorporateWebBaseURL() + "</h3><br>F&uuml;r die Nutzung des Dienstes steht Entwicklern "
                + "<a href=\"http://www.deepamehta.de/wiki/en/Application:_Web_Service\">hier</a> die Software "
                + " Dokumentation zur Verfügung. Ein Beispiel zur Nutzung eines Kiezatlas Dienstes ist "
                + "<a href=\"http://www.kiezatlas.de/maps/map.php?topicId=t-ka-schoeneberg&workspaceId=t-ka-workspace\">"
                + "hier</a> abrufbar.");
        return PAGE_SERVE;
    }

    protected void preparePage(String page, String params, Session session, CorporateDirectives directives) {
        session.setAttribute("forumActivition", "off");
    }

    // --- Remote Methods
    private String getGeoObjectInfo(String params) {
        String topicId = params.substring(2, params.length() - 2);
        System.out.println(">>>> getGeoObjectInfo(" + topicId + ")");
        // String parameters[] = params.split(",");
        StringBuffer messages = new StringBuffer("\"");
        StringBuffer result = new StringBuffer("{\"result\": ");
        String geoObjectString;
        try {
            BaseTopic t = as.getLiveTopic(topicId, 1);
            if (t != null && t.getType().equals("tt-user")) {
                System.out.println("*** KiezServlet.SecurityAccessDenied: not allowed to access user information");
                messages = new StringBuffer("Access Denied");
                geoObjectString = "\"\"";
            } else if (t != null) {
                geoObjectString = createGeoObjectBean(t, messages);
            } else {
                geoObjectString = "{}";
                messages.append("404 - Topic not found");
            }
        } catch (Exception tex) {
            geoObjectString = "{}";
            messages.append("" + toJSON(tex.toString()) + " - 404 - Topic not found");
        }
        result.append(geoObjectString);
        messages.append("\"");
        result.append(", \"error\": " + messages + "}");
        // System.out.println("result: "+ result.toString());
        return result.toString();
    }

    /**
     * Serializes Criterias and their Categories for a worksapce into JSON
     *
     * @param params
     * @return
     */
    private String getWorkspaceCriterias(String params) {
        String mapId = params.substring(2, params.length() - 2);
        StringBuffer messages = null;
        StringBuffer result = new StringBuffer("{\"result\": ");
        String criteriaList = createCritCatSystemList(mapId);
        result.append(criteriaList);
        result.append(", \"error\": " + messages + "}");
        // System.out.println("[DEBUG] .. " + result.toString() + " \n ");
        return result.toString();
    }

    /**
     * Serializes Workspace Infos into JSON
     *
     * @param params
     * @return
     */
    private String getWorkspaceInfos(String params) {
        System.out.println(">>>> getWorkspaceInfos(" + params + ")");
        String parameters[] = params.split(":");
        String workspaceId = parameters[0];
        StringBuffer messages = null;
        StringBuffer result = new StringBuffer("{\"result\": ");
        String infos = createWorkspaceInfos(workspaceId.substring(2, workspaceId.length() - 2));
        result.append(infos);
        result.append(", \"error\": " + messages + "}");
        return result.toString();
    }

    /**
     * Serializes Workspace Infos into JSON
     *
     * @param params
     * @return
     */
    private String getCityMaps(String params) {
        System.out.println(">>>> getCityMaps(" + params + ")");
        String parameters[] = params.split(":");
        String workspaceId = parameters[0];
        StringBuffer messages = null;
        StringBuffer result = new StringBuffer("{\"result\": ");
        String infos = createCityMaps(workspaceId.substring(2, workspaceId.length() - 2));
        result.append(infos);
        result.append(", \"error\": " + messages + "}");
        return result.toString();
    }

    /**
     * Serializes Infos on mobile CityMaps into JSON
     *
     * @param params
     * @return
     */
    private String getMobileCityMaps(String params) {
        System.out.println(">>>> getAllMobileCityMaps()");
        StringBuffer messages = null;
        StringBuffer result = new StringBuffer("{\"result\": ");
        String infos = createMobileCityMaps();
        result.append(infos);
        result.append(", \"error\": " + messages + "}");
        return result.toString();
    }

    /**
     * search for topic property name and returns a list of slim geo objects as results
     *
     * @param params
     * @param directives
     * @return
     */
    private String searchTopics(String params, CorporateDirectives directives) {
        System.out.println(">>>> searchTopic(" + params + ")");
        StringBuffer result = new StringBuffer("{\"result\": ");
        StringBuffer messages = null;
        String parameters[] = params.split(",");
        String query = parameters[0];
        String topicmapId = parameters[1];
        String workspaceId = parameters[2];
        query = query.substring(2, query.length() - 1);
        topicmapId = topicmapId.substring(2, topicmapId.length() - 1);
        workspaceId = workspaceId.substring(2, workspaceId.length() - 2);
        Vector criterias = getKiezCriteriaTypes(workspaceId);
        BaseTopic geoType = getWorkspaceGeoType(workspaceId);
        // perform search
        Vector results = cm.getViewTopics(topicmapId, 1, geoType.getID(), query);
        if (!query.equals("")) {
            Hashtable propertyFilter = new Hashtable();
            propertyFilter.put(PROPERTY_TAGFIELD, query);
            Vector taggedInsts = cm.getTopics(geoType.getID(), propertyFilter, topicmapId);
            for (int k = 0; k < taggedInsts.size(); k++) {
                BaseTopic taggedTopic = (BaseTopic) taggedInsts.get(k);
                Vector pts = cm.getViewTopics(topicmapId, 1, geoType.getID(), taggedTopic.getName());
                if (pts.size() >= 1) {
                    if (!results.contains(pts.get(0))) { // prevent doublings..
                        results.add(pts.get(0));
                    }
                } // insts.add(new PresentableTopic(topic, new Point()));
            }
        }
        // +" named and "+streetResults.size()+ " streetnames like " + query);
        System.out.println("    >> found " + results.size() + " by name and tag");
        //
        result.append("[");
        for (int i = 0; i < results.size(); i++) {
            BaseTopic topic = (BaseTopic) results.get(i);
            result.append(createSlimGeoObject(topic, criterias, new StringBuffer()));
            if (results.indexOf(topic) == results.size() - 1) { // does not allow doublings
                result.append("]");
            } else {
                result.append(", ");
            }
        }
        if (results.isEmpty()) {
            result.append("]");
        }
        result.append(", \"error\": " + messages + "}");
        return result.toString();
    }

    /**
     * delivers all topics in a citymap as "slim" geoobjects
     *
     * @param params (mapId, workspaceId)
     * @param session
     * @param directives
     * @return
     */
    private String getGeoMapTopics(String params, Session session, CorporateDirectives directives) {
        String parameters[] = params.split(",");
        String mapId = parameters[0];
        String workspaceId = parameters[1];
        mapId = mapId.substring(2, mapId.length() - 2);
        workspaceId = workspaceId.substring(2, workspaceId.length() - 2);
        StringBuffer messages = null;
        StringBuffer result = new StringBuffer("{\"result\": ");
        StringBuffer mapTopics = new StringBuffer("{ \"map\": \"" + mapId + "\", \"topics\": [");
        //
        BaseTopic geoType = (BaseTopic) getWorkspaceGeoType(workspaceId);
        Vector criterias = getKiezCriteriaTypes(workspaceId);
        Vector allTopics = cm.getTopics(geoType.getID(), new Hashtable(), mapId);
        System.out.println(">>> " + allTopics.size() + " within current map");
        for (int i = 0; i < allTopics.size(); i++) {
            BaseTopic topic = (BaseTopic) allTopics.get(i);
            String geo = createSlimGeoObject(topic, criterias, messages);
            mapTopics.append(geo);
            if (allTopics.indexOf(topic) != allTopics.size() - 1) {
                mapTopics.append(",");
            }
        }
        mapTopics.append("]}");
        result.append(mapTopics);
        result.append(", \"error\": " + messages + "}");
        return result.toString();
    }

    private String getGeoCode(String params) {
        // Set your return content type
        // String q = params;// .getParameter("q");
        String parameters[] = params.split(",");
        String query = parameters[0];
        String key = parameters[1];
        String locale = parameters[2];
        String result = "";
        // String topicmapId = parameters[2];
        query = query.substring(2, query.length() - 1);
        key = key.substring(2, key.length() - 1);
        locale = locale.substring(2, locale.length() - 2);
        try {
            // Encoded url to open
            String url = "http://maps.googleapis.com/maps/api/geocode/json?address=" + query + "&sensor=false&locale=" + locale;
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Charset", "UTF-8");
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            result = sb.toString();
        } catch (UnsupportedEncodingException ex) {
            System.out.println("*** KiezServlet.getGeoCode ERROR: " + ex.getMessage());
        } catch (MalformedURLException mux) {
            System.out.println("*** KiezServlet.getGeoCode ERROR: " + mux.getMessage());
        } catch (IOException ioex) {
            System.out.println("*** KiezServlet.getGeoCode ERROR: " + ioex.getMessage());
        }
        //
        return result;
    }

    private String getOldGeoCode(String params) {
        // Set your return content type
        // String q = params;// .getParameter("q");
        String parameters[] = params.split(",");
        String query = parameters[0];
        String key = parameters[1];
        String locale = parameters[2];
        String result = "";
        // String topicmapId = parameters[2];
        query = query.substring(2, query.length() - 1);
        key = key.substring(2, key.length() - 1);
        locale = locale.substring(2, locale.length() - 2);
        try {
            // query = URLEncoder.encode(query, "UTF-8");
            query = query.replaceAll(" ", "%20");
            // Website url to open
            // ### FIXME: "gl=+locale"
            if (locale.equals("")) {
                locale = "de";
            }
            String url = "http://maps.google.com/maps/geo?q="
                    + query + "&oe=utf8&key=" + key + "&output=json&sensotr=false&gl=" + locale;
            System.out.println("OldGeoCode =>\"" + url + "\"");
            // String url = "http://maps.google.com/maps/geo?q="
            // + query + "&output=json&oe=utf8&sensotr=false&key="+key+locale;
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Charset", "ISO-8859-1");
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            result = sb.toString();
        } catch (UnsupportedEncodingException ex) {
            System.out.println("*** KiezServlet.getOldGeoCode ERROR: " + ex.getMessage());
        } catch (MalformedURLException mux) {
            System.out.println("*** KiezServlet.getOldGeoCode ERROR: " + mux.getMessage());
        } catch (IOException ioex) {
            System.out.println("*** KiezServlet.getOldGeoCode ERROR: " + ioex.getMessage());
        }
        //
        return result;
    }

    // -------------------
    // --- Utility Methods
    // -------------------
    private String createWorkspaceInfos(String workspaceId) {
        StringBuffer object = new StringBuffer();
        String workspaceName = as.getTopicProperty(workspaceId, 1, PROPERTY_NAME);
        String logoURL = "";
        String impressumURL = "";
        String homepageURL = "";
        //
        BaseTopic logo = as.getRelatedTopic(workspaceId, ASSOCTYPE_ASSOCIATION, TOPICTYPE_IMAGE, 2, true);
        if (logo != null) {
            logoURL = as.getTopicProperty(logo, PROPERTY_FILE);
        }
        BaseTopic homepage = as.getRelatedTopic(workspaceId,
                KiezAtlas.ASSOCTYPE_HOMEPAGE_LINK, TOPICTYPE_WEBPAGE, 2, true);
        if (homepage != null) {
            homepageURL = as.getTopicProperty(homepage, PROPERTY_URL);
        }
        BaseTopic impressum = as.getRelatedTopic(workspaceId,
                KiezAtlas.ASSOCTYPE_IMPRESSUM_LINK, TOPICTYPE_WEBPAGE, 2, true);
        if (impressum != null) {
            impressumURL = as.getTopicProperty(impressum, PROPERTY_URL);
        }
        //
        object.append("{\"name\": \"" + workspaceName + "\",");
        object.append("\"logo\": \"" + logoURL + "\",");
        object.append("\"imprint\": \"" + impressumURL + "\",");
        object.append("\"homepage\": \"" + homepageURL + "\"");
        object.append("}");
        return object.toString();
    }

    private String createCityMaps(String workspaceId) {
        StringBuffer object = new StringBuffer();
        String workspaceName = as.getTopicProperty(workspaceId, 1, PROPERTY_NAME);
        BaseTopic wsMap = as.getRelatedTopic(workspaceId, ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
        //
        object.append("{\"name\": \"" + workspaceName + "\",");
        object.append("\"id\": \"" + workspaceId + "\",");
        object.append("\"maps\": [");
        Vector cityMaps = cm.getViewTopics(wsMap.getID(), 1, TOPICTYPE_CITYMAP);
        System.out.println("   > found : " + cityMaps.size() + " citymaps");
        for (int p = 0; p < cityMaps.size(); p++) {
            BaseTopic cityMap = (BaseTopic) cityMaps.get(p);
            String id, name, webAlias = "";
            id = cityMap.getID();
            name = cityMap.getName();
            webAlias = as.getTopicProperty(cityMap, PROPERTY_WEB_ALIAS);
            object.append("{");
            if (!webAlias.equals("")) {
                object.append("\"id\": \"" + id + "\", ");
                object.append("\"name\": \"" + toJSON(name) + "\", ");
                object.append("\"alias\": \"" + webAlias + "\" ");
                //
            }
            if (cityMaps.indexOf(cityMap) == cityMaps.size() - 1) {
                object.append("}");
            } else {
                object.append("},");
            }
        }
        object.append("]");
        object.append("}");
        return object.toString();
    }

    private String createMobileCityMaps() {
        StringBuffer object = new StringBuffer();
        Hashtable propertyFilter = new Hashtable();
        // all citymaps have their "mobile citymap"-flag set
        propertyFilter.put(PROPERTY_MOBILE_CITYMAP, "on");
        Vector mobileCityMaps = cm.getTopics("tt-ka-stadtplan", propertyFilter);
        //
        object.append("{\"name\": \"Mobile Stadtpl&auml;ne im KiezAtlas\",");
        object.append("\"id\": \"0\",");
        object.append("\"maps\": [");
        for (int p = 0; p < mobileCityMaps.size(); p++) {
            String id, name, webAlias, workspaceId = "";
            BaseTopic cityMap = (BaseTopic) mobileCityMaps.get(p);
            LiveTopic cityMapLive = (LiveTopic) as.getLiveTopic(cityMap.getID(), 1);
            Vector childs = as.getRelatedTopics(cityMapLive.getID(), ASSOCTYPE_DERIVATION, 2);
            // derived topics are related to the corresponding workspace..
            if (childs != null) {
                for (int i = 0; i < childs.size(); i++) {
                    BaseTopic subtopic = (BaseTopic) childs.get(i);
                    BaseTopic workspace = as.getRelatedTopic(subtopic.getID(), ASSOCTYPE_PUBLISHING, TOPICTYPE_WORKSPACE, 2, true);
                    if (workspace != null) {
                        workspaceId = workspace.getID();
                        break; // should be done with the outer for loop.. double check.
                    }
                }
            }
            id = cityMap.getID();
            name = cityMap.getName();
            webAlias = as.getTopicProperty(cityMap, PROPERTY_WEB_ALIAS);
            // apend all citymaps who have the web alias set and are currently part of the workspace-topicmap
            if (!webAlias.equals("") && isPublishedMobileCityMap(workspaceId, id)) {
                object.append("{");
                object.append("\"id\": \"" + id + "\", ");
                object.append("\"name\": \"" + toJSON(name) + "\", ");
                object.append("\"alias\": \"" + webAlias + "\", ");
                object.append("\"workspaceId\": \"" + workspaceId + "\" ");
                if (mobileCityMaps.indexOf(cityMap) == mobileCityMaps.size() - 1) {
                    object.append("}");
                } else {
                    object.append("},");
                }
            }
        }
        object.append("]");
        object.append("}");
        return object.toString();
    }

    private boolean isPublishedMobileCityMap(String workspaceId, String mobileCityMapId) {
        BaseTopic workspaceMap = as.getRelatedTopic(workspaceId, ASSOCTYPE_AGGREGATION, TOPICTYPE_TOPICMAP, 2, true);
        if (workspaceMap != null) {
            int tv = cm.getViewTopicVersion(workspaceMap.getID(), 1, "", mobileCityMapId);
            return (tv != 0) ? true : false;
        }
        return false;
    }

    private String createSlimGeoObject(BaseTopic topic, Vector criterias, StringBuffer messages) {
        StringBuffer object = new StringBuffer();
        //
        String latitude = as.getTopicProperty(topic, "LAT"); // ### get an interface place for this final string value
        String longnitude = as.getTopicProperty(topic, "LONG"); // ### get an interface place for this final string
        String originId = as.getTopicProperty(topic, PROPERTY_PROJECT_ORIGIN_ID);
        if (latitude.equals("") && longnitude.equals("")) {
            latitude = "0.0";
            longnitude = "0.0";
        }
        String name = topic.getName();
        name = toJSON(name);
        //}
        object.append("{\"name\": \"" + name + "\",");
        object.append("\"id\": \"" + topic.getID() + "\",");
        object.append("\"originId\": \"" + originId + "\",");
        object.append("\"lat\": \"" + latitude + "\",");
        object.append("\"long\": \"" + longnitude + "\",");
        // System.out.println(">>> createCritCatList(" + topic.getID()+") ...");
        object.append("\"criterias\": " + createTopicCategorizations(topic.getID(), criterias));
        object.append("}");
        return object.toString();
    }

    /**
     * Serializes TopicBean into JSON
     *
     * @param topic
     * @param messages
     * @return
     */
    private String createGeoObjectBean(BaseTopic topic, StringBuffer messages) {
        StringBuffer bean = new StringBuffer();
        //
        TopicBean topicBean = as.createTopicBean(topic.getID(), 1);
        String adminInfo = topicBean.getValue(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
        if (adminInfo != null && adminInfo.indexOf("lor/analysen/") != -1) {
            topicBean = prepareNewLorPageLink(topicBean);
        }
        removeCredentialInformation(topicBean);
        String topicName = removeQuotationMarksFromNames(topicBean.name);
        bean.append("{\"id\": \"" + topicBean.id + "\",");
        bean.append("\"name\": \"" + topicName + "\",");
        bean.append("\"icon\": \"" + topicBean.icon + "\",");
        bean.append("\"properties\": [");
        Vector properties = topicBean.fields;
        for (int p = 0; p < properties.size(); p++) {
            TopicBeanField field = (TopicBeanField) properties.get(p);
            bean.append("{\"type\": \"" + field.type + "\", ");
            bean.append("\"name\": \"" + field.name + "\", ");
            bean.append("\"label\": \"" + field.label + "\", ");
            if (field.type == 0) {
                String value = field.value;
                value = toJSON(value);
                bean.append("\"value\":  \"" + value + "\"");
            } else {
                Vector relatedFields = field.values;
                if (relatedFields.size() == 0) {
                    bean.append("\"values\": []");
                } else {
                    bean.append("\"values\": [");
                    for (int r = 0; r < relatedFields.size(); r++) {
                        BaseTopic relatedTopic = (BaseTopic) relatedFields.get(r);
                        bean.append("{\"name\": \"" + relatedTopic.getName() + "\",");
                        // ### geoObject has it's own icon ?
                        bean.append("\"icon\": \"" + as.getLiveTopic(relatedTopic).getIconfile() + "\"}");
                        if (r == relatedFields.size() - 1) {
                            bean.append("]");
                        } else {
                            bean.append(", ");
                        }
                    }
                }
            }
            if (properties.indexOf(field) == properties.size() - 1) {
                bean.append("}");
            } else {
                bean.append("},");
            }
        }
        bean.append("]");
        bean.append("}");
        return bean.toString();
    }

    private TopicBean prepareNewLorPageLink(TopicBean topicBean) {
        // start migrating all links in the system to the new lor-pages
        String lorId = null;
        String adminInfo = topicBean.getValue(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
        Pattern pattern = Pattern.compile("(<a href=\"\\D+)(/analysen/)(\\d+)(.pdf)(\\D+)(target=\"_blank\">)(\\D+)(</a>)");
        Matcher matcher = pattern.matcher(adminInfo);
        while (matcher.find()) {
            Pattern idPattern = Pattern.compile("(\\d+)");
            Matcher idMatch = idPattern.matcher(matcher.group());
            while (idMatch.find()) {
                lorId = idMatch.group();
            }
            if (lorId != null) {
                String before = adminInfo.substring(0, matcher.start());
                String link = "<a href=\"http://jugendserver.spinnenwerk.de/~lor/seiten/2013/06/?lor="
                        + lorId + "\" target=\"_blank\">Sozialdaten Planungsraum</a>";
                String after = adminInfo.substring(matcher.end());
                adminInfo = before + link + after;
                TopicBeanField adminField = topicBean.getField(KiezAtlas.PROPERTY_ADMINISTRATION_INFO);
                adminField.value = adminInfo; // migration sucessfull
            }
        }
        return topicBean;
    }

    private TopicBean removeCredentialInformation(TopicBean topicBean) {
        topicBean.removeFieldsContaining(PROPERTY_PASSWORD);
        topicBean.removeFieldsContaining(PROPERTY_OWNER_ID);
        topicBean.removeFieldsContaining(PROPERTY_WEB_ALIAS);
        topicBean.removeFieldsContaining(PROPERTY_LAST_MODIFIED);
        //
        return topicBean;
    }

    /**
     * creates a list of categorizations for each slim topic checks for each criteria type if one is directly associated
     * with the category
     *
     * @param topicId
     * @param workspaceId
     * @return
     */
    private String createTopicCategorizations(String topicId, Vector criterias) {
        StringBuffer catList = new StringBuffer("[");
        BaseTopic topic = cm.getTopic(topicId, 1);
        for (int i = 0; i < criterias.size(); i++) {
            BaseTopic criteria = (BaseTopic) criterias.get(i);
            // which topics are related and are of type criteria
            Vector categories = as.getRelatedTopics(topic.getID(), "at-association", criteria.getID(), 2);
            int andex;
            if (categories.size() == 0) {
                catList.append("{\"critId\": \"" + criteria.getID() + "\", ");
                catList.append("\"categories\": []");
                andex = criterias.indexOf(criteria);
                // awkyard
                if (andex == criterias.size() - 1) {
                    catList.append("}]");
                } else {
                    catList.append("},");
                }
                continue;
            }
            catList.append("{\"critId\": \"" + criteria.getID() + "\", ");
            catList.append("\"categories\": [");
            for (int c = 0; c < categories.size(); c++) {
                BaseTopic cat = (BaseTopic) categories.get(c);
                int index = categories.indexOf(cat);
                // awkyard
                if (index == categories.size() - 1) {
                    catList.append("\"" + cat.getID() + "\"");
                } else {
                    catList.append("\"" + cat.getID() + "\", ");
                }
            }
            int c = criterias.indexOf(criteria);
            if (c == criterias.size() - 1) {
                catList.append("]}]");
            } else {
                catList.append("]},");
            }
        }
        if (criterias.size() == 0) {
            catList.append("]");
        }
        return catList.toString();
    }

    /**
     * list of all categorizations for one workspace
     *
     * @param workspaceId
     * @return
     */
    private String createCritCatSystemList(String mapId) {
        StringBuffer objectList = new StringBuffer();
        objectList.append("[");
        // Vector criterias = getKiezCriteriaTypes(workspaceId);
        CityMapTopic mapTopic = (CityMapTopic) as.getLiveTopic(mapId, 1);
        SearchCriteria[] crits = mapTopic.getSearchCriterias();
        // Vector collectedCrits = new Vector();
        for (int i = 0; i < crits.length; i++) {
            SearchCriteria searchCriteria = crits[i];
            // collectedCrits.add(searchCriteria.criteria.getID());
            objectList.append("{\"critId\": \"" + searchCriteria.criteria.getID() + "\", ");
            objectList.append("\"critName\": \"" + toJSON(searchCriteria.criteria.getName()) + "\", ");
            objectList.append("\"categories\": [");
            // sorted query
            Vector categories = cm.getTopics(searchCriteria.criteria.getID(), null, new Hashtable(), null, null, true);
            for (int c = 0; c < categories.size(); c++) {
                objectList.append("{");
                objectList.append("\"catId\":");
                BaseTopic cat = (BaseTopic) categories.get(c);
                // System.out.println(">>>> category(" + cat.getName() +" icon: "+ as.getLiveTopic(cat).getIconfile());
                objectList.append("\"" + cat.getID() + "\", ");
                objectList.append("\"catName\":");
                objectList.append("\"" + toJSON(cat.getName()) + "\", ");
                objectList.append("\"catIcon\":");
                objectList.append("\"" + as.getLiveTopic(cat).getIconfile() + "\"");
                //
                int index = categories.indexOf(cat);
                if (index == categories.size() - 1) {
                    objectList.append("}");
                } else {
                    objectList.append("},");
                }
            }
            if (i == crits.length - 1) {
                objectList.append("]}");
            } else {
                objectList.append("]},");
            }
        }
        objectList.append("]");
        // System.out.println("list is: " + objectList);
        return objectList.toString();
    }

    /**
     * simply retrieves the topics assigned to a workspace which are used for navigation in web-frontends
     *
     * @param workspaceId
     * @return
     */
    private Vector getKiezCriteriaTypes(String workspaceId) {
        //
        Vector criterias = new Vector();
        TypeTopic critType = as.type("tt-ka-kriterium", 1);
        Vector subtypes = critType.getSubtypeIDs();
        Vector workspacetypes = as.getRelatedTopics(workspaceId, "at-uses", TOPICTYPE_TOPICTYPE, 2, true, true);
        for (int i = 0; i < workspacetypes.size(); i++) {
            BaseTopic topic = (BaseTopic) workspacetypes.get(i);
            for (int a = 0; a < subtypes.size(); a++) {
                String derivedOne = (String) subtypes.get(a);
                if (derivedOne.equals(topic.getID())) {
                    // System.out.println(">>> use criteria (" + derivedOne + ") " + topic.getName());
                    criterias.add(topic);
                }
            }
        }
        return criterias;
    }

    // ### ToDo: as.getLiveTopic().getSearchCriteria();
    /**
     * returns null if no topictype whihc is assigned to the given workspace, is a subtype of "GeoObjectTopic"
     *
     * @param workspaceId
     * @return
     */
    private BaseTopic getWorkspaceGeoType(String workspaceId) {
        //
        TypeTopic geotype = as.type(TOPICTYPE_KIEZ_GEO, 1);
        Vector subtypes = geotype.getSubtypeIDs();
        Vector workspacetypes = as.getRelatedTopics(workspaceId, ASSOCTYPE_USES, 2);
        int i;
        for (i = 0; i < workspacetypes.size(); i++) {
            BaseTopic topic = (BaseTopic) workspacetypes.get(i);
            int a;
            for (a = 0; a < subtypes.size(); a++) {
                // System.out.println(" counter: " + a);
                String derivedOne = (String) subtypes.get(a);
                // System.out.println("    " + derivedOne.getID() + ":" + derivedOne.getName());
                if (derivedOne.equals(topic.getID())) {
                    // System.out.println(" found geoType " + topic.getID() + ":" + topic.getName());
                    return topic;
                }
            }
        }
        return null;
    }

    private boolean hasQuotationMarks(String value) {
        return value.indexOf("\"") != -1;
    }

    private String removeQuotationMarksFromNames(String name) {
        name = name.replaceAll("\"", "");
        return name;
    }

    private String removeControlChars(String value) {
        // html uses carriage-return, line-feed and horizontal tab
        value = value.replaceAll("\r", "\\\\r");
        value = value.replaceAll("\n", "\\\\n");
        value = value.replaceAll("\t", "\\\\t");
        value = value.replaceAll("\f", "\\\\f");
        value = value.replaceAll("\b", "\\\\b");
        value = value.replaceAll("\"", "\\\\\"");
        //System.out.println("replaced value is : " + value);
        return value;
    }

    private String toJSON(String text) {
        // strip HTML tags
        text = text.replaceAll("<html>", "");
        text = text.replaceAll("</html>", "");
        text = text.replaceAll("<head>", "");
        text = text.replaceAll("</head>", "");
        text = text.replaceAll("<body>", "");
        text = text.replaceAll("</body>", "");
        text = text.replaceAll("<p>", "");
        text = text.replaceAll("<p style=\"margin-top: 0\">", "");
        text = text.replaceAll("</p>", "");
        text = text.replaceAll("'", "\'");
        text = text.replaceAll("&", "&amp;");
        // text = text.replaceAll("\'", "");
        // convert HTML entities
        text = toUnicode(text);
        //
        text = text.trim();
        // JSON conformity
        text = removeControlChars(text);
        //
        return text;
    }

    private String toUnicode(String text) {
        StringBuffer buffer = new StringBuffer();
        Pattern p = Pattern.compile("&#(\\d+);");
        Matcher m = p.matcher(text);
        while (m.find()) {
            int c = Integer.parseInt(m.group(1));
            m.appendReplacement(buffer, Character.toString((char) c));
        }
        m.appendTail(buffer);
        return buffer.toString();
    }
}
