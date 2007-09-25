/*
 * Copyright 2006 TG. (techieguy@gmail.com)
 * Copyright 2006 Eclipse Guru (eclipseguru@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.googlipse.gwt.facet;

import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;
import org.eclipseguru.gwt.core.facet.GwtFacetConstants;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class GwtWebInstallActionConfigFactory extends FacetInstallDataModelProvider {

	// public Set getPropertyNames() {
	// Set names = super.getPropertyNames();
	// return names;
	// }
	//
	@Override
	public Object getDefaultProperty(String propertyName) {

		Object property;
		if (propertyName.equals(FACET_ID))
			property = getFacetId();
		else
			property = super.getDefaultProperty(propertyName);

		return property;
	}

	/**
	 * @return
	 */
	protected String getFacetId() {
		return GwtFacetConstants.FACET_ID_GWT_WEB;
	}

	// public boolean propertySet(String propertyName, Object propertyValue) {
	// return super.propertySet(propertyName, propertyValue);
	// }

}
