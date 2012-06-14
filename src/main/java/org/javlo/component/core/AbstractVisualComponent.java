/*
 * Created on 9 oct. 2003
 */
package org.javlo.component.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.javlo.component.config.ComponentConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;
import org.javlo.context.GlobalContext;
import org.javlo.exception.RessourceNotFoundException;
import org.javlo.helper.ConfigHelper;
import org.javlo.helper.Jsp2String;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.ServletHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XHTMLHelper;
import org.javlo.helper.Comparator.StringSizeComparator;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.navigation.MenuElement;
import org.javlo.rendering.Device;
import org.javlo.service.RequestService;
import org.javlo.utils.DebugListening;
import org.javlo.utils.SufixPreffix;

/**
 * @author pvanderm
 */
public abstract class AbstractVisualComponent implements IContentVisualComponent {

	public static Logger logger = Logger.getLogger(AbstractVisualComponent.class.getName());

	public static final String COMPONENT_KEY = "wcms_component";

	public static final String I18N_FILE = "component_[lg].properties";

	private static final String CACHE_NAME = "component";

	public static final String TIME_CACHE_NAME = "component-time";

	protected static final String VALUE_SEPARATOR = "-";

	public static final String getComponentId(HttpServletRequest request) {
		return (String) request.getAttribute(COMP_ID_REQUEST_PARAM);
	}

	/**
	 * get a component in the request if there are.
	 * 
	 * @param request
	 *            the HTTP request
	 * @return a IContentVisualComponent, null if there are no composant in the request
	 */
	public static final IContentVisualComponent getRequestComponent(HttpServletRequest request) {
		IContentVisualComponent res = (IContentVisualComponent) request.getAttribute(COMPONENT_KEY);
		return res;
	}

	private final Map<String, Properties> i18nView = new HashMap<String, Properties>();

	// protected ContentContext ctx;

	protected String errorMessage = null;

	protected ComponentBean componentBean = new ComponentBean();

	// protected boolean modify = false;

	Set formatSupported = new TreeSet();

	ServletContext servletContext;

	private GenericMessage msg;

	private IContentVisualComponent nextComponent = null;

	private IContentVisualComponent previousComponent = null;

	private boolean needRefresh = false;

	private boolean visible = false;

	private boolean hidden = false;

	// private String contentCache = null;

	private final Map<String, String> replacement = new HashMap<String, String>();

	protected Properties viewData = null;

	// private Map<String, String> contentCache = new HashMap<String, String>();

	// protected TimeMap<String, String> viewTimeCache = new TimeMap<String, String>(2 * 60 * 60); // 120 minutes cache

	// protected TimeMap<String, Object> shortTimeCache = new TimeMap<String, Object>(60); // 60 sec cache

	private final Object lockContent = new Object();

	private final Object lockContentTime = new Object();

	/**
	 *
	 */
	private MenuElement page = null;

	protected String applyReplacement(String content) {
		Map<String, String> remp = getRemplacement();
		if (remp.size() > 0) {
			Collection<String> keys = remp.keySet();
			java.util.List<String> list = new LinkedList<String>();
			list.addAll(keys);
			Collections.sort(list, new StringSizeComparator());
			for (String key : keys) {
				content = StringUtils.replace(content, key, remp.get(key));
			}
		}
		return content;
	}

