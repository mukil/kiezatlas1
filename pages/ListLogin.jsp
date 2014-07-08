<%@ include file="KiezAtlas.jsp" %>

<% begin(KiezAtlas.SERVLET_LIST, session, out); %>
<h2>Zugang zu den Listen</h2>
<form method="post">
	<table>
		<tr>
			<td><small>Username</small></td>
			<td><input TYPE="Text" NAME="username"></td>
		</tr>
		<tr>
			<td><small>Passwort</small></td>
			<td><input TYPE="Password" NAME="password"></td>
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
