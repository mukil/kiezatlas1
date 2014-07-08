<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<%
	HTMLGenerator html = (HTMLGenerator) session.getAttribute("html");
	BaseTopic workspace = (BaseTopic) session.getAttribute("workspace");
%>
<h2><%= workspace.getName() %> - Zur Eintragung eines Datensatzes</h2>
<form method="post">
	<table>
		<tr>
      <td><small>Username</small></td>
			<td><input TYPE="text" NAME="Username"></td>
    </tr>
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
