<%@ include file="../KiezAtlas.jsp" %>

<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic workspace = (BaseTopic) session.getAttribute("workspace");
%>
<h2>Zur Migration des Workspace <%= workspace.getName() %> </h2>
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
