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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipseguru.gwt.core.GwtCore;

/**
 * This is the {@link LaunchableAdapterDelegate} implementation that generates
 * launchables for running in the GWT browser.
 */
public class GwtLaunchableAdapter extends LaunchableAdapterDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.LaunchableAdapterDelegate#getLaunchable(org.eclipse.wst.server.core.IServer,
	 *      org.eclipse.wst.server.core.IModuleArtifact)
	 */
	@Override
	public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) throws CoreException {
		if (!(moduleArtifact instanceof GwtBrowserResource))
			return null;

		try {
			IURLProvider urlAdapter = (IURLProvider) server.getAdapter(IURLProvider.class);
			if (null == urlAdapter)
				urlAdapter = (IURLProvider) server.loadAdapter(IURLProvider.class, new NullProgressMonitor());
			if (null == urlAdapter)
				return null;

			URL url = (urlAdapter).getModuleRootURL(moduleArtifact.getModule());
			if (null == url)
				return null;

			GwtBrowserResource webResource = (GwtBrowserResource) moduleArtifact;
			String path = webResource.getPath().toString();
			if ((path != null) && path.startsWith("/") && (path.length() > 0))
				path = path.substring(1);
			if ((path != null) && (path.length() > 0))
				url = new URL(url, path);

			return new GwtBrowserLaunchable(url, webResource.getGwtModule());
		} catch (MalformedURLException e) {
			GwtCore.logError("Error while preparing launchable for GWT Browser.", e);
			return null;
		}
	}

}
