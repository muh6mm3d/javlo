package org.javlo.service.shared.url;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.javlo.helper.NetHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.helper.XMLManipulationHelper;
import org.javlo.helper.XMLManipulationHelper.TagDescription;
import org.javlo.service.shared.AbstractSharedContentProvider;
import org.javlo.service.shared.SharedContent;

public class URLImageSharedContentProvider extends AbstractSharedContentProvider {

	private static Logger logger = Logger.getLogger(URLImageSharedContentProvider.class.getName());

	private Collection<SharedContent> content = null;

	public URLImageSharedContentProvider(URL url) {
		setName(url.getHost());
		setURL(url);
	}

	@Override
	public void refresh() {
		content = null;
	}

	@Override
	public Collection<SharedContent> getContent() {
		if (content == null) {
			content = new LinkedList<SharedContent>();
			try {
				String html = NetHelper.readPage(getURL());
				TagDescription[] tags = XMLManipulationHelper.searchAllTag(html, false);
				
				String urlPrefix = getURL().toString();
				if (urlPrefix.contains("/")) {
					urlPrefix = urlPrefix.substring(0, urlPrefix.lastIndexOf('/'));
				}

				String id = null;
				String imageURL = null;
				String imagePreviewURL = null;
				String imageTitle = null;				
				for (TagDescription tag : tags) {
					TagDescription parent = XMLManipulationHelper.searchParent(tags, tag);
					if (tag.getName().toLowerCase().equals("img") && parent.getName().toLowerCase().equals("a")) {
						String href = parent.getAttribute("href", "");
						String src = tag.getAttribute("src", "");
						if (StringHelper.isImage(href) && StringHelper.isImage(src)) {
							if (StringHelper.isURL(href)) {
								imageURL = href;
							} else {
								imageURL = URLHelper.mergePath(urlPrefix, href);
							}
							if (StringHelper.isURL(src)) {
								imagePreviewURL = src;
							} else {
								imagePreviewURL = URLHelper.mergePath(urlPrefix, src);
							}
							imageTitle = tag.getAttribute("alt", parent.getAttribute("title", StringHelper.getFileNameWithoutExtension(StringHelper.getFileNameFromPath(imageURL))));
							id = StringHelper.getFileNameFromPath(imagePreviewURL);							
							if (imageURL != null && imagePreviewURL != null) {
								URLSharedContent sharedContent = new URLSharedContent(id, null);
								sharedContent.setImageUrl(imagePreviewURL);
								sharedContent.setTitle(imageTitle);
								sharedContent.setRemoteImageUrl(imageURL);
								content.add(sharedContent);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return content;
	}

	public static void main(String[] args) {
		try {
			URL url = new URL("http://teenkasia.com/hosted/030_red_and_black_schoolgirl/2312040");
			URLImageSharedContentProvider provider = new URLImageSharedContentProvider(url);
			for (SharedContent content : provider.getContent()) {
				System.out.println(content.getImageURL());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getType() {	
		return TYPE_IMAGE;
	}
}