	@Override
	public void clearReplacement() {
		replacement.clear();
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		IContentVisualComponent res = null;
		try {
			res = this.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	protected int countLine() {
		StringReader strReader = new StringReader(getValue());
		BufferedReader bufReader = new BufferedReader(strReader);
		int countLine = 0;
		try {
			while (bufReader.readLine() != null) {
				countLine++;
			}
		} catch (IOException e) {
			// no error possible
		}
		return countLine;
	}

	@Override
	public void delete(ContentContext ctx) {
		try {
			resetViewData(ctx);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AbstractVisualComponent)) {
			return false;
		}
		AbstractVisualComponent comp = (AbstractVisualComponent) obj;

		boolean eq = comp.getComponentBean().equals(getComponentBean());

		return eq;
	}
	
	protected String executeJSP(ContentContext ctx, String jsp) throws ServletException, IOException {
		ctx.getRequest().setAttribute(COMPONENT_KEY, this);
		String url = jsp;
		if (!url.startsWith("/")) {
			url = URLHelper.createJSPComponentURL(ctx.getRequest(), jsp, getComponentPath());
		}
		logger.fine("execute jsp in '" + getType() + "' : " + url);
		return ServletHelper.executeJSP(ctx, url);
	}

	protected String executeRenderer(ContentContext ctx) throws ServletException, IOException {
		String renderer = getRenderer(ctx);
		if (renderer == null) {
			return "";
		} else {
			return executeJSP(ctx, getRenderer(ctx));
		}
	}

	@Override
	public String getArea() {
		if (componentBean.getArea() == null) {
			return "content"; // default value, needed for old DC web site
			// work correctly
		}
		return componentBean.getArea();
	}

	protected String getBaseHelpURL(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		return globalContext.getHelpURL();
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#getBean()
	 */
	@Override
	public ComponentBean getBean(ContentContext ctx) {
		ComponentBean beanCopy = new ComponentBean(componentBean.getId(), componentBean.getType(), componentBean.getValue(), componentBean.getLanguage(), componentBean.isRepeat());
		beanCopy.setList(componentBean.isList());
		beanCopy.setStyle(componentBean.getStyle());
		beanCopy.setArea(componentBean.getArea());
		beanCopy.setRenderer(componentBean.getRenderer());
		return beanCopy;
	}

	@Override
	public int getComplexityLevel() {
		return COMPLEXITY_EASY;
	}

	@Override
	public ComponentBean getComponentBean() {
		return componentBean;
	}

	protected String getComponentCSS(ServletContext application, String css) throws ServletException, IOException {
		InputStream stream = ResourceHelper.getStaticComponentRessource(application, getComponentPath(), css);

		try {
			StringWriter strWriter = new StringWriter();
			PrintWriter out = new PrintWriter(strWriter);

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = reader.readLine();
			while (line != null) {
				out.println(line);
				line = reader.readLine();
			}

			out.close();
			return strWriter.toString();
		} finally {
			ResourceHelper.closeResource(stream);
		}
	}

	@Override
	public String getComponentLabel(ContentContext ctx, String lg) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		I18nAccess i18nAccess = null;
		try {
			i18nAccess = I18nAccess.getInstance(globalContext, ctx.getRequest().getSession());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return i18nAccess.getText("content." + getType(), getType());
	}

	/**
	 * the the localisation of the JSP files in the "component" directory in webapps. normaly this localisation is the name of the component direcoty in the src.
	 * 
	 * @return a part of a path
	 */
	protected String getComponentPath() {
		return getType();
	}

	public ComponentConfig getConfig(ContentContext ctx) {
		if ((ctx == null) || (ctx.getRequest() == null) || ((ctx.getRequest().getSession() == null))) {
			return ComponentConfig.getInstance();
		}
		return ComponentConfig.getInstance(ctx, getType());
	}

	public String getContentCache(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Cache cache = globalContext.getCache(CACHE_NAME);
		Element elem = cache.get(getContentCacheKey(ctx));
		if (elem == null) {
			return null;
		}
		return (String) elem.getValue();
	}

	private String getContentCacheKey(ContentContext ctx) {
		if (ctx.getDevice() == null) { // TODO: check why this method can return "null"
			return Device.DEFAULT_DEVICE + '_' + getId();
		}
		return ctx.getDevice().getCode() + '_' + getId();
	}

	/**
	 * get the XHTML input field name for the content
	 * 
	 * @return a XHTML input field name.
	 */
	@Override
	public String getContentName() {
		return "data__" + getId();
	}

	public String getContentTimeCache(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Cache cache = globalContext.getCache(TIME_CACHE_NAME);
		Element elem = cache.get(getContentCacheKey(ctx));
		if (elem == null) {
			return null;
		}
		return (String) elem.getValue();
	}

	/**
	 * get the current page
	 * 
	 * @param componentPage
	 *            if true return the page of the component, if false return the current page (in case of repeat component)
	 * @return a page.
	 * @throws Exception
	 */
	public MenuElement getCurrentPage(ContentContext ctx, boolean componentPage) throws Exception {
		if (componentPage) {
			return getPage();
		} else {
			return ctx.getCurrentPage();
		}
	}

	protected String getCurrentRenderer(ContentContext ctx) {
		if (getBean(ctx).getRenderer() == null && getConfig(ctx).getRenderes().size() > 0) {
			return getConfig(ctx).getRenderes().keySet().iterator().next();
		} else {
			return getBean(ctx).getRenderer();
		}
	}

	public String getDebugHeader(ContentContext ctx) {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		RequestService requestService = RequestService.getInstance(ctx.getRequest());
		if (StringHelper.isTrue(requestService.getParameter("_debug", "false"))) {
			out.println("<div class=\"debug-header\"><dl>");
			out.println("<dt>id</dt>");
			out.println("<dd>" + getId() + "</dd>");
			out.println("</dl></div>");
			out.close();
		}
		return writer.toString();
	}

	/**
	 * @param context
	 */
	// public void setServletContext(ServletContext context) {
	// servletContext = context;
	// }

	@Override
	public String getEditText(ContentContext ctx, String key) {
		I18nAccess i18nAccess;
		try {
			i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			String text = i18nAccess.getComponentText(getComponentPath(), key);
			if (text == null) {
				text = "TEXT [" + key + "] NOT FOUND.";
			}
			return text;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	protected String getEditXHTMLCode(ContentContext ctx) throws Exception {
		StringBuffer finalCode = new StringBuffer();
		finalCode.append(getDebugHeader(ctx));
		finalCode.append(getSpecialInputTag());
		finalCode.append("<textarea class=\"resizable-textarea full-width\" id=\"" + getContentName() + "\" name=\"" + getContentName() + "\"");
		finalCode.append(" rows=\"" + (countLine() + 1) + "\">");
		finalCode.append(getValue());
		finalCode.append("</textarea>");

		return finalCode.toString();
	}

	public String getXHTMLConfig(ContentContext ctx) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);

		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());

		if (isRepeatable()) {
			out.println("<div class=\"line\">");
			out.println("<label for=\"repeat-" + getId() + "\">" + i18nAccess.getText("content.repeat") + "</label>");
			out.println(XHTMLHelper.getCheckbox("repeat-" + getId(), isRepeat()));
			out.println("</div>");
		}

		if (isListable()) {
			out.println("<div class=\"line\">");
			out.println("<label for=\"inlist-" + getId() + "\">" + i18nAccess.getText("component.inlist") + "</label>");
			out.println(XHTMLHelper.getCheckbox("inlist-" + getId(), isList(ctx)));
			out.println("</div>");
		}

		String[] styles = getStyleList(ctx);
		if (styles.length > 1) {
			String[] stylesLabel = getStyleLabelList(ctx);
			if (styles.length != stylesLabel.length) {
				throw new ComponentException("size of styles is'nt the same than size of styles label.");
			}
			out.println("<div class=\"line\">");
			out.println("<label for=\"style-" + getId() + "\">" + getStyleTitle(ctx) + "</label>");
			out.println(XHTMLHelper.getInputOneSelect("style-" + getId(), styles, stylesLabel, getStyle(ctx), null));
			out.println("</div>");
		}

		if (getConfig(ctx).getRenderes().size() > 0) {
			out.println("<div class=\"line\">");
			out.println("<label for=\"renderer-" + getId() + "\">" + getRendererTitle() + "</label>");
			out.println(XHTMLHelper.getInputOneSelect("style-" + getId(), getConfig(ctx).getRenderes(), getRenderer(ctx)));
			out.println("</div>");
		}

		out.close();
		return new String(outStream.toByteArray());
	}

	public String performConfig(ContentContext ctx) throws Exception {
		RequestService requestService = RequestService.getInstance(ctx.getRequest());

		boolean isRepeat = requestService.getParameter("repeat-" + getId(), null) != null;
		if (isRepeat != isRepeat()) {
			setRepeat(isRepeat);
			setModify();
		}

		/** renderer **/
		String renderer = requestService.getParameter(getInputNameRenderer(), null);
		if (renderer != null) {
			if (!renderer.equals(getRenderer(ctx))) {
				setRenderer(ctx, renderer);
				setModify();
			}
		}

		/** style **/
		String newStyle = requestService.getParameter("style-" + getId(), null);
		if (newStyle != null && !newStyle.equals(getStyle(ctx))) {
			setStyle(ctx, newStyle);
			setModify();
		}

		/** in list **/
		if (getArea().equals(ctx.getArea())) {
			boolean newInlist = requestService.getParameter("inlist-" + getId(), null) != null;
			if (newInlist != isList(ctx)) {
				setList(newInlist);
				setModify();
			}
		}

		return null;
	}

	/*
	 * public String getContentTimeCache(ContentContext ctx) { return viewTimeCache.get(getContentCacheKey(ctx)); }
	 */

	protected String getEmptyCode(ContentContext ctx) throws Exception {
		if ((ctx.getRenderMode() == ContentContext.PREVIEW_MODE)) {
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			EditContext editCtx = EditContext.getInstance(globalContext, ctx.getRequest().getSession());
			if (editCtx.isEditPreview()) {
				MenuElement currentPage = ctx.getCurrentPage();
				if (currentPage.equals(getPage())) { // not edit component
					// is repeated and
					// user is not on
					// the definition
					// page
					I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
					return ("<div " + getSpecialPreviewCssClass(ctx, "pc_empty-component") + getSpecialPreviewCssId(ctx) + ">[" + i18nAccess.getText("content." + getType()) + "]</div>");
				}
			}
		}
		return null;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String getErrorMessage(String fieldName) throws RessourceNotFoundException {
		return "no error message defined.";
	}

	/**
	 * @return
	 */
	// public ServletContext getServletContext() {
	// return servletContext;
	// }

	@Override
	public Collection<String> getExternalResources(ContentContext ctx) {
		if (!needJavaScript(ctx)) {
			return Collections.emptyList();
		} else {
			List<String> resources = new LinkedList<String>();
			// resources.add("/css/slimbox/slimbox.css");
			resources.add("/js/mootools.js");
			// resources.add("/js/slimbox.js");
			resources.add("/js/global.js");
			resources.add("/js/calendar/js/HtmlManager.js");
			resources.add("/js/calendar/js/calendarFunctions.js");
			resources.add("/js/calendar/js/calendarOptions.js");
			resources.add("/js/calendar/js/calendarTranslate_" + ctx.getContentLanguage() + ".js");
			resources.add("/js/calendar/css/style_calendar.css");
			resources.add("/js/calendar/css/style_calendarcolor.css");
			resources.add("/js/shadowbox/src/adapter/shadowbox-base.js");
			resources.add("/js/shadowbox/src/shadowbox.js");
			resources.add("/js/shadowboxOptions.js");
			resources.add("/js/onLoadFunctions.js");
			return resources;
		}

	}

	@Override
	public String getFirstPrefix(ContentContext ctx) {
		if (!componentBean.isList()) {
			return "";
		} else {
			String cssClass = "";
			if (getStyle(ctx) != null && getStyle(ctx).trim().length() > 0) {
				cssClass = ' ' + getStyle(ctx);
			}
			return "<ul class=\"" + getType() + cssClass + "\">";
		}
	}

	public String getFormName() {
		return "content_update";
	}

	@Override
	public String getHeaderContent(ContentContext ctx) {
		return null;
	}

	@Override
	public final String getHelpURL(ContentContext ctx, String lang) {
		if (ctx.getRenderMode() == ContentContext.PAGE_MODE) {
			return getBaseHelpURL(ctx) + "/page/" + lang + getHelpURI(ctx);
		} else {
			String url  = URLHelper.mergePath(getBaseHelpURL(ctx),lang,getHelpURI(ctx));
			System.out.println("*********************************************************************************************************");
			System.out.println("*********************************************************************************************************");			
			System.out.println("***** AbstractVisualComponent.getHelpURL : type="+getType()+"  help url = "+url); //TODO: remove debug trace
			System.out.println("*********************************************************************************************************");
			System.out.println("*********************************************************************************************************");
			return url;
		}

	}

	protected String getHelpURI(ContentContext ctx) {
		return "/components/" + getType() + ".html";
	}

	@Override
	public String getHexColor() {
		return DEFAULT_COLOR;
	}

	@Override
	public List<String> getI18nEditableKeys(ContentContext ctx) {
		return Collections.EMPTY_LIST;
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#getContentLanguage()
	 */
	// public String getLanguage() {
	// return ctx.getRequestContentLanguage();
	// }
	@Override
	public final String getId() {
		return componentBean.getId();
	}

	protected String getInputName(String field) {
		return field + "-" + getId();
	}

	@Override
	public String getInputNameRenderer() {
		return "_renderer_file_" + getId();
	}

	public String getInputNameRendererTitle() {
		return "_renderer_title_" + getId();
	}

	public List<SufixPreffix> getItalicAndStrongLanguageMarkerList(ContentContext ctx) {
		List<SufixPreffix> out = new LinkedList<SufixPreffix>();

		I18nAccess i18nAccess = null;
		try {
			i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SufixPreffix sufixPreffix = new SufixPreffix("<em>", "</em>", i18nAccess.getText("component.marker.italic"));
		out.add(sufixPreffix);
		sufixPreffix = new SufixPreffix("<strong>", "</strong>", i18nAccess.getText("component.marker.strong"));
		out.add(sufixPreffix);

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Collection<String> lgs = globalContext.getContentLanguages();
		for (String lg : lgs) {
			Locale locale = new Locale(lg);
			sufixPreffix = new SufixPreffix("<span lang=\"" + locale.getLanguage() + "\">", "</span>", locale.getDisplayLanguage(new Locale(ctx.getRequestContentLanguage())));
			out.add(sufixPreffix);
		}
		return out;
	}

	@Override
	public String getJSOnSubmit() {
		return "";
	}

	@Override
	public String getKey() {
		return getClass().getName();
	}

	@Override
	public String getLastSufix(ContentContext ctx) {
		if (previousComponent != null) {
			if (previousComponent.getBean(ctx).isList() && previousComponent.getType().equals(getType())) {
				return "</ul>";
			}
		}
		if (getBean(ctx).isList()) {
			return "</ul>";
		}
		return "";
	}

	@Override
	public List<SufixPreffix> getMarkerList(ContentContext ctx) {
		return null;
	}

	@Override
	public GenericMessage getMessage() {
		return msg;
	}

	@Override
	public IContentVisualComponent getNextComponent() {
		return nextComponent;
	}

	@Override
	public MenuElement getPage() {
		return page;
	}

	@Override
	public String getPrefixViewXHTMLCode(ContentContext ctx) {

		if (getConfig(ctx).getProperty("prefix", null) != null) {
			return getConfig(ctx).getProperty("prefix", null);
		}

		String style = getStyle(ctx);
		if (style != null) {
			style = style + " ";
		} else {
			style = "";
		}

		if (getPreviousComponent() == null || !getPreviousComponent().isList(ctx) || !getPreviousComponent().getType().equals(getType())) {
			style = style + " first";
		}

		if (!componentBean.isList()) {
			return "<div " + getSpecialPreviewCssClass(ctx, style + ' ' + getType()) + getSpecialPreviewCssId(ctx) + " >";
		} else {
			return "<li" + getSpecialPreviewCssClass(ctx, style + ' ' + getType()) + getSpecialPreviewCssId(ctx) + " >";
		}
	}

	@Override
	public IContentVisualComponent getPreviousComponent() {
		return previousComponent;
	}

	public List<SufixPreffix> getQuotationLanguageMarkerList(ContentContext ctx) {
		List<SufixPreffix> out = new LinkedList<SufixPreffix>();

		I18nAccess i18nAccess = null;
		try {
			i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SufixPreffix sufixPreffix = new SufixPreffix("<q>", "</q>", i18nAccess.getText("component.marker.quotation"));
		out.add(sufixPreffix);

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Collection<String> lgs = globalContext.getContentLanguages();
		for (String lg : lgs) {
			Locale locale = new Locale(lg);
			sufixPreffix = new SufixPreffix("<span lang=\"" + locale.getLanguage() + "\">", "</span>", locale.getDisplayLanguage(new Locale(ctx.getRequestContentLanguage())));
			out.add(sufixPreffix);
		}
		return out;
	}

	protected Map<String, String> getRemplacement() {
		return replacement;
	}

	public String getDefaultRenderer(ContentContext ctx) {
		return null;
	}

	@Override
	public String getRenderer(ContentContext ctx) {
		if (getConfig(ctx).getRenderes().size() == 0) {
			return getDefaultRenderer(ctx);
		} else if (getConfig(ctx).getRenderes().size() == 1 || getCurrentRenderer(ctx) == null) {
			return getConfig(ctx).getRenderes().values().iterator().next();
		} else {
			String renderer = getConfig(ctx).getRenderes().get(getCurrentRenderer(ctx));
			if (renderer != null) {
				return renderer;
			} else {
				return getConfig(ctx).getRenderes().values().iterator().next();
			}
		}
	}

	protected String getRendererTitle() {
		return null;
	}

	@Override
	public int getSearchLevel() {
		return SEARCH_LEVEL_LOW;
	}

	protected String getSelectRendererXHTML(ContentContext ctx) throws Exception, IOException {
		if (getCurrentRenderer(ctx) == null && getRenderer(ctx).length() <= 1) { // for use getChooseRendererXHTML you must implement method getSelectedRenderer and return empty string if empty and not null (default value).
			return "";
		}

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		I18nAccess i18nAccess = I18nAccess.getInstance(globalContext, ctx.getRequest().getSession());

		out.println("<fieldset class=\"display\">");
		out.println("<legend>" + i18nAccess.getText("content.page-teaser.display-type") + "</legend><div class=\"line\">");

		if (getRendererTitle() != null) { // for use title you must implement method getRendererTitle and return empty string if empty and not null (default value).
			out.println("<div class=\"line\">");
			out.println("<label for=\"" + getInputNameRendererTitle() + "\">" + i18nAccess.getText("global.title") + " : </label>");
			out.println("<input type=\"text\" id=\"" + getInputNameRendererTitle() + "\" name=\"" + getInputNameRendererTitle() + "\" value=\"" + getRendererTitle() + "\"  />");
			out.println("</div>");
		}

		out.println("<div class=\"line\">");
		/* display as slide show */

		Map<String, String> renderers = getConfig(ctx).getRenderes();
		for (Map.Entry<String, String> entry : renderers.entrySet()) {
			out.println(XHTMLHelper.getRadio(getInputNameRenderer(), entry.getKey(), getCurrentRenderer(ctx)));
			out.println("<label for=\"" + entry.getKey() + "\">" + entry.getKey() + "</label></div><div class=\"line\">");
		}

		out.println("</fieldset>");

		out.close();
		return new String(outStream.toByteArray());

	}

	/**
	 * create technical input tag. sample : for know the type of component with only the <code>request</code>
	 * 
	 * @return a part of a form in XHTML
	 */
	protected String getSpecialInputTag() {
		StringBuffer finalCode = new StringBuffer();
		finalCode.append("<input type=\"hidden\" name=\"");
		finalCode.append(getTypeInputName());
		finalCode.append("\" value=\"");
		finalCode.append(getType());
		finalCode.append("\"/>");
		return finalCode.toString();
	}

	public final String getSpecialPreviewCssClass(ContentContext ctx, String currentClass) {
		if (currentClass == null) {
			currentClass = "";
		} else {
			currentClass = ' ' + currentClass.trim();
		}
		if (ctx.getRenderMode() == ContentContext.PREVIEW_MODE) {
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			EditContext editCtx = EditContext.getInstance(globalContext, ctx.getRequest().getSession());
			if (editCtx.isEditPreview()) {
				MenuElement currentPage;
				try {
					currentPage = ctx.getCurrentPage();
					//f (currentPage.equals(getPage())) { // not edit component is
						// repeated and user is
						// not on the definition
						// page
						return " class=\"editable-component" + currentClass + "\"";
					//}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (currentClass != null && currentClass.trim().length() > 0) {
			return " class=\"" + currentClass.trim() + "\"";
		}

		return "";
	}

	public String getSpecialPreviewCssId(ContentContext ctx) {
		if (ctx.getRenderMode() == ContentContext.PREVIEW_MODE) {
			return " id=\"cp_" + getId() + "\"";
		} else {
			return "";
		}
	}

	@Override
	public final String getStyle(ContentContext ctx) {
		if (componentBean.getStyle() == null) {
			if ((getStyleList(ctx) != null) && (getStyleList(ctx).length > 0)) {
				componentBean.setStyle(getStyleList(ctx)[0]);
			}
		}
		return componentBean.getStyle();
	}

	public String getStyleLabel(ContentContext ctx) {
		String[] styles = getStyleList(ctx);
		for (int i = 0; i < styles.length; i++) {
			if (styles[i].equals(getStyle(ctx))) {
				return getStyleLabelList(ctx)[i];
			}
		}
		return "";
	}

	public void setRenderer(ContentContext ctx, String renderer) {
		componentBean.setRenderer(renderer);
	}

	@Override
	public String[] getStyleLabelList(ContentContext ctx) {
		I18nAccess i18nAccess;
		try {
			i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			String[] styleLabel = getConfig(ctx).getStyleLabelList();
			if (styleLabel.length != getStyleList(ctx).length) {
				return getStyleList(ctx);
			}
			for (int i = 0; i < styleLabel.length; i++) {
				styleLabel[i] = i18nAccess.getText(styleLabel[i]);
			}
			return styleLabel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String[] getStyleList(ContentContext ctx) {
		return getConfig(ctx).getStyleList();
	}

	@Override
	public String getStyleTitle(ContentContext ctx) {
		I18nAccess i18nAccess;
		try {
			i18nAccess = I18nAccess.getInstance(ctx.getRequest());
			String styleTitleKey = getConfig(ctx).getStyleTitle();
			if (styleTitleKey == null) {
				return "";
			} else {
				return i18nAccess.getText(styleTitleKey);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getSufixViewXHTMLCode(ContentContext ctx) {

		if (getConfig(ctx).getProperty("suffix", null) != null) {
			return getConfig(ctx).getProperty("suffix", null);
		}

		if (!componentBean.isList()) {
			return "</div>";
		} else {
			return "</li>";
		}
	}

	@Override
	public String getTextForSearch() {
		return StringEscapeUtils.unescapeHtml(getValue());
	}

	@Override
	public String getTextLabel() {
		return getValue();
	}

	@Override
	public String getTextTitle() {
		return getValue();
	}

	@Override
	public int getTitleLevel(ContentContext ctx) {
		return 0;
	}

	protected String getTypeInputName() {
		return getId() + ID_SEPARATOR + "type";
	}

	public String getValue() {
		return componentBean.getValue();
	}

	@Override
	public String getValue(ContentContext ctx) {
		return getValue();
	}

	public Properties getViewData(ContentContext ctx) throws IOException {
		if (viewData == null) {
			loadViewData(ctx);
		}
		return viewData;
	}

	protected File getViewDataFile(ContentContext ctx) throws IOException {
		return getViewDataFile(ctx, true);
	}

	private File getViewDataFile(ContentContext ctx, boolean createFile) throws IOException {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		String folder = URLHelper.mergePath(globalContext.getDataFolder(), "components_view_data");
		File viewDataFile = new File(URLHelper.mergePath(folder, getId() + ".properties"));
		if (!viewDataFile.exists() && createFile) {
			viewDataFile.getParentFile().mkdirs();
			viewDataFile.createNewFile();
		}
		return viewDataFile;
	}

	@Override
	public String getViewText(ContentContext ctx, String key) throws RessourceNotFoundException {

		Properties i18n = i18nView.get(ctx.getRequestContentLanguage());
		if (i18n == null) {
			i18n = new Properties();
			String fileName = "/i18n/" + I18N_FILE.replaceAll("\\[lg\\]", ctx.getRequestContentLanguage());
			ServletContext srvCtx = ctx.getRequest().getSession().getServletContext();
			InputStream in = null;
			try {
				in = ConfigHelper.getComponentConfigRessourceAsStream(srvCtx, getComponentPath(), fileName);
			} catch (RessourceNotFoundException e) {
				// ressource can not be found.
				e.printStackTrace();
				logger.severe(e.getMessage());
			}
			if (in != null) {
				try {
					i18n.load(in);
					i18nView.put(ctx.getRequestContentLanguage(), i18n);
				} catch (Exception e) {
					throw new RessourceNotFoundException("can not load the ressource : " + fileName);
				}
			}
		}
		String text = i18n.getProperty(key);
		if (text == null) {
			text = "TEXT [" + key + "] NOT FOUND.";
		}
		return text;
	}

	public String getViewXHTMLCode(ContentContext ctx) throws Exception {
		if (getRenderer(ctx) != null) {
			ctx.getRequest().setAttribute(COMPONENT_KEY, this);
			String url = getRenderer(ctx);
			if (!url.startsWith("/")) {
				url = URLHelper.createJSPComponentURL(ctx.getRequest(), url, getComponentPath());
			}
			logger.fine("execute view jsp in '" + getType() + "' : " + url);
			return ServletHelper.executeJSP(ctx, url);
		} else {
			return getValue();
		}
	}

	public int getWordCount(ContentContext ctx) {
		String value = getValue();
		if (value != null) {
			return value.split(" ").length;
		}
		return 0;
	}

	@Override
	public String getXHTMLCode(ContentContext ctx) {

		setNeedRefresh(false);

		try {

			if (ctx.getRenderMode() != ContentContext.EDIT_MODE) {
				processView(ctx);
			}

			if ((ctx.getRenderMode() == ContentContext.PREVIEW_MODE)) {
				GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
				EditContext editCtx = EditContext.getInstance(globalContext, ctx.getRequest().getSession());
				if (editCtx.isEditPreview() && isDefaultValue(ctx)) {
					String emptyCode = getEmptyCode(ctx);
					if (emptyCode != null) {
						return emptyCode;
					}
				}
			}
			if (ctx.getRenderMode() == ContentContext.EDIT_MODE) {
				return getEditXHTMLCode(ctx);
			} else {
				ctx.getRequest().setAttribute(COMP_ID_REQUEST_PARAM, getId());
				if (ctx.getRenderMode() == ContentContext.VIEW_MODE && isContentCachable(ctx)) {
					if (getContentCache(ctx) != null) {
						if (getContentCache(ctx) != null) {
							return getContentCache(ctx);
						}
					} else {
						synchronized (lockContent) {
							if (getContentCache(ctx) != null) {
								return getContentCache(ctx);
							}
						}
					}
				}
				if (ctx.getRenderMode() == ContentContext.VIEW_MODE && isContentTimeCachable(ctx)) {
					String timeContent = getContentTimeCache(ctx);
					if (timeContent != null) {
						return timeContent;
					} else {
						synchronized (lockContentTime) {
							timeContent = getContentTimeCache(ctx);
							if (timeContent != null) {
								return timeContent;
							}
						}
					}
				}
				if (ctx.getRenderMode() == ContentContext.VIEW_MODE && isContentCachable(ctx)) {
					logger.fine("add content in cache for component " + getType() + " in page : " + ctx.getPath());
					synchronized (lockContent) {
						long beforeTime = System.currentTimeMillis();
						prepareView(ctx);
						setContentCache(ctx, getViewXHTMLCode(ctx));
						logger.fine("render content cache '" + getType() + "' : " + (System.currentTimeMillis() - beforeTime) / 1000 + " sec.");
					}
					return getContentCache(ctx);
				} else {
					String content;
					if (isContentTimeCachable(ctx)) {
						synchronized (lockContentTime) {
							long beforeTime = System.currentTimeMillis();
							prepareView(ctx);
							content = getViewXHTMLCode(ctx);
							logger.fine("render content time cache '" + getType() + "' : " + (System.currentTimeMillis() - beforeTime) / 1000 + " sec.");
							setContentTimeCache(ctx, content);
						}
					} else {
						prepareView(ctx);
						content = getViewXHTMLCode(ctx);
					}
					return content;
				}

			}

		} catch (Exception e) {
			DebugListening.getInstance().sendError(ctx.getRequest(), e, "error in component : " + getType());
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * prepare the rendering of a component.
	 * default attributes put in request : style, value, type, compid
	 * @param ctx
	 * @throws Exception
	 */
	public void prepareView(ContentContext ctx) throws Exception {
		ctx.getRequest().setAttribute("style", getStyle(ctx));
		ctx.getRequest().setAttribute("value", getValue());
		ctx.getRequest().setAttribute("type", getType());
		ctx.getRequest().setAttribute("compid", getId());
		if (isValueProperties()) {
			Properties p = new Properties();
			p.load(new StringReader(getValue()));
			ctx.getRequest().setAttribute("properties", p);
		}
	}

	protected void includeComponentJSP(ContentContext ctx, String jsp) throws ServletException, IOException {
		try {
			ctx.getRequest().setAttribute(COMPONENT_KEY, this);
			String url = URLHelper.createJSPComponentURL(ctx.getRequest(), jsp, getComponentPath());
			logger.info("include jsp in '" + getType() + "' : " + url);
			includePage(ctx, url);
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		}
	}

	protected String getDisplayAsInputName() {
		return "display-as-" + getId();
	}

	private String getDisplayType() {
		String[] values = getValue().split(VALUE_SEPARATOR);
		String out = null;
		if (values.length >= 5) {
			out = values[4];
			if (out.isEmpty()) {
				out = null;
			}
		}
		return out;
	}

	protected void renderRendererSelection(ContentContext ctx, PrintStream out) throws Exception {
		Map<String, String> renderers = getConfig(ctx).getRenderes();
		I18nAccess i18nAccess = I18nAccess.getInstance(ctx.getRequest());
		if (renderers.size() > 1) {
			out.println("<fieldset class=\"display\">");
			out.println("<legend>" + i18nAccess.getText("content.choose-renderer") + "</legend><div class=\"line\">");

			out.println("<div class=\"line\">");
			for (Map.Entry<String, String> entry : renderers.entrySet()) {
				out.println(XHTMLHelper.getRadio(getDisplayAsInputName(), entry.getKey(), getDisplayType()));
				out.println("<label for=\"" + entry.getKey() + "\">" + entry.getKey() + "</label></div>");
			}
			out.println("</fieldset>");

		}
	}

	protected void includePage(ContentContext ctx, String jsp) throws ServletException, IOException {
		// ctx.getResponse().flushBuffer();
		try {
			ctx.getRequest().getRequestDispatcher(jsp).include(ctx.getRequest(), ctx.getResponse());
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	protected void init() throws RessourceNotFoundException {
		// loadViewData();
		msg = null;
	}

	protected void init(ComponentBean bean, ContentContext ctx) throws Exception {
		assert bean != null;
		setComponentBean(bean);
		init();
	}

	/**
	 * insert a text in the component
	 * 
	 * @param text
	 *            the text to be insered
	 */
	@Override
	public void insert(String text) {
		setValue(text);
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public boolean isContentCachable(ContentContext ctx) {
		return false;
	}

	public boolean isContentTimeCachable(ContentContext ctx) {
		return false;
	}

	@Override
	public boolean isDefaultValue(ContentContext ctx) {
		return isEmpty(ctx);
	}

	@Override
	public boolean isEmpty(ContentContext ctx) {
		return getValue().trim().length() == 0;
	}

	/**
	 * you can check if it is possible to extract the selection of the component in a other component.
	 * 
	 * @return true if content is extractable
	 */
	@Override
	public boolean isExtractable() {
		return false;
	}

	protected boolean isFirstElementOfRepeatSequence(ContentContext ctx) throws Exception {
		MenuElement currentPage = ctx.getCurrentPage();
		if (isRepeat() && currentPage.equals(getPage())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isFirstRepeated() {

		IContentVisualComponent previousComp = getPreviousComponent();

		while (previousComp != null && previousComp.getArea().equals(getArea())) {
			if (!previousComp.isRepeat()) {
				return false;

			}
			previousComp = previousComp.getPreviousComponent();
		}

		return true;
	}

	@Override
	public boolean isHidden(ContentContext ctx) {
		return hidden;
	}

	@Override
	public boolean isInline() {
		return false;
	}

	/**
	 * you can insert a text in this component
	 * 
	 * @return true if a text is insertable
	 */
	@Override
	public boolean isInsertable() {
		return true;
	}

	@Override
	public boolean isLabel() {
		return false;
	}

	@Override
	public boolean isList(ContentContext ctx) {
		return componentBean.isList();
	}

	@Override
	public boolean isListable() {
		return false;
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#isModify()
	 */
	@Override
	public boolean isModify() {
		return componentBean.isModify();
	}

	@Override
	public boolean isNeedRefresh() {
		return needRefresh;
	}

	@Override
	public boolean isRealContent(ContentContext ctx) {
		return false;
	}

	/**
	 * @return Returns the repeat.
	 */
	@Override
	public boolean isRepeat() {
		return componentBean.isRepeat();
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	protected boolean isViewDataFile(ContentContext ctx) throws IOException {
		return getViewDataFile(ctx, false).exists();
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	/**
	 * default : visible only in LARGE format.
	 */
	@Override
	public boolean isVisible(ContentContext ctx) {
		return true;
	}

	@Override
	public synchronized void loadViewData(ContentContext ctx) throws IOException {
		if (viewData == null) {
			viewData = new Properties();
		}
		if (isViewDataFile(ctx)) {
			InputStream in;
			try {
				in = new FileInputStream(getViewDataFile(ctx));
				Properties newViewData = new Properties();
				newViewData.load(in);
				in.close();
				viewData = newViewData;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected boolean needJavaScript(ContentContext ctx) {
		return false;
	}

	@Override
	public IContentVisualComponent newInstance(ComponentBean bean, ContentContext newCtx) throws Exception {
		AbstractVisualComponent res = (AbstractVisualComponent) this.clone();
		res.init(bean, newCtx);
		return res;
	}

	@Override
	public IContentVisualComponent next() {
		return nextComponent;
	}

	protected void onStyleChange(ContentContext ctx) {
	}

	@Override
	public IContentVisualComponent previous() {
		return previousComponent;
	}

	/**
	 * prepare the rendering of the page.
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	protected String processView(ContentContext ctx) throws Exception {
		ctx.getRequest().setAttribute("comp", getComponentBean());
		ctx.getRequest().setAttribute("config", getConfig(ctx));
		ctx.getRequest().setAttribute("style", getStyle(ctx));
		// ctx.getRequest().setAttribute("viewXHTML", getViewXHTMLCode(ctx));
		return null;
	}

	@Override
	public void performEdit(ContentContext ctx) throws Exception {
		RequestService requestService = RequestService.getInstance(ctx.getRequest());
		String newContent = requestService.getParameter(getContentName(), null);
		System.out.println("***** AbstractVisualComponent.performEdit : newContent = "+newContent); //TODO: remove debug trace
		System.out.println("***** AbstractVisualComponent.performEdit : 1.componentBean.getValue() = "+componentBean.getValue()); //TODO: remove debug trace
		if (newContent != null) {
			if (!componentBean.getValue().equals(newContent)) {
				componentBean.setValue(newContent);
				setModify();
			}
		}
		System.out.println("***** AbstractVisualComponent.performEdit : 2.componentBean.getValue() = "+componentBean.getValue()); //TODO: remove debug trace
	}

	public final void refresh(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ContentContext ctx = ContentContext.getContentContext(request, response);
		performEdit(ctx);
	}

	@Override
	public void replaceAllInContent(Map<String, String> replacement) {
		if (replacement != null) {
			this.replacement.putAll(replacement);
		}
	}

	@Override
	public void replaceInContent(String source, String target) {
		getRemplacement().put(source, target);
	}

	public void resetContentCache(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		globalContext.getCache(CACHE_NAME).removeAll();
	}

	@Override
	public void resetViewData(ContentContext ctx) throws IOException {
		if (isViewDataFile(ctx)) {
			try {
				getViewDataFile(ctx).delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setComponentBean(ComponentBean componentBean) {
		this.componentBean = componentBean;
	}

	public void setContentCache(ContentContext ctx, String contentCache) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Cache cache = globalContext.getCache(CACHE_NAME);
		cache.put(new Element(getContentCacheKey(ctx), contentCache));
	}

	public void setContentTimeCache(ContentContext ctx, String contentCache) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		Cache cache = globalContext.getCache(TIME_CACHE_NAME);
		cache.put(new Element(getContentCacheKey(ctx), contentCache));
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public void setList(boolean inList) {
		componentBean.setList(inList);
	}

	public void setMessage(GenericMessage inMsg) {
		msg = inMsg;
	}

	public void setModify() {
		componentBean.setModify(true);
	}

	@Override
	public void setNeedRefresh(boolean needRefresh) {
		this.needRefresh = needRefresh;
	}

	@Override
	public void setNextComponent(IContentVisualComponent nextComponent) {
		this.nextComponent = nextComponent;
	}

	public void setPage(MenuElement inPage) {
		page = inPage;
	}

	@Override
	public void setPreviousComponent(IContentVisualComponent previousComponent) {
		this.previousComponent = previousComponent;
	}

	@Override
	public void setRepeat(boolean newRepeat) {
		if (newRepeat == componentBean.isRepeat()) {
			return;
		} else {
			componentBean.setRepeat(newRepeat);
			setModify();
			setNeedRefresh(true);
		}
	}

	@Override
	public void setStyle(ContentContext ctx, String inStyle) {
		boolean styleModify = false;
		if ((getStyle(ctx) != null) && (!getStyle(ctx).equals(inStyle))) {
			styleModify = true;
		}
		if (getStyle(ctx) == null && inStyle != null) {
			styleModify = true;
		}
		if (styleModify) {
			componentBean.setStyle(inStyle);
			setModify();
			onStyleChange(ctx);
		}
	}

	@Override
	public void setValid(boolean inVisible) {
		visible = inVisible;
	}

	@Override
	public void setValue(String inContent) {
		componentBean.setValue(StringHelper.escapeWordChar(inContent));
	}

	/**
	 * @see org.javlo.itf.IContentVisualComponent#stored()
	 */
	@Override
	public void stored() {
		componentBean.setModify(false);
	}

	public void storeViewData(ContentContext ctx) throws IOException {
		ResourceHelper.writePropertiesToFile(getViewData(ctx), getViewDataFile(ctx), "view-data storage");
	}

	public InputStream stringToStream(String str) {
		return new ByteArrayInputStream(str.getBytes());
	}

	/**
	 * @deprecated use XHTMLHelper.textToXHTML(String text)
	 * @param text
	 * @return
	 */
	@Deprecated
	public String textToXHTML(String text) {
		String res = XHTMLHelper.autoLink(text);
		res = res.replaceAll("\n", "<br />\n");
		return res.replaceAll("  ", "&nbsp;&nbsp;");
	}

	@Override
	public boolean isMetaTitle() {
		return false;
	}
	
	public String getClassName() {
		return getClass().getName();
	}
	
	/**
	 * if content of the component is a list of properties (key=value) this method must return true.  If this method return true prepare method will add a mal called "properties" in request attrivute and this map can be used in renderer (jsp).
	 * @return true if content is a list of properties.
	 */
	public boolean isValueProperties() {
		return false;
	}

	// generate compilation error : use for refactoring

	/*
	 * protected final boolean isDefaultValue() {return false;} protected final String getHelpURL(String lang) {return null;} //TODO: remove after refactoring protected final boolean isEmpty() {return false;}//TODO: remove after refactoring; protected final boolean isVisible (int format ){return false;}//TODO: remove after refactoring; protected final List<SufixPreffix> getMarkerList() {return null;}//TODO: remove after refactoring; protected final String getSufixViewXHTMLCode() {return null;}//TODO: remove after refactoring; protected final String getPrefixViewXHTMLCode() {return null;}//TODO: remove after refactoring; protected final boolean isHidden(){return false;}//TODO: remove after refactoring; protected final String getFirstPrefix(){return null;}//TODO: remove after refactoring; protected final String getViewXHTMLCode() throws Exception {return null;}//TODO: remove after refactoring; protected final String getEditXHTMLCode() throws Exception {return null;}//TODO: remove after
	 * refactoring; protected final boolean needJavaScript(){return false;}//TODO: remove after refactoring; protected final String[] getStyleLabelList() {return null;}//TODO: remove after refactoring; protected final String getImageLinkTitle() {return null;}//TODO: remove after refactoring; protected final String[] getStyleList() {return null;}//TODO: remove after refactoring; protected final String getStyleTitle() {return null;}//TODO: remove after refactoring; protected final boolean isList(){return false;}//TODO: remove after refactoring; protected final boolean isContentCachable(){return false;}//TODO: remove after refactoring; protected final String getLastSufix() {return null;}//TODO: remove after refactoring; protected final String getCSSClassName() {return null;}//TODO: remove after refactoring; protected final Collection<String> getExternalResources() {return null;}//TODO: remove after refactoring; protected final int getTitleLevel() {return 1;}//TODO: remove after
	 * refactoring; protected final boolean isImageValid() {return false;}//TODO: remove after refactoring; protected final String getHeaderContent(){return null;}//TODO: remove after refactoring; protected final String getImageUploadTitle(){return null;}//TODO: remove after refactoring; protected final String getImageChangeTitle() {return null;}//TODO: remove after refactoring; protected final String getDeleteTitle(){return null;}//TODO: remove after refactoring; protected final String createFileURL(String inURL){return null;}//TODO: remove after refactoring; protected final String getFileDirectory(){return null;}//TODO: remove after refactoring;
	 */

}