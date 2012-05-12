package org.javlo.servlet;

import java.io.StringWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javlo.context.ContentContext;
import org.javlo.context.EditContext;
import org.javlo.context.GlobalContext;
import org.javlo.data.InfoBean;
import org.javlo.helper.ServletHelper;
import org.javlo.service.NotificationService;
import org.json.JSONObject;

public class AjaxServlet extends HttpServlet {

	private static final long serialVersionUID = -2086338711912267925L;

	/**
	 * create a static logger.
	 */
	protected static Logger logger = Logger.getLogger(AjaxServlet.class.getName());

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			ContentContext ctx = ContentContext.getContentContext(request, response);
			ctx.setAjax(true);
			GlobalContext globalContext = GlobalContext.getInstance(ctx.getRequest());
			EditContext editCtx = EditContext.getInstance(globalContext, request.getSession());
			InfoBean.updateInfoBean(ctx);
			
			String action = ServletHelper.execAction(ctx);
			logger.info("exec action : " + action);

			ServletHelper.prepareModule(ctx);

			JSONObject outMap = new JSONObject();

			String msgXhtml = ServletHelper.executeJSP(ctx, editCtx.getMessageTemplate());
			ctx.addAjaxInsideZone( "message-container", msgXhtml);
			
			int unreadNotification = NotificationService.getInstance(globalContext).getUnreadNotificationSize(editCtx.getUserPrincipal().getName(), 99);
			ctx.addAjaxInsideZone("notification-count", ""+unreadNotification);
			
			StringWriter strWriter = new StringWriter();

			outMap.put("insideZone", ctx.getAjaxInsideZone());
			outMap.put("zone", ctx.getAjaxZone());
			outMap.write(strWriter);
			
			response.setContentType("application/json");
			response.getWriter().write(strWriter.toString());
			response.flushBuffer();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
