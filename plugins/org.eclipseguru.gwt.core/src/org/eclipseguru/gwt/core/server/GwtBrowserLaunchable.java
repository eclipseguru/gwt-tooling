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
package org.eclipseguru.gwt.core.server;

import org.eclipseguru.gwt.core.GwtModule;

import java.net.URL;

/**
 * A launchable for the GWT Browser
 */
public class GwtBrowserLaunchable {

	/** gwtModule */
	private final GwtModule gwtModule;

	private final URL url;

	/**
	 * Creates a new instance.
	 * 
	 * @param url
	 * @param module
	 */
	public GwtBrowserLaunchable(final URL url, final GwtModule module) {
		this.url = url;
		gwtModule = module;
	}

	/**
	 * Returns the GWT module.
	 * 
	 * @return the module
	 */
	public GwtModule getGwtModule() {
		return gwtModule;
	}

	/**
	 * Return the URL to the object.
	 * 
	 * @return the URL to the object
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "GwtBrowserLaunchable[url=" + url.toString() + ", module=" + gwtModule.getModuleId() + "]";
	}

}
