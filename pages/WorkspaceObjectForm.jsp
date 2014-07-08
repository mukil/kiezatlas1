<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_EDIT, session, out); %>
<%!
	static String[] hiddenProps = {
		KiezAtlas.PROPERTY_ICON,
		KiezAtlas.PROPERTY_ADMINISTRATION_INFO,
		KiezAtlas.PROPERTY_LAST_MODIFIED,
        "Straße", KiezAtlas.PROPERTY_STREET, "Postleitzahl", KiezAtlas.PROPERTY_CITY, KiezAtlas.PROPERTY_TAGFIELD,
        KiezAtlas.PROPERTY_GPS_LONG, KiezAtlas.PROPERTY_GPS_LAT, KiezAtlas.PROPERTY_PASSWORD, KiezAtlas.PROPERTY_WEB_ALIAS,
        "Description", "Image", "YADE x", "YADE y", "Telefon", "Stra&szlig;e",
		"Forum Aktivierung",	/* can't use PROPERTY_FORUM_ACTIVITION because */
								/* relabled by GeoObjectTopic's propertyLabel() hook */
		"Title", "Content",
		"Width", "Height"
  };
%>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	String instTypeId = (String) session.getAttribute("instTypeId");
    Vector cityMaps = (Vector) session.getAttribute("cityMaps");
    StringBuffer cityMapsFormHtml = new StringBuffer("Bitte selektieren Sie mindestens einen der folgenden Stadtpl&auml;ne in denen ihr Datensatz ver&ouml;ffentlicht werden soll:<p>");
    for (int i=0; i < cityMaps.size(); i++) {
        BaseTopic cityMap = (BaseTopic) cityMaps.get(i);
        if (cityMaps.size() == 1) {
            // if there is just one map, select it right away...
            cityMapsFormHtml.append("<input type=\"checkbox\" value=\""+cityMap.getID()+"\" checked=\"true\" name=\"cityMap\" id=\""
            + cityMap.getID()+"\" /><label for=\""+cityMap.getID()+"\">" + cityMap.getName() + "</label>");
        } else {
            cityMapsFormHtml.append("<input type=\"checkbox\" value=\""+cityMap.getID()+"\" name=\"cityMap\" id=\""
            + cityMap.getID()+"\" /><label for=\""+cityMap.getID()+"\">" + cityMap.getName() + "</label>&nbsp;&nbsp;&nbsp;");
        }
    }
    cityMapsFormHtml.append("</p><br />");
	//
	// institution form
	out.println("<H2>Neuer Eintrag</H2>");
	out.println(html.extendedForm(instTypeId, KiezAtlas.ACTION_CREATE_GEO, hiddenProps, true, cityMapsFormHtml.toString()));
%>
<% end(out); %>
