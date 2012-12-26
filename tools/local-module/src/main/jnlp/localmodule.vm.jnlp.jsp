<%@page contentType="application/x-java-jnlp-file; charset=UTF-8"
%><%@page import="org.javlo.context.ContentContext"
%><%@page import="org.javlo.helper.URLHelper"%><%
ContentContext ctx = ContentContext.getContentContext(request, response);
ctx.setAbsoluteURL(true);
%><?xml version="1.0" encoding="utf-8"?>
<jnlp
	spec="$jnlpspec"
	codebase="<%=URLHelper.createStaticURL(ctx, "/webstart/")%>"
	context="$$context"
	href="$outputFile">
	<information>
		<title>Javlo LocalModule</title>
		<vendor>Javlo.org</vendor>
		<homepage href="http://www.javlo.org/" />
		<description kind="one-line">Javlo LocalModule</description>
		<description kind="short">Javlo LocalModule</description>
		<description kind="tooltip">Javlo LocalModule</description>
		<icon href="icon.png" kind="default" />
		<shortcut online="true">
			<desktop />
			<menu submenu="Javlo LocalModule" />
		</shortcut>
	</information>
	<security>
		<all-permissions />
	</security>
	<resources>
		<j2se version="$j2seVersion" initial-heap-size="32m" max-heap-size="128m" />
		$dependencies
		$extensions
	</resources>
	<application-desc main-class="$mainClass" />
</jnlp>