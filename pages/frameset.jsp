<%@ include file="KiezAtlas.jsp" %>
<%
	BaseTopic map = (BaseTopic) session.getAttribute("map");
%>
<html>
<head>
	<title>Kiezatlas - <%= map.getName() %></title>
</head>
<frameset cols="*,360">
	<frame name="left" src="../pages/blank.html">
	<frame name="right" src="controller?action=initFrame&frame=<%= KiezAtlas.FRAME_RIGHT %>">
</frameset>
</html>
