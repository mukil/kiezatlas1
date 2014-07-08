<%@ include file="../KiezAtlas.jsp" %>

<%@page contentType="text/html" pageEncoding="iso-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%
    String mapAlias = (String) session.getAttribute("mapAlias");
    out.println("It's about " + mapAlias);
%>
