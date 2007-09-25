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
