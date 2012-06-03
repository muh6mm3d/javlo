<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${fn:length(page.children) gt 0}">
<ul class="menu">	
	<c:forEach var="page" items="${page.children}" varStatus="status">
		<c:if test="${page.info.visible}">
  				<c:if test="${page.info.depth <= end}">				
				<li class="depth-${page.info.depth} ${page.selected ? "active" : "not-active" }">
					<a href="${page.url}" title="${page.info.title}" >${page.info.label}</a>				
    				<c:set var="page" value="${page}" scope="request" />		 
		        	<jsp:include page="default.jsp"/>
		        </li>
		 	</c:if>	
	</c:if>				 			
	</c:forEach>
</ul>
</c:if>