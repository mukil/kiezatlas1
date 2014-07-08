<%@ include file="KiezAtlas.jsp" %>

<%@page contentType="text/html" pageEncoding="iso-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%  String link = (String) session.getAttribute("formLetter");
    out.println("<h3>Download der Steuerdatei</h3>");
    out.println("<p>Bitte klicken Sie mit der rechten Maustaste auf den untenstehenden Link und wählen Sie \"Ziel Speichern unter\"</p>");
    out.println("<a href=\"" + link + "\">Steuerdatei herunterladen</a>");
%>
