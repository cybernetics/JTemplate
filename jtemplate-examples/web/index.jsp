<%@page pageEncoding="UTF-8"%>

<html>
<head>
<title>Test Form</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/events" method="post">
<table>
<tr>
<td>Title</td><td><input type="text" name="title"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Add Event"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/upload" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>File</td><td><input type="file" name="file"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Upload"/></td>
</tr>
</table>
</form>

<hr/>

<a href="${pageContext.request.contextPath}/keys/a/123/b/héllo/c/456?d=göodbye">Key List</a></br>
<a href="${pageContext.request.contextPath}/keys/a/123/b/héllo/c/456/~html?d=göodbye">Key List (HTML)</a></br>
</body>
</html>
