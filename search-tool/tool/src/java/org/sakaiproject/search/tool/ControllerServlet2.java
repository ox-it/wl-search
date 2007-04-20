/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.search.tool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.search.tool.api.SearchBeanFactory;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.util.ResourceLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author ieb
 */
public class ControllerServlet2 extends HttpServlet
{
	private static final Log log = LogFactory.getLog(ControllerServlet2.class);

	private static final String MACROS = "/WEB-INF/vm/macros.vm";

	private static final String BUNDLE_NAME = "org.sakaiproject.search.tool.bundle.Messages"; //$NON-NLS-1$

	private static final ResourceLoader rlb = new ResourceLoader(BUNDLE_NAME);

	/**
	 * Required for serialization... also to stop eclipse from giving me a
	 * warning!
	 */
	private static final long serialVersionUID = 676743152200357708L;

	public static final String SAVED_REQUEST_URL = "org.sakaiproject.search.api.last-request-url";

	private static final String PANEL = "panel";

	private static final Object TITLE_PANEL = "Title";

	private WebApplicationContext wac;

	private SearchBeanFactory searchBeanFactory = null;

	private SessionManager sessionManager;

	private Map<String, String> contentTypes = new HashMap<String, String>();

	private Map<String, String> characterEncodings = new HashMap<String, String>();

	private VelocityEngine vengine;

	private String inlineMacros;

	private String basePath;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException
	{
		super.init(servletConfig);

		ServletContext sc = servletConfig.getServletContext();

		wac = WebApplicationContextUtils.getWebApplicationContext(sc);
		if (wac == null)
		{
			throw new ServletException("Unable to get WebApplicationContext ");
		}
		searchBeanFactory = (SearchBeanFactory) wac.getBean("search-searchBeanFactory");
		if (searchBeanFactory == null)
		{
			throw new ServletException("Unable to get search-searchBeanFactory ");
		}
		sessionManager = (SessionManager) wac.getBean(SessionManager.class.getName());
		if (sessionManager == null)
		{
			throw new ServletException("Unable to get " + SessionManager.class.getName());
		}

		searchBeanFactory.setContext(sc);

		inlineMacros = MACROS;
		try
		{
			vengine = new VelocityEngine();

			vengine.setApplicationAttribute(ServletContext.class.getName(), sc);

			Properties p = new Properties();
			p.load(this.getClass().getResourceAsStream("searchvelocity.config"));
			vengine.init(p);
			vengine.getTemplate(inlineMacros);

		}
		catch (Exception ex)
		{
			throw new ServletException(ex);
		}
		contentTypes.put("opensearch", "application/opensearchdescription+xml");
		contentTypes.put("sakai.src", "application/opensearchdescription+xml" );
		contentTypes.put("rss20", "text/xml" );
	}

