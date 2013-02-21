package org.javlo.template;

import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.javlo.component.core.ComponentBean;
import org.javlo.config.StaticConfig;
import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.filter.PropertiesFilter;
import org.javlo.helper.ResourceHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XMLManipulationHelper;
import org.javlo.helper.XMLManipulationHelper.BadXMLException;
import org.javlo.i18n.I18nAccess;
import org.javlo.message.GenericMessage;
import org.javlo.navigation.DefaultTemplate;
import org.javlo.remote.IRemoteResource;
import org.javlo.rendering.Device;
import org.javlo.service.exception.ServiceException;
import org.javlo.servlet.zip.ZipManagement;

public class Template implements Comparable<Template> {

	public static final String DEFAULT_TEMPLATE_FOLDER = "default";

	public static final String FORCE_TEMPLATE_PARAM_NAME = "force-template";

	private static class TemplateComparator implements Comparator<Template> {

		@Override
		public int compare(Template o1, Template o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	public static class TemplateData {
		public static final TemplateData EMPTY = new TemplateData();
		private Color background = null;
		private Color foreground = null;
		private Color text = null;
		private Color backgroundMenu = null;
		private Color textMenu = null;
		private Color border = null;
		private Color link = null;
		private String toolsServer = null;
		private String logo = null;

		public TemplateData() {
		};

		public TemplateData(String rawData) {
			String[] data = rawData.split(";");
			try {
				if (data.length > 7) {
					int i = 0;
					if (data[i].length() > 0) {
						setBackground(Color.decode('#' + data[i]));
					}
					i++;
					if (data[i].length() > 0) {
						setForeground(Color.decode('#' + data[i]));
					}
					i++;
					if (data[i].length() > 0) {
						setText(Color.decode('#' + data[i]));
					}
					i++;
					if (data[i].length() > 0) {
						setBackgroundMenu(Color.decode('#' + data[i]));
					}
					i++;
					if (data[i].length() > 0) {
						setTextMenu(Color.decode('#' + data[i]));
					}
					i++;
					if (data[i].length() > 0) {
						setBorder(Color.decode('#' + data[i]));
					}
					if (data.length > 8) {
						i++;
						if (data[i].length() > 0) {
							setLink(Color.decode('#' + data[i]));
						}
					}
					i++;
					if (data[i].length() > 0) {
						setToolsServer(data[i]);
					}
					i++;
					if (data[i].length() > 0) {
						setLogo(data[i]);
					}
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

		}

		public Color getBackground() {
			return background;
		}

		public Color getBackgroundMenu() {
			return backgroundMenu;
		}

		public Color getBorder() {
			return border;
		}

		public Color getForeground() {
			return foreground;
		}

		public Color getLink() {
			return link;
		}

		public String getLogo() {
			return logo;
		}

		public Color getText() {
			return text;
		}

		public Color getTextMenu() {
			return textMenu;
		}

		public String getToolsServer() {
			return toolsServer;
		}

		public void setBackground(Color background) {
			this.background = background;
		}

		public void setBackgroundMenu(Color backgroundMenu) {
			this.backgroundMenu = backgroundMenu;
		}

		public void setBorder(Color border) {
			this.border = border;
		}

		public void setForeground(Color foreGround) {
			foreground = foreGround;
		}

		public void setLink(Color link) {
			this.link = link;
		}

		public void setLogo(String logo) {
			this.logo = logo;
		}

		public void setText(Color text) {
			this.text = text;
		}

		public void setTextMenu(Color textMenu) {
			this.textMenu = textMenu;
		}

		public void setToolsServer(String toolsServer) {
			this.toolsServer = toolsServer;
		}

		@Override
		public String toString() {
			StringBuffer out = new StringBuffer();
			out.append(StringHelper.colorToHexStringNotNull(getBackground()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getForeground()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getText()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getBackgroundMenu()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getTextMenu()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getBorder()));
			out.append(';');
			out.append(StringHelper.colorToHexStringNotNull(getLink()));
			out.append(';');
			out.append(getToolsServer());
			out.append(';');
			out.append(getLogo());
			return out.toString();
		}

	}

	public static final class TemplateBean implements IRemoteResource {
		String name;
		String previewURL;
		String viewURL;
		String HTMLURL;
		String HTMLFile;
		String creationDate;
		String downloadURL;
		List<String> ids;
		Map<String, String> areaMap;
		List<String> areas;
		boolean valid;
		String imageURL;
		String url;
		String authors;
		String description;
		String licence;
		Date date;
		String id = StringHelper.getRandomId();
		boolean mailing;
		String category;
		String version;
		String deployId = StringHelper.getRandomId();
		String type;

		public TemplateBean() {
		};

		public TemplateBean(ContentContext ctx, Template template) throws Exception {
			StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());

			name = template.getName();
			previewURL = URLHelper.createTransformStaticTemplateURL(ctx, template, "template", template.getVisualFile());
			viewURL = URLHelper.createTransformStaticTemplateURL(ctx, template, "template_view", template.getVisualFile());
			HTMLURL = URLHelper.createStaticTemplateURL(ctx, template, template.getHTMLFile(null));
			HTMLFile = template.getHTMLFile(ctx.getDevice());
			creationDate = StringHelper.renderDate(template.getCreationDate(), staticConfig.getDefaultDateFormat());
			downloadURL = "/folder/template/" + template.getName() + ".zip";

			ContentContext remoteCtx = ctx.getContextForAbsoluteURL();
			downloadURL = URLHelper.createStaticURL(remoteCtx, downloadURL);
			ids = template.getHTMLIDS();
			Collections.sort(ids);
			areas = template.getAreas();
			Collections.sort(areas);
			areaMap = template.getAreasMap();
			valid = template.isValid();
			try {
				imageURL = URLHelper.createTransformStaticTemplateURL(remoteCtx, template, "template", template.getVisualFile()) + "?deployId=" + template.getDeployId();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				url = URLHelper.createStaticTemplateURL(remoteCtx, template, template.getHTMLFile(null));
			} catch (Exception e) {
				e.printStackTrace();
			}
			name = template.getName();
			authors = template.getAuthors();
			description = template.getDescription(ctx.getLanguage());
			licence = template.getLicenceFile();
			date = template.getCreationDate();
			version = template.getVersion();
			deployId = template.getDeployId();
			type = IRemoteResource.TYPE_TEMPLATE;
			category = staticConfig.getMarketServerName();
		}

		public String getPreviewUrl() throws Exception {
			return previewURL;
		}

		public String getViewUrl() throws Exception {
			return viewURL;
		}

		public String getHtmlUrl() throws Exception {
			return HTMLURL;
		}

		public String getHtmlFile() {
			return HTMLFile;
		}

		public String getCreationDate() {
			return creationDate;
		}

		@Override
		public String getDownloadURL() {
			return downloadURL;
		}

		public Collection<String> getHTMLIDS() {

			return ids;
		}

		public Collection<String> getAreas() {
			return areas;
		}

		public Map<String, String> getAreasMap() {
			return areaMap;
		}

		public boolean isValid() {
			return valid;
		}

		public boolean isMailing() {
			return mailing;
		}

		public String getDeployId() {
			return deployId;
		};

		@Override
		public String getImageURL() {
			return imageURL;
		}

		@Override
		public String getURL() {
			return url;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getAuthors() {
			return authors;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getLicence() {
			return licence;
		}

		@Override
		public Date getDate() {
			return date;
		}

		@Override
		public void setDownloadURL(String url) {
			this.downloadURL = url;
		}

		@Override
		public void setImageURL(String url) {
			this.imageURL = url;
		}

		@Override
		public void setURL(String url) {
			this.url = url;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public void setAuthors(String authors) {
			this.authors = authors;
		}

		@Override
		public void setDescription(String description) {
			this.description = description;
		}

		@Override
		public void setLicence(String licence) {
			this.licence = licence;
		}

		@Override
		public void setDate(Date date) {
			this.date = date;
		}

		@Override
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public void setCategory(String category) {
			this.category = category;
		}

		@Override
		public String getDateAsString() {
			return StringHelper.renderDate(getDate());
		}

		@Override
		public String getVersion() {
			return version;
		}

		@Override
		public void setVersion(String version) {
			this.version = version;
		}
	}

	public static class TemplateDateComparator implements Comparator<Template> {

		public static final TemplateDateComparator instance = new TemplateDateComparator();

		@Override
		public int compare(Template o1, Template o2) {
			return o2.getCreationDate().compareTo(o1.getCreationDate());
		}

	}

	private static class WEBFileFilter implements FileFilter {

		private boolean accept = false;
		private boolean jsp = true;
		private boolean copyVisualFile = false;
		private Template template = null;

		public WEBFileFilter(Template inTemplate, boolean inAccept, boolean inJSP, boolean inCopyVisualFile) {
			accept = inAccept;
			jsp = inJSP;
			copyVisualFile = inCopyVisualFile;
			template = inTemplate;
		}

		@Override
		public boolean accept(File file) {
			if (!copyVisualFile && file.getName().endsWith(template.getVisualFile())) {
				return false;
			}
			String ext = FilenameUtils.getExtension(file.getName());
			if ((ext.equalsIgnoreCase("html") && jsp) || ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("css") || ext.equalsIgnoreCase("htm") || ext.equalsIgnoreCase("js")) {
				return accept;
			} else {
				return !accept;
			}
		}

	}

	public static final String __NO_CONTEXT = "__no_context";

	public static final TemplateComparator TEMPLATE_COMPARATOR = new TemplateComparator();

	/**
	 * create a static logger.
	 */
	protected static Logger logger = java.util.logging.Logger.getLogger(Template.class.getName());

	private static final String AREA_FORCE_DISPLAY_PREFIX = "area.forcedisplay.";

	private static final String MAIL_FOLDER = "mail";

	public static final String CONFIG_FILE = "config.properties";

	private static final String PRIVATE_CONFIG_FILE = "private-config.properties";

	private static final String LINK_EMAIL_FILE = "link_email_[lg].txt";

	private static final String DYNAMIC_COMPONENTS_PROPERTIES_FOLDER = "components";

	private static final String CONFIG_COMPONENTS_PROPERTIES_FOLDER = "components-config";

	private static final String I18N_FILE = "view_";

	private static final String MACRO_FOLDER = "macro";

	public static final String EDIT_TEMPLATE_CODE = "[edit]";

	private static final String RESOURCES_DIR = "resources";

	public static final String PLUGIN_FOLDER = "plugins";

	public static final String GZ_FILE_EXT = "httpgz";

	public static void main(String[] args) {
		TemplateData data = new TemplateData("FFFFFF;000000;787878;787878;FFFFFF;55AA55;5555AA;http://localhost:8080;logo.png");
		System.out.println("**** background : " + data.getBackground());
		System.out.println("**** link color : " + data.getLink());
	}

	private final PropertiesConfiguration properties = new PropertiesConfiguration();

	private final PropertiesConfiguration privateProperties = new PropertiesConfiguration();

	private File dir = null;

	protected StaticConfig config;

	private final String buildId = StringHelper.getRandomId();

	private boolean jsp = false;

	private static Template emptyTemplate = null;

	private String deployId = StringHelper.getRandomId();

	private List<Properties> dynamicsComponents = null;

	private final Set<String> contextWithTemplateImported = new HashSet<String>();

	public static Template getApplicationInstance(ServletContext application, ContentContext ctx, String templateDir) throws ConfigurationException, IOException {

		Template outTemplate = null;
		if (templateDir == null) {
			// logger.severe("templateDir is null");
			return null;
		}

		outTemplate = TemplateFactory.getTemplates(application).get(templateDir);
		if (outTemplate == null) {
			return getInstance(StaticConfig.getInstance(application), ctx, templateDir);
		}

		if (!outTemplate.isTemplateInWebapp(ctx)) {
			outTemplate.importTemplateInWebapp(StaticConfig.getInstance(application), ctx);
		}

		outTemplate.parent = outTemplate.getParent(StaticConfig.getInstance(application), ctx);
		if (outTemplate.parent == null) { // parent must be never null
			outTemplate.parent = DefaultTemplate.INSTANCE;
		}

		if (ctx != null) {
			if (outTemplate.isAlternativeTemplate(ctx)) {
				outTemplate = outTemplate.getAlternativeTemplate(StaticConfig.getInstance(application), ctx);
			}
		}
		return outTemplate;
	}

	public static Template getInstance(StaticConfig config, ContentContext ctx, String templateDir) throws ConfigurationException, IOException {
		return getInstance(config, ctx, templateDir, true);
	}

	private static Template getInstance(StaticConfig config, ContentContext ctx, String templateDir, boolean alternativeTemplate) throws ConfigurationException, IOException {
		if ((templateDir == null) || templateDir.trim().length() == 0) {
			return DefaultTemplate.INSTANCE;
		}
		Template template = new Template();
		String templateFolder = URLHelper.mergePath(config.getTemplateFolder(), templateDir);

		template.dir = new File(templateFolder);
		template.config = config;

		if (!template.isTemplateInWebapp(ctx)) {
			template.importTemplateInWebapp(config, ctx);
		}

		File configFile = new File(URLHelper.mergePath(templateFolder, CONFIG_FILE));
		File privateConfigFile = new File(URLHelper.mergePath(templateFolder, PRIVATE_CONFIG_FILE));

		try {
			if (!configFile.exists()) {
				configFile.createNewFile();
			}
			template.properties.setFile(configFile);
			template.properties.load();

			if (!privateConfigFile.exists()) {
				privateConfigFile.createNewFile();
			}
			template.privateProperties.setFile(privateConfigFile);
			template.privateProperties.load();

			template.jsp = config.isTemplateJSP();

		} catch (Throwable t) { // if default template directory not exist
			// TODO Auto-generated catch block
			// logger.warning("problem with file : " +
			// configFile.getAbsolutePath());
			// t.printStackTrace();
		}

		template.parent = template.getParent(config, ctx);
		if (template.parent == null) { // parent must be never null
			template.parent = DefaultTemplate.INSTANCE;
		}

		if (alternativeTemplate && ctx != null) {
			if (template.isAlternativeTemplate(ctx)) {
				Template altTemplate = template.getAlternativeTemplate(config, ctx);
				return altTemplate;
			}
		}

		return template;
	}

	public static Template getMailingInstance(StaticConfig config, ContentContext ctx, String templateDir) throws ConfigurationException, IOException {
		return getInstance(config, ctx, templateDir, true);
	}

	private Template parent = null;

	private boolean templateImportationError = false;

	/**
	 * check the structure of the template.
	 * 
	 * @return the error message, null if no error.
	 * @throws IOException
	 * @throws BadXMLException
	 */
	public List<GenericMessage> checkRenderer(GlobalContext globalContext, I18nAccess i18nAccess) throws IOException, BadXMLException {
		String templateFolder = config.getTemplateFolder();

		File HTMLFile = new File(URLHelper.mergePath(URLHelper.mergePath(templateFolder, getSourceFolder()), getHTMLFile(null)));

		List<GenericMessage> messages = new LinkedList<GenericMessage>();
		List<String> resources = new LinkedList<String>();

		try {
			TemplatePluginFactory templatePluginFactory = TemplatePluginFactory.getInstance(globalContext.getServletContext());
			XMLManipulationHelper.convertHTMLtoTemplate(globalContext, i18nAccess, HTMLFile, null, getMap(), getAreas(), resources, templatePluginFactory.getAllTemplatePlugin(globalContext.getTemplatePlugin()), messages);
		} catch (Throwable t) {
			messages.add(new GenericMessage(t.getMessage(), GenericMessage.ERROR));
		}

		if (getParentName() != null) { // parent is valid >> template is valid //TODO: ameliorated this test.
			messages = Collections.EMPTY_LIST;
		}

		return messages;
	}

	public void clearRenderer(ContentContext ctx) {
		String templateFolder = config.getTemplateFolder();
		File templateSrc = new File(URLHelper.mergePath(templateFolder, getSourceFolder()));
		if (templateSrc.exists()) {
			try {
				FileUtils.deleteDirectory(new File(URLHelper.mergePath(getWorkTemplateFolder(), getSourceFolder())));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			importTemplateInWebapp(config.getInstance(ctx.getRequest().getSession().getServletContext()), ctx);
		} catch (IOException e) {
			e.printStackTrace();
		}
		dynamicsComponents = null;
		templateImportationError = false;
		contextWithTemplateImported.clear();
	}

	public void delete() {
		try {
			FileUtils.deleteDirectory(new File(getTemplateRealPath()));
			FileUtils.deleteDirectory(new File(URLHelper.mergePath(getWorkTemplateFolder(), getSourceFolder())));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void disabledAlternativeTemplate(ContentContext ctx) {
		ctx.getRequest().getSession().removeAttribute(getAlternateTemplateSessionKey());
	}

	/*
	 * public String getDefaultHTMLFile() { return properties.getString("html", "index.html"); }
	 */

	public void enabledAlternativeTemplate(ContentContext ctx) {
		if (getAlternativeTemplateName() != null) {
			ctx.getRequest().getSession().setAttribute(getAlternateTemplateSessionKey(), new Object());
		}
	}

	public boolean exist() {
		return dir != null;
	}

	private String getAlternateTemplateSessionKey() {
		return "_alternate_template";
	}

	private Template getAlternativeTemplate(StaticConfig config, ContentContext ctx) throws IOException, ConfigurationException {
		Template aTemplate = this;
		String alternativeTemplate = getAlternativeTemplateName();
		if (alternativeTemplate != null) {
			aTemplate = Template.getInstance(config, ctx, alternativeTemplate, false);
		}
		return aTemplate;
	}

	private String getAlternativeTemplateName() {
		return properties.getString("template.alternative", null);
	}

	@SuppressWarnings("unchecked")
	public List<String> getAreas() {
		List<String> areas = new LinkedList<String>();
		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.startsWith(XMLManipulationHelper.AREA_PREFIX) && StringUtils.countMatches(key, ".") < 2) {
				areas.add(key.substring(XMLManipulationHelper.AREA_PREFIX.length()));
			}
		}
		if (areas.size() == 0) {
			if (getParent() == null) {
				areas.add(ComponentBean.DEFAULT_AREA);
			} else {
				return getParent().getAreas();
			}
		}
		return areas;
	}

	public Set<String> getComponentsIncludeForArea(String area) {
		String key = XMLManipulationHelper.AREA_PREFIX + area + ".components.include";
		String typeRAW = properties.getString(key);
		if (typeRAW == null) {
			return null;
		} else {
			return new HashSet(StringHelper.stringToCollection(typeRAW, ";"));
		}
	}

	public Set<String> getComponentsExcludeForArea(String area) {
		String key = XMLManipulationHelper.AREA_PREFIX + '.' + "components.exclude";
		String typeRAW = properties.getString(key);
		if (typeRAW == null) {
			return null;
		} else {
			return new HashSet(StringHelper.stringToCollection(typeRAW, ";"));
		}
	}

	public Map<String, String> getAreasMap() {
		Map<String, String> areas = new HashMap<String, String>();
		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.startsWith(XMLManipulationHelper.AREA_PREFIX) && !key.endsWith(".navigation")) {
				areas.put(key.substring(XMLManipulationHelper.AREA_PREFIX.length()), properties.getString(key));
			}
		}
		if (areas.size() == 0) {
			if (getParent() != null) {
				return getParent().getAreasMap();
			}
		}
		return areas;
	}

	public void setArea(String area, String id) {
		properties.setProperty(XMLManipulationHelper.AREA_PREFIX + area, id);
		try {
			properties.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void deleteArea(String area) {
		properties.clearProperty(XMLManipulationHelper.AREA_PREFIX + area);
		try {
			properties.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * this area is display if specialrendere is defined
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getAreasForceDisplay() {
		List<String> areas = new LinkedList<String>();
		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.startsWith(AREA_FORCE_DISPLAY_PREFIX)) {
				String area = key.substring(AREA_FORCE_DISPLAY_PREFIX.length());
				if (StringHelper.isTrue(properties.getString(key))) {
					areas.add(area);
				}
			}
		}
		if (areas.size() == 0) {
			if (getParent() != null) {
				return getParent().getAreasForceDisplay();
			}
		}
		return areas;
	}

	public String getAuthors() {
		return properties.getString("authors", getParent().getAuthors());
	}

	public Collection<String> getCategories() {
		String categoriesRaw = properties.getString("categories");
		if (categoriesRaw == null) {
			return Collections.emptyList();
		}
		return StringHelper.stringToCollection(categoriesRaw, ",");
	}

	protected List<File> getComponentFile(GlobalContext globalContext) throws IOException {
		String templateFolder = getWorkTemplateFolder();

		String path = URLHelper.mergePath(URLHelper.mergePath(templateFolder, getFolder(globalContext)), DYNAMIC_COMPONENTS_PROPERTIES_FOLDER);
		File dynCompDir = new File(path);
		if (!dynCompDir.exists()) {
			return Collections.emptyList();
		}
		File[] propertiesFile = dynCompDir.listFiles(new PropertiesFilter());
		return Arrays.asList(propertiesFile);
	}

	public Properties getConfigComponentFile(GlobalContext globalContext, String type) throws IOException {
		String templateFolder = getWorkTemplateFolder();
		String path = URLHelper.mergePath(URLHelper.mergePath(templateFolder, getFolder(globalContext)), CONFIG_COMPONENTS_PROPERTIES_FOLDER, type + ".properties");
		File configFile = new File(path);
		if (configFile.exists()) {
			return ResourceHelper.loadProperties(configFile);
		}
		return null;
	}

	public Date getCreationDate() {
		String creationDateString = privateProperties.getString("creation-date", null);
		if (creationDateString == null) {
			setCreationDate(new Date());
			creationDateString = privateProperties.getString("creation-date", StringHelper.renderDate(new Date()));
		}
		try {
			return StringHelper.parseDate(creationDateString);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getDepth() {
		return properties.getInt("depth", getParent().getDepth());
	}

	public String getDescription(String lang) {
		if (lang != null) {
			return properties.getString("description." + lang, "");
		} else {
			return properties.getString("description", "");
		}
	}

	public String getDominantColor() {
		return properties.getString("color.dominant", getParent().getDominantColor());
	}

	public final List<Properties> getDynamicComponentsProperties(GlobalContext globalContext) throws IOException {
		if (dynamicsComponents == null) {
			List<File> files = getComponentFile(globalContext);
			List<Properties> outProperties = new LinkedList<Properties>();
			for (File file : files) {
				Properties prop = new Properties();
				InputStream in = new FileInputStream(file);
				Reader inReader = new InputStreamReader(in, ContentContext.CHARACTER_ENCODING);
				try {
					prop.load(inReader);
				} finally {
					ResourceHelper.closeResource(in);
					ResourceHelper.closeResource(inReader);
				}
				outProperties.add(prop);
			}
			dynamicsComponents = outProperties;
		}
		return dynamicsComponents;
	}

	public List<String> getEmailLinkFileList() {
		Locale[] locales = Locale.getAvailableLocales();
		List<String> outEmailLinkFileList = new ArrayList<String>();
		for (Locale locale : locales) {
			String file = getLinkEmailFileName(locale.getLanguage());
			if (isLinkEmail(locale.getLanguage())) {
				if (!outEmailLinkFileList.contains(file)) {
					outEmailLinkFileList.add(file);
				}
			}
		}
		outEmailLinkFileList.addAll(getParent().getEmailLinkFileList());
		return outEmailLinkFileList;
	}

	/**
	 * template can be change in some context, call this method for obtain the final Template to be use for rendering.
	 * 
	 * @param ctx
	 * @return final template for rendering.
	 * @throws ConfigurationException
	 * @throws IOException
	 */
	public Template getFinalTemplate(ContentContext ctx) throws ConfigurationException, IOException {
		if (ctx != null) {
			if (ctx.getRenderMode() == ContentContext.VIEW_MODE || ctx.getRenderMode() == ContentContext.PREVIEW_MODE) {
				if (isAlternativeTemplate(ctx)) {
					Template altTemplate = getAlternativeTemplate(StaticConfig.getInstance(ctx.getRequest().getSession().getServletContext()), ctx);
					if (altTemplate != null) {
						return altTemplate;
					}
				}
			}
		}
		return this;
	}

	public String getFolder(GlobalContext globalContext) {
		if (dir == null) {
			return null;
		} else {
			String siteFolder = __NO_CONTEXT;
			if (globalContext != null) {
				siteFolder = globalContext.getContextKey();
			}
			return dir.getName() + '/' + siteFolder;
		}
	}

	public String getHomeRenderer(GlobalContext globalContext) {
		if (getHTMLHomeFile() == null) {
			return null;
		}
		String renderer = properties.getString("home-renderer", "home.jsp");
		File jspFile = new File(getTemplateTargetFolder(globalContext), renderer);
		if (!jspFile.exists()) {
			File HTMLFile = new File(URLHelper.mergePath(getTemplateTargetFolder(globalContext), getHTMLHomeFile()));
			logger.warning(jspFile + " not found, try to generate from " + HTMLFile);
			if (!HTMLFile.exists()) {
				logger.warning(HTMLFile + " not found.");
			}
			try {
				List<String> resources = new LinkedList<String>();
				TemplatePluginFactory templatePluginFactory = TemplatePluginFactory.getInstance(globalContext.getServletContext());
				int depth = XMLManipulationHelper.convertHTMLtoTemplate(globalContext, HTMLFile, jspFile, getMap(), getAreas(), resources, templatePluginFactory.getAllTemplatePlugin(globalContext.getTemplatePlugin()), null, false);
				setDepth(depth);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return renderer;
	}

	public String getVersion() {
		return properties.getString("version", "?");
	}

	public String getHomeRendererFullName(GlobalContext globalContext) {
		if (dir == null) {
			logger.warning("no valid dir : " + dir);
			return null;
		}
		return URLHelper.mergePath(getLocalTemplateTargetFolder(globalContext), getHomeRenderer(globalContext));
	}

	public String getHTMLFile(Device device) {
		String defaultRenderer = properties.getString("html", getParent().getHTMLFile(device));
		String deviceRenderer = null;
		if (device != null) {
			deviceRenderer = properties.getString("html." + device.getCode(), defaultRenderer);
		}
		if (deviceRenderer != null) {
			logger.info("device renderer found : " + deviceRenderer);
			return deviceRenderer;
		} else {
			return defaultRenderer;
		}
	}

	public String getHTMLHomeFile() {
		return properties.getString("home", getParent().getHTMLHomeFile());
	}

	public Properties getI18nProperties(GlobalContext globalContext, Locale locale) throws IOException {
		File i18nFile = new File(URLHelper.mergePath(URLHelper.mergePath(getWorkTemplateFolder(), getFolder(globalContext)), I18N_FILE + locale.getLanguage() + ".properties"));
		Properties prop = new Properties();
		if (i18nFile.exists()) {
			Reader reader = new FileReader(i18nFile);
			prop.load(reader);
			reader.close();
		}
		return prop;
	}

	public String getId() {
		if (dir == null) {
			return null;
		} else {
			return dir.getName();
		}
	}

	public PropertiesConfiguration getImageConfig() {
		File templateImageConfigFile = new File(URLHelper.mergePath(getTemplateRealPath(), getImageConfigFile()));
		if (templateImageConfigFile.exists()) {
			try {
				PropertiesConfiguration templateProperties = new PropertiesConfiguration();
				templateProperties.load(templateImageConfigFile);
				if (isParent()) {
					PropertiesConfiguration templatePropertiesParent = getParent().getImageConfig();
					if (templatePropertiesParent != null) {
						Iterator keys = templatePropertiesParent.getKeys();
						while (keys.hasNext()) {
							String key = (String) keys.next();
							try {
								if (templateProperties.getProperty(key) == null) {
									if (templatePropertiesParent.getProperty(key) != null && templatePropertiesParent.getProperty(key).toString().trim().length() > 0) {
										templateProperties.setProperty(key, templatePropertiesParent.getProperty(key));
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				return templateProperties;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (isParent()) {
			return getParent().getImageConfig();
		}
		return null;
	}

	public String getImageConfigFile() {
		return properties.getString("image-config", getParent().getImageConfigFile());
	}

	public List<String> getImageFilters() {
		String filterRaw = getImageFiltersRAW();
		if (filterRaw == null) {
			return Collections.emptyList();
		}
		List<String> outFilter = new LinkedList<String>(Arrays.asList(StringHelper.stringToArray(filterRaw, ";")));
		return outFilter;
	}

	protected String getImageFiltersRAW() {
		return properties.getString("images-filter", getParent().getImageFiltersRAW());
	}

	public String getLastSelectedClass() {
		return properties.getString("class.selected.last", getParent().getLastSelectedClass());
	}

	public String getLicenceFile() {
		return properties.getString("licence", getParent().getLicenceFile());
	}

	public File getLinkEmail(String lg) {
		String templateFolder = config.getTemplateFolder();
		File linkEmailFile = new File(URLHelper.mergePath(URLHelper.mergePath(templateFolder, getSourceFolder()), getLinkEmailFileName(lg)));
		return linkEmailFile;

	}

	public String getLinkEmailFileName(String lg) {
		String linkEmailFile = properties.getString("link-email", LINK_EMAIL_FILE).replace("[lg]", lg);
		return linkEmailFile;
	}

	private String getLocalTemplateTargetFolder(GlobalContext globalContext) {
		String templateTgt = URLHelper.mergePath(getLocalWorkTemplateFolder(), getFolder(globalContext));
		return templateTgt;
	}

	public String getLocalWorkMailingTemplateFolder() {
		return properties.getString("work-mailing-folder", "/work_mailing_template");
	}

	public String getLocalWorkTemplateFolder() {
		return properties.getString("work-folder", "/work_template");
	}

	public final File getMacroFile(GlobalContext globalContext, String fileName) throws IOException {
		String templateFolder = getWorkTemplateFolder();
		String path = URLHelper.mergePath(URLHelper.mergePath(templateFolder, getFolder(globalContext)), MACRO_FOLDER, fileName);
		File macroFile = new File(path);
		return macroFile;
	}

	private final List<File> getMacroFile(GlobalContext globalContext) throws IOException {
		String templateFolder = getWorkTemplateFolder();

		String path = URLHelper.mergePath(URLHelper.mergePath(templateFolder, getFolder(globalContext)), MACRO_FOLDER);
		File dynCompDir = new File(path);
		if (!dynCompDir.exists()) {
			return Collections.emptyList();
		}
		File[] propertiesFile = dynCompDir.listFiles(new PropertiesFilter());
		return Arrays.asList(propertiesFile);
	}

	public Properties getMacroProperties(GlobalContext globalContext, String macroKey) throws IOException {
		List<File> macroFiles = getMacroFile(globalContext);
		for (File pFile : macroFiles) {
			if (pFile.getName().equals(macroKey + ".properties")) {
				Properties prop = new Properties();
				InputStream in = new FileInputStream(pFile);
				try {
					prop.load(in);
				} finally {
					ResourceHelper.closeResource(in);
				}
				return prop;
			}
		}
		return null;
	}

	public String getMailContent(ContentContext ctx, String mailName) throws IOException {
		String folder = URLHelper.mergePath(getTemplateRealPath(), MAIL_FOLDER);
		File htmlFile = new File(URLHelper.mergePath(folder, mailName + '-' + ctx.getContentLanguage() + ".html"));
		if (!htmlFile.exists()) {
			logger.warning("html file not found : " + htmlFile);
			return null;
		} else {
			return FileUtils.readFileToString(htmlFile, ContentContext.CHARACTER_ENCODING);
		}
	}

	public File getMailJsp(ContentContext ctx, String mailName) throws Exception {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		File htmlFile = new File(URLHelper.mergePath(getFolder(globalContext), MAIL_FOLDER) + mailName + '-' + ctx.getContentLanguage() + ".html");
		File jspFile = new File(URLHelper.mergePath(getFolder(globalContext), MAIL_FOLDER) + mailName + '-' + ctx.getContentLanguage() + ".jsp");
		if (!htmlFile.exists()) {
			return null;
		} else {
			if (jspFile.exists()) {
				return jspFile;
			} else {
				XMLManipulationHelper.convertHTMLtoMail(htmlFile, jspFile);
				return jspFile;
			}
		}
	}

	public String getMailSubject(String lg) {
		return properties.getString("mail.subject." + lg, "");
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getMap() {
		Map<String, String> out = new HashMap<String, String>();
		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			out.put(key, properties.getString(key));
		}

		if (getParent() != null) {
			for (Map.Entry<String, String> parentEntry : getParent().getMap().entrySet()) {
				if (!out.containsKey(parentEntry.getKey())) {
					out.put(parentEntry.getKey(), parentEntry.getValue());
				}
			}
		}

		return out;
	}

	public String getName() {
		if (dir == null) {
			return "";
		} else {
			return properties.getString("name", dir.getName());
		}
	}

	public String getOwner() {
		return privateProperties.getString("owner", getParent().getOwner());
	}

	public Template getParent() {
		if (parent == null || parent.getName().equals(getName())) {
			return DefaultTemplate.INSTANCE;
		}
		return parent;
	}

	private Template getParent(StaticConfig config, ContentContext ctx) throws IOException, ConfigurationException {
		Template parent = null;
		String parentId = getParentName();
		if (parentId != null && !parentId.equals(getName())) {
			if (ctx == null) {
				parent = Template.getInstance(config, ctx, parentId, false);
			} else {
				parent = TemplateFactory.getTemplates(ctx.getRequest().getSession().getServletContext()).get(parentId);
			}
		}
		return parent;
	}

	private String getParentName() {
		return properties.getString("parent", null);
	}

	public synchronized String getRenderer(ContentContext ctx) throws IOException, BadXMLException {

		String renderer = getRendererFile(ctx.getDevice());

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());

		String jspPath = URLHelper.mergePath(getTemplateTargetFolder(globalContext), renderer);
		File jspFile = new File(jspPath);

		if (!jspFile.exists()) {
			importTemplateInWebapp(globalContext.getStaticConfig(), ctx);
			File HTMLFile = new File(URLHelper.mergePath(getTemplateTargetFolder(globalContext), getHTMLFile(ctx.getDevice())));
			logger.info(jspFile + " not found, try to generate from " + HTMLFile);
			if (!HTMLFile.exists()) {
				logger.warning(HTMLFile + " not found.");
			}
			List<String> resources = new LinkedList<String>();
			TemplatePluginFactory templatePluginFactory = TemplatePluginFactory.getInstance(globalContext.getServletContext());
			List<String> ids = new LinkedList<String>();
			int depth = XMLManipulationHelper.convertHTMLtoTemplate(globalContext, HTMLFile, jspFile, getMap(), getAreas(), resources, templatePluginFactory.getAllTemplatePlugin(globalContext.getTemplatePlugin()), ids, isMailing());
			setHTMLIDS(ids);
			setDepth(depth);
		}
		return renderer;
	}

	public synchronized String getRenderer(ContentContext ctx, String file) throws Exception {
		String renderer = properties.getString("renderer." + file, getParent().getRenderer(ctx, file));
		if (renderer == null) {
			return null;
		}
		return URLHelper.createStaticTemplateURLWithoutContext(ctx, this, renderer);
	}

	protected String getRSSRendererFile() {
		String renderer = properties.getString("renderer.rss", getParent().getRSSRendererFile());
		return renderer;
	}

	public String getRSSRendererFullName(ContentContext ctx) throws ServiceException {
		GlobalContext globalContext = null;
		if (ctx != null) {
			globalContext = GlobalContext.getInstance(ctx.getRequest());
		}
		if (dir == null) {
			logger.warning("no valid dir : " + dir);
			return null;
		}
		String renderer = null;
		try {
			renderer = getRSSRendererFile();
		} catch (Exception e) {
			throw new ServiceException(e.getMessage());
		}
		if (renderer == null) {
			return null;
		}
		return URLHelper.mergePath(getLocalTemplateTargetFolder(globalContext), renderer);
	}

	protected String getRendererFile(Device device) {
		String renderer = properties.getString("renderer", getParent().getRendererFile(device));
		if (device != null && !device.isDefault()) {
			renderer = StringHelper.addSufixToFileName(renderer, '-' + device.getCode());
		}
		return renderer;
	}

	public String getRendererFullName(ContentContext ctx) throws ServiceException {
		GlobalContext globalContext = null;
		if (ctx != null) {
			globalContext = GlobalContext.getInstance(ctx.getRequest());
		}
		if (dir == null) {
			logger.warning("no valid dir : " + dir);
			return null;
		}
		String renderer = null;
		try {
			renderer = getRenderer(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (renderer == null) {
			throw new ServiceException("renderer not found on template : " + getName() + " (parent:" + getParent() + ")");
		}
		return URLHelper.mergePath(getLocalTemplateTargetFolder(globalContext), renderer);
	}

	public List<String> getResources() {
		List<String> outResources = new ArrayList<String>();
		Collection<File> allFiles = FileUtils.listFiles(dir, null, true);
		for (File file : allFiles) {
			if (!file.getName().equals(PRIVATE_CONFIG_FILE) && !file.getName().endsWith("~")) {
				outResources.add(file.getAbsolutePath().replace(dir.getAbsolutePath(), ""));
			}
		}
		return outResources;
	}

	/**
	 * get resources define in the template.
	 * 
	 * @return
	 */
	public Collection<File> getResources(ContentContext ctx) {
		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
		File resourcesDir = new File(URLHelper.mergePath(URLHelper.mergePath(getWorkTemplateFolder(), getFolder(globalContext)), RESOURCES_DIR));
		if (!resourcesDir.exists()) {
			return Collections.EMPTY_LIST;
		} else {
			return ResourceHelper.getAllFilesList(resourcesDir);
		}
	}

	public String getRssCSS() {
		return properties.getString("rss.css", null);
	}

	public String getSearchFormID() {
		return properties.getString("tagid.form.search", getParent().getSearchFormID());
	}

	public String getSearchRenderer(ContentContext ctx) throws Exception {
		String renderer = properties.getString("renderer.search", null);
		Template parent = getParent();
		if (renderer == null && parent != null) {
			return parent.getSearchRenderer(ctx);
		}
		return URLHelper.createStaticTemplateURLWithoutContext(ctx, ctx.getCurrentTemplate(), renderer); // not this ?
	}

	public String getSelectedClass() {
		return properties.getString("class.selected", getParent().getSelectedClass());
	}

	public String getSource() {
		return properties.getString("source", getParent().getSource());
	}

	public String getSourceFolder() {
		if (dir == null) {
			return null;
		} else {
			return dir.getName();
		}
	}

	public String getSpecialRendererTemplate() {
		return properties.getString("special-renderer.template", null);
	}

	public TemplateData getTemplateData() {
		TemplateData templateData = new TemplateData();
		String background = properties.getString("data.color.background", null);
		if (background != null) {
			Color backgroundColor = Color.decode('#' + background);
			templateData.setBackground(backgroundColor);
		}
		String foreground = properties.getString("data.color.foreground", null);
		if (foreground != null) {
			Color foregroundColor = Color.decode('#' + foreground);
			templateData.setForeground(foregroundColor);
		}
		String text = properties.getString("data.color.text", null);
		if (text != null) {
			Color color = Color.decode('#' + text);
			templateData.setText(color);
		}
		String border = properties.getString("data.color.border", null);
		if (border != null) {
			Color color = Color.decode('#' + border);
			templateData.setBorder(color);
		}
		String textMenu = properties.getString("data.color.menu.text", null);
		if (textMenu != null) {
			Color color = Color.decode('#' + textMenu);
			templateData.setTextMenu(color);
		}
		String backgroundMenu = properties.getString("data.color.menu.background", null);
		if (backgroundMenu != null) {
			Color color = Color.decode('#' + backgroundMenu);
			templateData.setBackgroundMenu(color);
		}
		String link = properties.getString("data.color.link", null);
		if (link != null) {
			Color color = Color.decode('#' + link);
			templateData.setLink(color);
		}
		String toolsURL = properties.getString("data.server.tools", null);
		if (toolsURL != null) {
			templateData.setToolsServer(toolsURL);
		}
		String logo = properties.getString("data.logo", null);
		if (logo != null) {
			templateData.setLogo(logo);
		}

		return templateData;
	}

	private Map<String, String> getTemplateDataMap(GlobalContext globalContext) {
		if (globalContext == null) {
			return Collections.EMPTY_MAP;
		}
		TemplateData templateDataUser = globalContext.getTemplateData();
		Map<String, String> templateDataMap = new HashMap<String, String>();
		TemplateData templateData = getTemplateData();
		if (templateData.getBackground() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getBackground()), StringHelper.colorToHexStringNotNull(templateDataUser.getBackground()));
		}
		if (templateData.getForeground() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getForeground()), StringHelper.colorToHexStringNotNull(templateDataUser.getForeground()));
		}
		if (templateData.getText() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getText()), StringHelper.colorToHexStringNotNull(templateDataUser.getText()));
		}
		if (templateData.getBackgroundMenu() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getBackgroundMenu()), StringHelper.colorToHexStringNotNull(templateDataUser.getBackgroundMenu()));
		}
		if (templateData.getTextMenu() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getTextMenu()), StringHelper.colorToHexStringNotNull(templateDataUser.getTextMenu()));
		}
		if (templateData.getLink() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getLink()), StringHelper.colorToHexStringNotNull(templateDataUser.getLink()));
		}
		if (templateData.getBorder() != null) {
			templateDataMap.put(StringHelper.colorToHexStringNotNull(templateData.getBorder()), StringHelper.colorToHexStringNotNull(templateDataUser.getBorder()));
		}
		if (templateData.getToolsServer() != null) {
			templateDataMap.put(templateData.getToolsServer(), templateDataUser.getToolsServer());
		}
		return templateDataMap;
	}

	public String getTemplateRealPath() {
		if (dir == null) {
			return null;
		}
		String templateFolder = config.getTemplateFolder();
		return URLHelper.mergePath(templateFolder, getSourceFolder());
	}

	private String getTemplateTargetFolder(GlobalContext globalContext) {
		String templateTgt = URLHelper.mergePath(getWorkTemplateFolder(), getFolder(globalContext));
		return templateTgt;
	}

	public String getUnSelectedClass() {
		return properties.getString("class.unselected", getParent().getUnSelectedClass());
	}

	public String getVisualFile() {
		return properties.getString("file.visual", getParent().getVisualFile());
	}

	public String getVisualPDFile() {
		return properties.getString("file.pdf", getParent().getVisualPDFile());
	}

	public String getWorkMailingTemplateFolder() {
		return config.getRealPath(getLocalWorkMailingTemplateFolder());
	}

	public String getWorkTemplateFolder() {
		return config.getRealPath(getLocalWorkTemplateFolder());
	}

	public void importTemplateInWebapp(StaticConfig config, ContentContext ctx) throws IOException {
		if (templateImportationError) {
			return;
		}
		GlobalContext globalContext = null;
		if (ctx != null) {
			globalContext = GlobalContext.getInstance(ctx.getRequest());
		}
		String templateFolder = config.getTemplateFolder();
		File templateSrc = new File(URLHelper.mergePath(templateFolder, getSourceFolder()));
		if (templateSrc.exists()) {

			File templateTgt = new File(getTemplateTargetFolder(globalContext));

			logger.info("copy template from '" + templateSrc + "' to '" + templateTgt + "'");

			FileUtils.deleteDirectory(templateTgt);
			importTemplateInWebapp(config, ctx, globalContext, templateTgt);
		} else {
			logger.severe("folder not found : " + templateSrc);
			templateImportationError = true;
		}
	}

	protected void importTemplateInWebapp(StaticConfig config, ContentContext ctx, GlobalContext globalContext, File templateTarget) throws IOException {
		if (isParent()) {
			getParent().importTemplateInWebapp(config, ctx, globalContext, templateTarget);
		}
		String templateFolder = config.getTemplateFolder();
		File templateSrc = new File(URLHelper.mergePath(templateFolder, getSourceFolder()));
		if (templateSrc.exists()) {
			logger.info("copy parent template from '" + templateSrc + "' to '" + templateTarget + "'");
			FileUtils.copyDirectory(templateSrc, templateTarget, new WEBFileFilter(this, false, jsp, true), false);
			/** filter html and css **/
			Iterator<File> files = FileUtils.iterateFiles(templateSrc, new String[] { "html", "htm", "jsp", "js", "css" }, true);

			/** plugins **/
			if (globalContext != null) {
				Collection<String> currentPlugin = globalContext.getTemplatePlugin();
				if (currentPlugin.size() > 0) {
					TemplatePluginFactory templatePluginFactory = TemplatePluginFactory.getInstance(ctx.getRequest().getSession().getServletContext());
					for (String pluginId : currentPlugin) {
						TemplatePlugin plugin = templatePluginFactory.getTemplatePlugin(pluginId);
						if (plugin != null) {
							plugin.importInTemplate(ctx, templateTarget);
						}
					}
				}
			}

			while (files.hasNext()) {
				File file = files.next();
				File targetFile = new File(file.getAbsolutePath().replace(templateSrc.getAbsolutePath(), templateTarget.getAbsolutePath()));
				Map<String, String> map = getTemplateDataMap(globalContext);
				if (ctx != null) {
					if (globalContext != null && globalContext.getTemplateData() != null) {
						String newLogo = globalContext.getTemplateData().getLogo();
						if (newLogo != null) {
							StaticConfig staticConfig = StaticConfig.getInstance(ctx.getRequest().getSession());
							ContentContext absoluteURLCtx = new ContentContext(ctx);
							absoluteURLCtx.setAbsoluteURL(true);
							String newLogoURL;
							try {
								String templateName = getName();
								newLogoURL = URLHelper.createTransformURL(absoluteURLCtx, null, URLHelper.mergePath(staticConfig.getStaticFolder(), newLogo), "logo", templateName);

							} catch (Exception e) {
								throw new IOException(e);
							}
							String srcLogo = getTemplateData().getLogo();
							if (srcLogo != null) {
								map.put(srcLogo, newLogoURL);
							}
						}
					} else {
						logger.warning("no template data for : " + this);
					}

					if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("jsp")) {
						ResourceHelper.filteredFileCopyEscapeScriplet(file, targetFile, map);
					} else {
						ResourceHelper.filteredFileCopy(file, targetFile, map);
						if (targetFile.getName().toLowerCase().endsWith(".css") || targetFile.getName().toLowerCase().endsWith(".js")) {
							File gzTargetFile = new File(targetFile.getAbsoluteFile().getAbsolutePath() + "." + GZ_FILE_EXT);
							ZipManagement.gzipFile(gzTargetFile, targetFile);
						}
					}
				}
			}
		}
		deployId = StringHelper.getRandomId();
		if (config != null) {
			TemplateFactory.clearTemplate(config.getServletContext());
		}

	}

	public boolean isAlternativeTemplate(ContentContext ctx) {
		boolean out = ctx.getRequest().getSession().getAttribute(getAlternateTemplateSessionKey()) != null;
		return out;
	}

	public boolean isHTML() {
		String htmlFile = getHTMLFile(null);

		String templateFolder = config.getTemplateFolder();
		File indexFile = new File(URLHelper.mergePath(URLHelper.mergePath(templateFolder, getSourceFolder()), htmlFile));
		return indexFile.exists();
	}

	public boolean isLinkEmail(String lg) {
		return getLinkEmail(lg).exists();
	}

	public boolean isMailing() {
		return properties.getBoolean("mailing", false);
	}

	public boolean isNavigationArea(String area) {
		if (area == null) {
			return false;
		}
		String key = XMLManipulationHelper.AREA_PREFIX + area + ".navigation";
		return properties.getBoolean(key, false);
	}

	protected boolean isParent() {
		return getParent() != null && !getParent().getName().equals(DefaultTemplate.NAME);
	}

	public boolean isPDFFile() {
		String pdfFilStr = getVisualPDFile();

		String templateFolder = config.getTemplateFolder();
		File pdfFile = new File(URLHelper.mergePath(URLHelper.mergePath(templateFolder, getSourceFolder()), pdfFilStr));
		return pdfFile.exists();
	}

	public boolean isReady() {
		return privateProperties.getBoolean("ready", false);
	}

	public boolean isRenderer(GlobalContext globalContext) {
		String renderer = getRendererFile(null);
		File jspFile = new File(getTemplateTargetFolder(globalContext), renderer);
		return jspFile.exists();
	}

	public boolean isSubjectLocked() {
		return StringHelper.isTrue(properties.getString("mail.subject.locked", "false"));
	}

	public boolean isTemplateInWebapp(ContentContext ctx) throws IOException {
		GlobalContext globalContext = null;
		if (ctx != null) {
			globalContext = GlobalContext.getInstance(ctx.getRequest());
			if (contextWithTemplateImported.contains(globalContext.getContextKey())) {
				return true;
			}
		}
		File templateTgt = new File(URLHelper.mergePath(getWorkTemplateFolder(), getFolder(globalContext)));
		if (templateTgt.exists() && globalContext != null) {
			contextWithTemplateImported.add(globalContext.getContextKey());
		}
		return templateTgt.exists();

	}

	public boolean isValid() {
		return privateProperties.getBoolean("valid", false);
	}

	public void reload() {
		getParent().reload();
		synchronized (properties) {
			properties.clear();
			privateProperties.clear();
			try {
				properties.load();
				privateProperties.load();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
		dynamicsComponents = null;
		contextWithTemplateImported.clear();
	}

	public void setAuthors(String name) {
		synchronized (properties) {
			properties.setProperty("authors", name);
			try {
				properties.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setCreationDate(Date date) {
		if (date != null) {
			synchronized (properties) {
				privateProperties.setProperty("creation-date", StringHelper.renderDate(date));
				try {
					privateProperties.save();
				} catch (ConfigurationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setDepth(int depth) {
		privateProperties.setProperty("depth", depth);
		try {
			privateProperties.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void setDominantColor(String color) {
		synchronized (properties) {
			properties.setProperty("color.dominant", color);
			try {
				properties.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setOwner(String owner) {
		privateProperties.setProperty("owner", owner);
		try {
			privateProperties.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void setReady(boolean ready) {
		synchronized (properties) {
			privateProperties.setProperty("ready", ready);
			try {
				privateProperties.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setSource(String name) {
		synchronized (properties) {
			properties.setProperty("source", name);
			try {
				properties.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	public void setValid(boolean inValid) {
		synchronized (properties) {
			privateProperties.setProperty("valid", inValid);
			try {
				privateProperties.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	public boolean visibleForRoles(Collection<String> inRoles) {
		String rolesRaw = properties.getString("roles");
		if (rolesRaw == null) { // if no role defined -> visible for everybody
			return true;
		}
		Collection<String> roles = StringHelper.stringToCollection(rolesRaw, ",");
		for (String inRole : inRoles) {
			if (roles.contains(inRole.trim())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return a code of build. This code change if some element will modifified.
	 * 
	 * @return
	 */
	public String getBuildId() {
		return buildId;
	}

	@Override
	public int compareTo(Template template) {
		return getName().compareTo(template.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Template) {
			return getName().equals(((Template) obj).getName());
		} else {
			return super.equals(obj);
		}
	}

	public void setHTMLIDS(Collection<String> ids) {
		privateProperties.setProperty("html.ids", StringHelper.collectionToString(ids, ","));
		try {
			privateProperties.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public List<String> getHTMLIDS() {
		String htmlIds = privateProperties.getString("html.ids");
		if (htmlIds == null) {
			return Collections.EMPTY_LIST;
		}
		List<String> ids = StringHelper.stringToCollection(htmlIds, ",");
		return ids;
	}

	public String getDeployId() {
		return deployId;
	}

	/**
	 * return true if this template contains a renderer for PDF.
	 * 
	 * @return true if PDF renderer defined.
	 */
	public boolean isPDFRenderer() {
		return properties.getString("html.pdf") != null;
	}

	/**
	 * return true if resources can be compressed by CMS (css, js).
	 * 
	 * @return true by default or the value of "resources.compress" property.
	 */
	public boolean isCompressResources() {
		return properties.getBoolean("resources.compress", false);
	}

	public String getRSSImageURL() {
		return properties.getString("rss.image-url", null);
	}

	public String getImportFolder() {
		return properties.getString("import", "/import");
	}

	public String getImportImageFolder() {
		return properties.getString("import.image", "/images/import");
	}

	public String getImportGalleryFolder() {
		return properties.getString("import.gallery", "/gallery/import");
	}

	public String getImportResourceFolder() {
		return properties.getString("import.resource", "/files/import");
	}

	public String getConfigItem(String key, String defaultValue) {
		return StringHelper.neverNull(properties.getString(key), defaultValue);
	}

}
