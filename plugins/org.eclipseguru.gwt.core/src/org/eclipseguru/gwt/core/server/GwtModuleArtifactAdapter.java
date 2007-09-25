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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.launch.GwtLaunchUtil;

/**
 * The GWT module arifact adapter.
 */
public class GwtModuleArtifactAdapter extends ModuleArtifactAdapterDelegate {

	/**
	 * Returns the Web module for the specified project.
	 * 
	 * @param project
	 * @return
	 */
	protected static IModule getWebModule(IProject project) {
		if (null == project)
			return null;

		IModule[] modules = ServerUtil.getModules("jst.web");
		if ((null == modules) || (modules.length == 0))
			return null;
		for (IModule module : modules) {
			if (project.equals(module.getProject()))
				return module;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate#getModuleArtifact(java.lang.Object)
	 */
	@Override
	public IModuleArtifact getModuleArtifact(Object obj) {
		if (!(obj instanceof IAdaptable))
			return null;

		IFile file = (IFile) ((IAdaptable) obj).getAdapter(IFile.class);
		if (null == file)
			return null;

		if (!GwtUtil.isModuleDescriptor(file))
			return null;

		GwtModule gwtModule = GwtCore.create(file);
		if (null == gwtModule)
			return null;

		IModule webModule = getWebModule(gwtModule.getProjectResource());
		if (null == webModule)
			return null;

		IPath path = GwtUtil.getDeploymentPath(gwtModule.getProject()).append(GwtLaunchUtil.computeDefaultUrl(gwtModule.getModuleId()));
		return new GwtBrowserResource(webModule, path, gwtModule);
	}
}
