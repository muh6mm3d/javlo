<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${fn:length(info.contentLanguages) > 1 and empty param.previewEdit && empty param.templateid}">
<div class="special${empty componentsList?' last':''}">
<form id="form-languages" action="${info.currentURL}" method="get" class="js-submit">
<div class="select-languages form_default">
	<input type="hidden" name="webaction" value="edit.changeLanguage" />	
	<c:if test="${not empty param[BACK_PARAM_NAME]}"><input type="hidden" name="${BACK_PARAM_NAME}" value="${param[BACK_PARAM_NAME]}" /></c:if>
	<select name="language">
	<c:forEach var="lang" items="${info.contentLanguages}">
		<option value="${lang}"${lang eq info.contentLanguage?' selected="selected"':''}>${lang}</option>
	</c:forEach>
	</select>
	<input class="action-button" type="submit" name="ok" value="${i18n.edit['global.ok']}" />
</div>
</form>
</div>
</c:if>

<c:if test="${not empty param[BACK_PARAM_NAME]}"><a class="action-button back" href="${param[BACK_PARAM_NAME]}"><span>${i18n.edit['action.back']}</span></a></c:if>
<c:if test="${empty param.templateid}"><a class="action-button save" href="#save" onclick="jQuery('#form-meta').submit(); return false;"><span>${i18n.edit['action.update']}</span></a></c:if>
<a href="${info.absoluteURLPrefix}${currentModule.path}/jsp/upload.jsp" class="popup cboxElement action-button"><span>${i18n.edit['action.add-files']}</span></a>
<c:if test="${not empty param.templateid}">
<a class="action-button ajax" href="${info.currentURL}?webaction=template.commit&webaction=browse&templateid=${param.templateid}&from-module=template"><span>${i18n.edit['template.action.commit']}</span></a>
<a class="action-button ajax" href="${info.currentURL}?webaction=template.commitChildren&webaction=browse&templateid=${param.templateid}&from-module=template"><span>${i18n.edit['template.action.commit-children']}</span></a>
</c:if>

<input type="text" name="filter" placeholder="${i18n.edit['global.filter']}" onkeyup="filter(this.value, '#main-renderer .file');"/>
<input type="text" id="allDate" name="all-date" placeholder="${i18n.edit['action.change-all-date']}" onkeyup="jQuery('.file-date').each(function(){var t = jQuery(this);if (t.val().length == 0 || t.data('empty') == true) {t.data('empty',true);t.val(jQuery('#allDate').val())}});"/>
<input type="text" id="allLocation" name="all-location" placeholder="${i18n.edit['action.change-all-location']}" onkeyup="jQuery('.file-location').each(function(){var t = jQuery(this);if (t.val().length == 0 || t.data('empty') == true) {t.data('empty',true);t.val(jQuery('#allLocation').val())}});"/>
<input type="text" id="allDescription" name="all-descritpion" placeholder="${i18n.edit['action.change-all-description']}" onkeyup="jQuery('.file-description').each(function(){var t = jQuery(this);if (t.val().length == 0 || t.data('empty') == true) {t.data('empty',true);t.val(jQuery('#allDescription').val())}});"/>
<input type="text" id="allTitle" name="all-title" placeholder="${i18n.edit['action.change-all-title']}" onkeyup="jQuery('.file-title').each(function(){var t = jQuery(this);if (t.val().length == 0 || t.data('empty') == true) {t.data('empty',true);t.val(jQuery('#allTitle').val())}});"/>

<div class="clear">&nbsp;</div>

