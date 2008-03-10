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

import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class GwtWebUninstallActionConfigFactory extends FacetInstallDataModelProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider#getDefaultProperty(java.lang.String)
	 */
	@Override
	public Object getDefaultProperty(final String propertyName) {
		if (propertyName.equals(FACET_ID))
			return getFacetId();
		return super.getDefaultProperty(propertyName);
	}

	/**
	 * Returns the facet id.
	 * 
	 * @return the facet id
	 */
	protected String getFacetId() {
		return GwtFacetConstants.FACET_ID_GWT_WEB;
	}

}
