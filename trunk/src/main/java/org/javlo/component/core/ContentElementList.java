/*
 * Created on 20 ao?t 2003
 */
package org.javlo.component.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.javlo.component.container.RepeatContainer;
import org.javlo.component.title.MenuTitle;
import org.javlo.component.title.PageTitle;
import org.javlo.component.title.SubTitle;
import org.javlo.context.ContentContext;
import org.javlo.navigation.MenuElement;
import org.javlo.service.ContentService;

/**
 * @author pvanderm
 */
public class ContentElementList implements IContentComponentsList {

	private final Set<String> addedElementId = new HashSet<String>();

	private final Set<String> repeatAddedElementId = new HashSet<String>();

	private LinkedList<IContentVisualComponent> contentElements = new LinkedList<IContentVisualComponent>();

	private final LinkedList<IContentVisualComponent> repeatContentElements = new LinkedList<IContentVisualComponent>();

	private int pos = 0;

	// private String area;

	private boolean allArea = false; // browse only the content of the current area.

	private MenuElement page;

	private String language;

	public ContentElementList() {
	};

	public ContentElementList(ComponentBean[] beans, ContentContext ctx, MenuElement inPage, boolean allArea) throws Exception {

		language = ctx.getRequestContentLanguage();

		// area = ctx.getArea();
		this.allArea = allArea;
		page = inPage;

		ContentService content = ContentService.getInstance(ctx.getRequest());

		IContentVisualComponent previousComponent = null;
		for (ComponentBean bean : beans) {
			// Logger.startCount("load component");
			assert bean != null;
			assert bean.getLanguage() != null;
			assert ctx != null;

			String lg = ctx.getRequestContentLanguage();

			if (bean.getLanguage().equals(lg)) {
				// Logger.stepCount("load component", "load comp step 1");
				IContentVisualComponent comp = content.getCachedComponent(ctx, bean.getId());
				// Logger.stepCount("load component", "load comp step 2");
				// IContentVisualComponent comp = null;

				if (comp == null /* || comp.getComponentBean() != bean */) {
					comp = ComponentFactory.createComponent(ctx, bean, inPage, previousComponent, null);
					content.setCachedComponent(comp);
				}
				// Logger.stepCount("load component", "load comp step 3 comp type = "+comp.getType());
				previousComponent = comp;
				// if (comp.isVisible(ctx)) { // this is allready checked in the isVisible method
				contentElements.add(comp);
				// }
				// Logger.endCount("load component", "load comp step 4");
			}
		}
	};

	public ContentElementList(ContentContext ctx, MenuElement inPage, boolean allArea) throws Exception {
		this(new ComponentBean[0], ctx, inPage, allArea);
	}

	public ContentElementList(ContentElementList list) {
		contentElements.addAll(list.contentElements);
		pos = list.pos;
		language = list.language;
		allArea = list.allArea;
	}

	protected void addElement(IContentVisualComponent elem) {
		/* need this test for repeat element */
		if (!addedElementId.contains(elem.getId())) {
			contentElements.add(elem);
			addedElementId.add(elem.getId());
		}
	}

	protected void addElementAsFirst(IContentVisualComponent elem) {
		/* need this test for repeat element */
		if (!addedElementId.contains(elem.getId())) {
			contentElements.addFirst(elem);
			addedElementId.add(elem.getId());
		}
	}

	public void addRepeatElement(IContentVisualComponent elem) {
		/* need this test for repeat element */
		if (!repeatAddedElementId.contains(elem.getId())) {
			repeatContentElements.add(elem);
			repeatAddedElementId.add(elem.getId());
		}
	}

	/**
	 * Return an iterable instance of this {@link ContentElementList} calling {@link #hasNext(ContentContext)} and {@link #next(ContentContext)} with the <code>ctx</code> parameter. <br/>
	 * WARNING: {@link #initialize()} is called when {@link Iterable#iterator()} is called.
	 * 
	 * @param ctx
	 * @return
	 */
	public Iterable<IContentVisualComponent> asIterable(final ContentContext ctx) {
		return new Iterable<IContentVisualComponent>() {
			@Override
			public Iterator<IContentVisualComponent> iterator() {
				initialize(ctx);
				return new Iterator<IContentVisualComponent>() {
					@Override
					public boolean hasNext() {
						return ContentElementList.this.hasNext(ctx);
					}

					@Override
					public IContentVisualComponent next() {
						return ContentElementList.this.next(ctx);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("Cannot modify ContentElementList");
					}
				};
			}
		};
	}

