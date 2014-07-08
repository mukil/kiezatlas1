<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_EDIT, session, out); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic geo = (BaseTopic) session.getAttribute("geo");
%>
<h2><%= geo.getName() %></h2>
<form method="post">
	<table>
		<tr>
			<td><small>Passwort</small></td>
			<td><input TYPE="Password" NAME="Password"></td>
		</tr>
		<tr>
			<td></td>
			<td>
				<input TYPE="Submit" VALUE="Login">
				<input TYPE="Hidden" NAME="action" VALUE="<%= KiezAtlas.ACTION_TRY_LOGIN %>">
			</td>
		</tr>
	</table>
</form>
<% end(out); %>
