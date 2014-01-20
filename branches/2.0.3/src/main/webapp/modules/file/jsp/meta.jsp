<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<div id="meta-edit" class="form-list">

<form id="form-meta" action="${info.currentURL}" method="post">

<input type="hidden" name="webaction" value="updateMeta" />
<c:if test="${not empty param[BACK_PARAM_NAME]}"><input type="hidden" name="${BACK_PARAM_NAME}" value="${param[BACK_PARAM_NAME]}" /></c:if>

<ul>
<c:forEach var="file" items="${files}">
	<li class="${file.directory?'directory':'file'} ${not empty param.select?'select':'no-select'}">
	    <c:set var="popularity" value=" - #${file.popularity}" />
		<div class="title">
			<span><a href="${file.URL}" title="${file.name}">${file.name}</a></span>
			<c:if test="${empty param.select}">
				<c:url value="${info.currentURL}" var="deleteURL">
					<c:param name="webaction" value="file.delete" />
					<c:param name="file" value="${file.path}" />
					<c:if test="${not empty param[BACK_PARAM_NAME]}">
						<c:param name="${BACK_PARAM_NAME}" value="${param[BACK_PARAM_NAME]}" />
					</c:if>
				</c:url>			
				<span class="delete"><a class="needconfirm" href="${deleteURL}">X</a></span>
			</c:if>
			<span class="size">${file.size} <span class="popularity">${info.admin?popularity:''}</span></span>
			<span class="last">${file.manType}</span>
		</div>
		<c:if test="${not empty param.select}">		
				<div class="special-action">	
					<c:if test="${file.image}">			
						<input class="select-item mce-close" type="button" value="select" data-url="${file.freeURL}" />						
					</c:if>
					<c:if test="${!file.image}">
						<span>unselectable</span>
					</c:if>					
				</div>
			</c:if>						
		
		<div class="body">
		<c:if test="${file.image}">		
		<div class="download picture">
			<div class="focus-zone">
			<a rel="image" href="${file.URL}"><img src="${file.thumbURL}" />&nbsp;</a>
			<div class="focus-point">x</div>			
			<input class="posx" type="hidden" name="posx-${file.id}" value="${file.focusZoneX}" />
			<input class="posy" type="hidden" name="posy-${file.id}" value="${file.focusZoneY}" />
			</div>	
		</div>
		</c:if>
		<c:if test="${not file.image}">
		<c:url var="fileURL" value="${file.URL}">
			<c:if test="${not empty param.select}">
				<c:param name="select" value="${param.select}"></c:param>
			</c:if>
		</c:url>
		<div class="download file ${file.type}"><a href="${fileURL}">${file.name}</a></div>
		</c:if>
		<div class="line">
			<label for="title-${file.id}">${i18n.edit["field.title"]}</label>
			<input class="file-title" type="text" id="title-${file.id}" name="title-${file.id}" value="${file.title}" />
		</div>
		<div class="line">
			<label for="description-${file.id}">${i18n.edit["field.description"]}</label>
			<textarea class="file-description" id="description-${file.id}" name="description-${file.id}" rows="5" cols="10">${file.description}</textarea>
		</div>
		<div class="line">
			<label for="location-${file.id}">${i18n.edit["field.location"]}</label>
			<input class="file-location" type="text" id="location-${file.id}" name="location-${file.id}" value="${file.location}" />
		</div>
		<div class="line">
			<label for="date-${file.id}">${i18n.edit["field.date"]}</label>
			<input class="file-date" type="text" id="date-${file.id}" name="date-${file.id}" value="${file.manualDate}" />
		</div>
		<div class="line">
			<label for="shared-${file.id}">${i18n.edit["field.shared"]}</label>
			<input type="checkbox" id="shared-${file.id}" name="shared-${file.id}" ${file.shared?'checked="checked"':''} />
		</div>				
						
		<c:if test="${fn:length(tags) > 0}">
		<fieldset class="tags">
		<legend>${i18n.edit["field.tags"]}</legend>
		    <c:forEach var="tag" items="${tags}">		    	
				<span><input type="checkbox" id="tag_${tag}_${file.id}" name="tag_${tag}_${file.id}" ${not empty file.tags[tag]?'checked="checked"':''}/><label for="tag_${tag}_${file.id}">${tag}</label></span>
			</c:forEach>
		</fieldset>
		</c:if>
		</div>
	</li>
</c:forEach>
</ul>

<div class="actions">
	<input class="action-button" type="submit" value="${i18n.edit['global.save']}"/> 
</div>

</form>

</div>

<c:if test="${not empty param.select}">
	<script type="text/javascript">
		jQuery(".select-item").click(function() {
			var fieldName = parent.jQuery("body").data("fieldName");
			var url = jQuery(this).data("url");
			parent.jQuery("#"+fieldName).val(url);
			parent.tinyMCE.activeEditor.windowManager.close(window);
		});
	</script>	
</c:if>