	IContentVisualComponent currentElem(ContentContext ctx) {
		if (getElement(pos - 1) != null) {
			if (!isVisible(ctx, getElement(pos - 1))) {
				return next(ctx);
			}
		}
		return getElement(pos - 1);
	}

	IContentVisualComponent firstElement(ContentContext ctx) {
		Iterator<IContentVisualComponent> compIt = contentElements.iterator();
		while (compIt.hasNext()) {
			IContentVisualComponent comp = compIt.next();
			if (isVisible(ctx, comp)) {
				return comp;
			}
		}
		return null;
	}

	IContentVisualComponent getElement(int elemPos) {
		Object[] array = contentElements.toArray();
		if ((elemPos >= array.length) || (elemPos < 0)) {
			return null;
		} else {
			return (IContentVisualComponent) array[elemPos];
		}
	}

	public String getLabel() {
		String res = "";
		Iterator elems = contentElements.iterator();
		while (elems.hasNext()) {
			IContentVisualComponent comp = (IContentVisualComponent) elems.next();
			if (comp.isLabel() && !comp.isRepeat()) {
				res = comp.getTextLabel();
				if (res == null) {
					res = "";
				}
			}
			if (comp instanceof MenuTitle && !comp.isRepeat()) {
				return comp.getTextLabel();
			}
		}
		if (res.length() == 0) { // if no element not repeat search with repeat element
			elems = contentElements.iterator();
			while (elems.hasNext()) {
				IContentVisualComponent comp = (IContentVisualComponent) elems.next();
				if (comp.isLabel()) {
					res = comp.getTextLabel();
					if (res == null) {
						res = "";
					}
				}
				if (comp instanceof MenuTitle) {
					return comp.getTextLabel();
				}
			}
		}
		return res;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	public MenuElement getPage() {
		return page;
	}

	public String getPageTitle() {

		String res = null;
		Iterator elems = contentElements.iterator();
		while (elems.hasNext()) {
			IContentVisualComponent comp = (IContentVisualComponent) elems.next();
			if (comp instanceof PageTitle) {
				res = comp.getTextTitle();
				if (res == null) {
					res = "";
				}
				return res;
			}
		}

		return res;
	}

	@Override
	public String getPrefixXHTMLCode(ContentContext ctx) {

		StringBuffer prefix = new StringBuffer();

		if (contentElements.size() != 0) {

			if (ctx.getRenderMode() != ContentContext.EDIT_MODE) {

				if (isPrevious(ctx)) {
					if (!previousElem(ctx).getType().equals(currentElem(ctx).getType())) {
						prefix.append(currentElem(ctx).getFirstPrefix(ctx));
					}
				} else {
					prefix.append(currentElem(ctx).getFirstPrefix(ctx));
				}
			}
		}

		return prefix.toString();
	}

	public String getSubTitle(ContentContext ctx) {
		String res = "";
		Iterator elems = contentElements.iterator();
		while (elems.hasNext() && res.length() == 0) {
			IContentVisualComponent comp = (IContentVisualComponent) elems.next();
			if (comp instanceof SubTitle) {
				res = comp.getValue(ctx);
				if (res == null) {
					res = "";
				} else {
					return res;
				}
			}
		}
		return "";
	}

	@Override
	public String getSufixXHTMLCode(ContentContext ctx) {

		StringBuffer sufix = new StringBuffer();
		if (ctx.getRenderMode() != ContentContext.EDIT_MODE) {
			if (contentElements.size() != 0) {
				if (isNext(ctx)) {
					if (!nextElem(ctx).getType().equals(currentElem(ctx).getType())) {
						sufix.append(currentElem(ctx).getLastSufix(ctx));
					}
				} else {
					sufix.append(currentElem(ctx).getLastSufix(ctx));
				}
			}
		}
		return sufix.toString();
	}

	public String getTitle() {

		String res = "";
		Iterator elems = contentElements.iterator();
		while (elems.hasNext() && res.length() == 0) {
			IContentVisualComponent comp = (IContentVisualComponent) elems.next();
			if (comp.isLabel() && !comp.isRepeat()) {
				res = comp.getTextTitle();
				if (res == null) {
					res = "";
				}
			}
		}
		if (res.length() == 0) { // if no element not repeat search with repeat element
			elems = contentElements.iterator();
			while (elems.hasNext() && res.length() == 0) {
				IContentVisualComponent comp = (IContentVisualComponent) elems.next();
				if (comp.isLabel()) {
					res = comp.getTextTitle();
					if (res == null) {
						res = "";
					}
				}
			}
		}
		if (res.length() == 0) {
			elems = contentElements.iterator();
			while (elems.hasNext()) {
				IContentVisualComponent comp = (IContentVisualComponent) elems.next();
				if (comp.getType().equals(PageTitle.TYPE)) {
					res = comp.getTextTitle();
					if (res == null) {
						res = "";
					}
					return res;
				}

			}
		}
		return res;
	}

	public String getXHTMLTitle(ContentContext ctx) throws Exception {
		String res = "";
		Iterator elems = contentElements.iterator();
		while (elems.hasNext()) {
			IContentVisualComponent comp = (IContentVisualComponent) elems.next();
			if (comp.isLabel()) {
				res = comp.getXHTMLCode(ctx);
			}

		}
		return res;
	}

	@Override
	public boolean hasNext(ContentContext ctx) {
		return isNext(ctx);
	}

	/**
	 * return to the start of the list
	 */
	@Override
	public void initialize(ContentContext ctx) {

		if (repeatContentElements.size() > 0) {

			LinkedList<IContentVisualComponent> newContentElements = new LinkedList<IContentVisualComponent>();

			boolean repeatComponentFound = false;
			for (IContentVisualComponent comp : contentElements) {
				if (comp instanceof RepeatContainer) {
					if (!((RepeatContainer) comp).isBlockRepeat(ctx)) {
						newContentElements.addAll(repeatContentElements);
					}
					repeatComponentFound = true;
				} else {
					newContentElements.add(comp);
				}
			}

			if (repeatComponentFound) {
				contentElements = newContentElements;
			} else {
				LinkedList<IContentVisualComponent> comps = new LinkedList<IContentVisualComponent>();
				for (IContentVisualComponent comp : repeatContentElements) {
					if (comp.isFirstRepeated()) {
						comps.addFirst(comp);
					} else {
						addElement(comp);
					}
				}
				for (IContentVisualComponent compElem : comps) {
					addElementAsFirst(compElem);
				}
			}
		}

		pos = 0;
	}

	boolean isNext(ContentContext ctx) {
		boolean isNext = nextElem(ctx) != null;
		return isNext;
	}

	boolean isPrevious(ContentContext ctx) {
		return previousElem(ctx) != null;
	}

	private boolean isVisible(ContentContext ctx, IContentVisualComponent comp) {
		boolean outVisibility;
		if (ctx.getRenderMode() == ContentContext.EDIT_MODE || allArea) {
			outVisibility = true;
		} else {
			outVisibility = comp.isVisible(ctx);
		}
		if (!(allArea || ctx.getArea() == null)) {
			outVisibility = outVisibility && comp.getArea().equals(ctx.getArea());
		}
		return outVisibility;

	}

	@Override
	public IContentVisualComponent next(ContentContext ctx) {

		IContentVisualComponent comp = getElement(pos);
		pos = pos + 1;
		while ((comp != null) && (!isVisible(ctx, comp))) {
			comp = getElement(pos);
			pos = pos + 1;
		}
		return comp;
	}

	IContentVisualComponent nextElem(ContentContext ctx) {
		IContentVisualComponent comp = getElement(pos);

		int i = 1;
		while ((comp != null) && (!isVisible(ctx, comp))) {
			comp = getElement(pos + i);
			i++;
		}
		return comp;
	}

	IContentVisualComponent previousElem(ContentContext ctx) {
		IContentVisualComponent comp = getElement(pos - 2);

		int i = 3;
		while ((comp != null) && (!isVisible(ctx, comp))) {
			comp = getElement(pos - i);
			i++;
		}
		return comp;
	}

	public int realSize() {
		return contentElements.size();
	}

	@Override
	public void setAllArea(boolean inAllArea) {
		allArea = inAllArea;
	}

	public void setPage(MenuElement page) {
		this.page = page;
	}

	@Override
	public int size(ContentContext ctx) {

		int outSize = 0;

		Iterator<IContentVisualComponent> compIt = contentElements.iterator();
		while (compIt.hasNext()) {
			IContentVisualComponent comp = compIt.next();
			if (isVisible(ctx, comp)) {
				outSize++;
			}
		}

		return outSize;
	}

}
