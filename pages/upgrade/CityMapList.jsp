<%@ include file="../KiezAtlas.jsp" %>

<form method="POST">

    <%
        Vector cityMaps = (Vector) session.getAttribute("cityMaps");
        StringBuffer cityMapsFormHtml = new StringBuffer("Bitte selektieren Sie den zu migrierenden Stadtplan:<p>");
        for (int i=0; i < cityMaps.size(); i++) {
            BaseTopic cityMap = (BaseTopic) cityMaps.get(i);
            if (cityMaps.size() == 1) {
                // if there is just one map, select it right away...
                cityMapsFormHtml.append("<input type=\"checkbox\" value=\""+cityMap.getID()+"\" checked=\"true\" name=\"cityMap\" id=\""
                + cityMap.getID()+"\" /><label for=\""+cityMap.getID()+"\">" + cityMap.getName() + "</label>");
            } else {
                cityMapsFormHtml.append("<input type=\"checkbox\" value=\""+cityMap.getID()+"\" name=\"cityMap\" id=\""
                + cityMap.getID()+"\" /><label for=\""+cityMap.getID()+"\">" + cityMap.getName() + "</label></br>");
            }
        }
        cityMapsFormHtml.append("</p><br />");
        //
        out.println(cityMapsFormHtml);
    %>

    <input type="submit" value="OK">
    <input type="hidden" name="action" value="<%= KiezAtlas.ACTION_MIGRATE_CITYMAP %>">

</form>

<% end(out); %>
