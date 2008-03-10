/*******************************************************************************
 * Copyright (c) 2006, 2008 EclipseGuru and others.
 * All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     EclipseGuru - initial API and implementation
 *******************************************************************************/
package org.eclipseguru.gwt.core.facet;

/**
 * The delegate for installing the module facet.
 */
public class GwtModuleInstallDelegate extends GwtWebInstallDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.facet.GwtWebInstallDelegate#addAccessRules()
	 */
	@Override
	protected boolean addAccessRules() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.facet.GwtWebInstallDelegate#getFacetName()
	 */
	@Override
	protected String getFacetName() {
		return "Module Facet";
	}
}
