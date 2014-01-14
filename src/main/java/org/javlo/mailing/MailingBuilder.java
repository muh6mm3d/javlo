package org.javlo.mailing;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.javlo.context.ContentContext;
import org.javlo.context.GlobalContext;
import org.javlo.helper.NetHelper;
import org.javlo.helper.StringHelper;
import org.javlo.helper.URLHelper;
import org.javlo.servlet.ContentOnlyServlet;
import org.javlo.user.AdminUserFactory;
import org.javlo.user.IUserFactory;
import org.javlo.user.IUserInfo;
import org.javlo.user.UserFactory;

public class MailingBuilder {

	private String currentTemplate;
	private String sender;
	private String subject;
	private String reportTo;
	private Collection<String> editorGroups = new LinkedHashSet<String>();
	private Collection<String> visitorGroups = new LinkedHashSet<String>();
	private Collection<String> excludedUsers = new HashSet<String>();
	private String recipients;
	private boolean isTestMailing;
	private Set<InternetAddress> allRecipients = new LinkedHashSet<InternetAddress>();

	public void setCurrentTemplate(String currentTemplate) {
		this.currentTemplate = currentTemplate;
	}

	public String getCurrentTemplate() {
		return currentTemplate;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getReportTo() {
		return reportTo;
	}

	public void setReportTo(String reportTo) {
		this.reportTo = reportTo;
	}

	public Collection<String> getEditorGroups() {
		return editorGroups;
	}

	public void setEditorGroups(Collection<String> editorGroups) {
		this.editorGroups = editorGroups;
	}

	public Collection<String> getVisitorGroups() {
		return visitorGroups;
	}

	public void setVisitorGroups(Collection<String> groups) {
		this.visitorGroups = groups;
	}

	public Collection<String> getExcludedUsers() {
		return excludedUsers;
	}

	public void setExcludedUsers(Collection<String> excludedUserLogin) {
		this.excludedUsers = excludedUserLogin;
	}

	public String getRecipients() {
		return recipients;
	}

	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}

	public boolean isTestMailing() {
		return isTestMailing;
	}

	public void setTestMailing(boolean isTest) {
		this.isTestMailing = isTest;
	}

	public Set<InternetAddress> getAllRecipients() {
		return allRecipients;
	}

	public void setAllRecipients(Set<InternetAddress> allRecipients) {
		this.allRecipients = allRecipients;
	}

	public boolean prepare(ContentContext ctx) {
		try {
			HttpServletRequest request = ctx.getRequest();
			GlobalContext globalContext = GlobalContext.getInstance(request);

			allRecipients.clear();
			if (editorGroups != null && !editorGroups.isEmpty()) {
				explodeGroups(allRecipients, editorGroups, excludedUsers, AdminUserFactory.createUserFactory(globalContext, request.getSession()));
			}
			if (visitorGroups != null && !visitorGroups.isEmpty()) {
				explodeGroups(allRecipients, visitorGroups, excludedUsers, UserFactory.createUserFactory(globalContext, request.getSession()));
			}
			if (recipients != null) {
				for (String fullEmail : StringHelper.searchEmail(recipients)) {
					InternetAddress email = new InternetAddress(fullEmail);
					if (!allRecipients.contains(email)) {
						allRecipients.add(email);
					}
				}
			}
			return true;
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		} catch (AddressException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void explodeGroups(Set<InternetAddress> allRecipients, Collection<String> groups, Collection<String> excludedUsers, IUserFactory userFactory) throws UnsupportedEncodingException {
		for (String group : groups) {
			Collection<IUserInfo> users = userFactory.getUserInfoForRoles(new String[] { group });
			for (IUserInfo user : users) {
				if (excludedUsers != null && excludedUsers.contains(user.getLogin())) {
					continue;
				}
				if (!StringHelper.isEmpty(user.getEmail())) {
					InternetAddress email = new InternetAddress(user.getEmail(), StringHelper.neverNull(user.getFirstName()) + " " + StringHelper.neverNull(user.getLastName()));
					if (!allRecipients.contains(email)) {
						allRecipients.add(email);
					}
				}
			}
		}
	}

	public void sendMailing(ContentContext ctx) throws Exception {
		ContentContext pageCtx = ctx.getContextWithOtherRenderMode(ContentContext.PAGE_MODE);
		pageCtx.setAbsoluteURL(true);
		Map<String, String> params = new HashMap<String, String>();
		if (currentTemplate != null) {
			params.put(ContentOnlyServlet.TEMPLATE_PARAM_NAME, currentTemplate);
		}
		String url = URLHelper.createURL(pageCtx);

		GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());

		Mailing m = new Mailing();
		m.setFrom(new InternetAddress(sender));
		m.setReceivers(allRecipients);
		m.setSubject(subject);
		m.setAdminEmail(globalContext.getAdministratorEmail());
		if (reportTo != null) {
			m.setNotif(new InternetAddress(reportTo));
		}
		m.setContent(NetHelper.readPage(url, true));
		m.setHtml(true);
		List<String> roles = new LinkedList<String>();
		if (editorGroups != null) {
			roles.addAll(editorGroups);
		}
		if (visitorGroups != null) {
			roles.addAll(visitorGroups);
		}
		m.setRoles(roles);
		m.store(ctx.getRequest().getSession().getServletContext());
	}
}