	protected void doGet(final HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{

		execute(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		execute(request, response);
	}

	public void execute(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		request.setAttribute(Tool.NATIVE_URL, Tool.NATIVE_URL);

		VelocityContext vc = new VelocityContext();

		String sakaiHeader = (String) request.getAttribute("sakai.html.head");
		String toolPlacement = (String) request.getAttribute("sakai.tool.placement.id");
		String toolPlacementJs = toolPlacement.toString().replace('-','x');
		String skin = "default/"; // this could be changed in the future to
		// make search skin awaire

		vc.put("skin", skin);
		vc.put("sakaiheader", sakaiHeader);
		vc.put("rlb",rlb);
		vc.put("sakai_tool_placement_id", toolPlacement);
		vc.put("sakai_tool_placement_id_js", toolPlacementJs);

		String targetURL = persistState(request);
		if (targetURL != null && targetURL.trim().length() > 0)
		{
			response.sendRedirect(targetURL);
			return;
		}
		String template = null;
		if (TITLE_PANEL.equals(request.getParameter(PANEL)))
		{
			template = "title";

		}
		else
		{
			
			
			
			String path = request.getPathInfo();
			if (path == null || path.length() == 0)
			{
				path = "index";
			}
			if (path.startsWith("/"))
			{
				path = path.substring(1);
			}
			template = path;
		}
		log.debug("Path is "+template+" for "+request.getPathInfo());
		if ( "sakai.gif".equals(template) ) {
			try
			{
				searchBeanFactory.newSherlockSearchBean(request).sendIcon(response);
			}
			catch (PermissionException e)
			{
				log.warn("Failed to send gif ",e);
			}
			return;
		}
		try
		{
			vc.put("searchModel", searchBeanFactory.newSearchBean(request));
		}
		catch (PermissionException e1)
		{
		}
		try
		{
			vc.put("adminModel", searchBeanFactory.newSearchAdminBean(request));
		}
		catch (PermissionException e1)
		{
		}
		try
		{
			vc.put("sherlockModel", searchBeanFactory.newSherlockSearchBean(request));
		}
		catch (PermissionException e1)
		{
		}
		try
		{
			vc.put("openSearchModel", searchBeanFactory.newOpenSearchBean(request));
		}
		catch (PermissionException e1)
		{
		}
		try
		{
			

			
			String filePath = "/WEB-INF/vm/" + template + ".vm";
			String contentType = contentTypes.get(template);
			if (contentType == null)
			{
				contentType = "text/html";
			}
			String characterEncoding = characterEncodings.get(template);
			if (characterEncoding == null)
			{
				characterEncoding = "UTF-8";
			}

			response.setContentType(contentType);
			response.setCharacterEncoding(characterEncoding);
			vengine.mergeTemplate(filePath, vc, response.getWriter());

			request.removeAttribute(Tool.NATIVE_URL);
		}
		catch (Exception e)
		{
			throw new ServletException("Search Failed ", e);
		}
	}

	public void addLocalHeaders(HttpServletRequest request)
	{
	}

	/**
	 * returns the request state for the tool. If the state is restored, we set
	 * the request attribute RWikiServlet.REQUEST_STATE_RESTORED to Boolean.TRUE
	 * and a Thread local named RWikiServlet.REQUEST_STATE_RESTORED to
	 * Boolean.TRUE. These MUST be checked by anything that modifies state, to
	 * ensure that a reinitialisation of Tool state does not result in a repost
	 * of data.
	 * 
	 * @param request
	 * @return
	 */
	private String persistState(HttpServletRequest request)
	{
		ToolSession ts = sessionManager.getCurrentToolSession();
		if (isPageToolDefault(request))
		{
			log.debug("Incomming URL is " + request.getRequestURL().toString() + "?"
					+ request.getQueryString());
			log.debug("Restore " + ts.getAttribute(SAVED_REQUEST_URL));
			return (String) ts.getAttribute(SAVED_REQUEST_URL);
		}
		if (isPageRestorable(request))
		{
			ts.setAttribute(SAVED_REQUEST_URL, request.getRequestURL().toString() + "?"
					+ request.getQueryString());
			log.debug("Saved " + ts.getAttribute(SAVED_REQUEST_URL));
		}
		return null;
	}

	/**
	 * Check to see if the reques represents the Tool default page. This is not
	 * the same as the view Home. It is the same as first entry into a Tool or
	 * when the page is refreshed
	 * 
	 * @param request
	 * @return true if the page is the Tool default page
	 */
	private boolean isPageToolDefault(HttpServletRequest request)
	{
		if (TITLE_PANEL.equals(request.getParameter(PANEL))) return false;
		String pathInfo = request.getPathInfo();
		String queryString = request.getQueryString();
		String method = request.getMethod();
		return ("GET".equalsIgnoreCase(method)
				&& (pathInfo == null || request.getPathInfo().length() == 0) && (queryString == null || queryString
				.length() == 0));
	}

	/**
	 * Check to see if the request represents a page that can act as a restor
	 * point.
	 * 
	 * @param request
	 * @return true if it is possible to restore to this point.
	 */
	private boolean isPageRestorable(HttpServletRequest request)
	{

		if (TITLE_PANEL.equals(request.getParameter(PANEL))) return false;
		String pathInfo = request.getPathInfo();
		if (pathInfo != null)
		{
			if (request.getPathInfo().endsWith(".gif"))
			{
				return false;
			}
			if (request.getPathInfo().endsWith(".src"))
			{
				return false;
			}
		}

		if ("GET".equalsIgnoreCase(request.getMethod())) return true;

		return false;
	}


}
