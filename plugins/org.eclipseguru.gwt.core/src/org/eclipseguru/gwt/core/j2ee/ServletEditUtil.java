/***************************************************************************************************
 * Copyright (c) 2006 Eclipse Guru and others.
 * All rights reserved. 
 *
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Eclipse Guru - initial API and implementation
 *               Eclipse.org - ideas, concepts and code from existing Eclipse projects
 **************************************************************************************************/
package org.eclipseguru.gwt.core.j2ee;

import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.JSPType;
import org.eclipse.jst.j2ee.webapplication.Servlet;
import org.eclipse.jst.j2ee.webapplication.ServletMapping;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipse.jst.j2ee.webapplication.WebapplicationFactory;

/**
 * A util for manipulating servlet data on a flex project.
 * <p>
 * This code is heavily inspired from WTP <code>AddServletOperation</code>.
 * </p>
 * 
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class ServletEditUtil {

	final WebArtifactEdit webArtifactEdit;

	/**
	 * Creates a new instance for the specified {@link WebArtifactEdit}. the
	 * {@link WebArtifactEdit} must be created for write operations.
	 * 
	 * @param webArtifactEdit
	 */
	public ServletEditUtil(WebArtifactEdit webArtifactEdit) {
		this.webArtifactEdit = webArtifactEdit;
	}

	/**
	 * Creates a new servlet and registers it on the web app.
	 * 
	 * @param qualifiedClassName
	 * @param isServletType
	 * @param simpleName
	 * @param displayName
	 * @param description
	 * @return Servlet instance
	 */
	@SuppressWarnings("unchecked")
	public Servlet createServlet(String qualifiedClassName, boolean isServletType, String simpleName, String displayName, String description) {
		// Create the servlet instance and set up the parameters from data model
		Servlet servlet = WebapplicationFactory.eINSTANCE.createServlet();
		servlet.setDisplayName(displayName);
		servlet.setServletName(simpleName);
		servlet.setDescription(description);
		// Handle servlet case
		if (isServletType) {
			ServletType servletType = WebapplicationFactory.eINSTANCE.createServletType();
			servletType.setClassName(qualifiedClassName);
			servlet.setWebType(servletType);
		}
		// Handle JSP case
		else {
			JSPType jspType = WebapplicationFactory.eINSTANCE.createJSPType();
			jspType.setJspFile(qualifiedClassName);
			servlet.setWebType(jspType);
		}
		// Add the servlet to the web application model
		WebApp webApp = getWebApp();
		webApp.getServlets().add(servlet);
		// Return the servlet instance
		return servlet;
	}

	/**
	 * Creates a new {@link ServletMapping} and registers it on the servlet.
	 * 
	 * @param servlet
	 * @param urlPattern
	 */
	@SuppressWarnings("unchecked")
	public ServletMapping createURLMappings(Servlet servlet, String urlPattern) {
		// Get the web app modelled object from the data model
		WebApp webApp = getWebApp();
		// Create the servlet mapping instance from the web factory
		ServletMapping mapping = WebapplicationFactory.eINSTANCE.createServletMapping();
		// Set the servlet and servlet name
		mapping.setServlet(servlet);
		mapping.setName(servlet.getServletName());
		// Set the URL pattern to map the servlet to
		mapping.setUrlPattern(urlPattern);
		// Add the servlet mapping to the web application modelled list
		webApp.getServletMappings().add(mapping);
		// return the mapping
		return mapping;
	}

	/**
	 * Returns the underlying {@link WebApp} from the {@link #webArtifactEdit}.
	 * 
	 * @return the {@link WebApp}
	 */
	protected WebApp getWebApp() {
		return webArtifactEdit.getWebApp();
	}
}
