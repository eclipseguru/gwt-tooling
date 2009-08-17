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
package org.eclipseguru.gwt.core.server;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.launch.GwtLaunchUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;

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
	protected static IModule getWebModule(final IProject project) {
		if (null == project) {
			return null;
		}

		final IModule[] modules = ServerUtil.getModules("jst.web");
		if ((null == modules) || (modules.length == 0)) {
			return null;
		}
		for (final IModule module : modules) {
			if (project.equals(module.getProject())) {
				return module;
			}
		}
		return null;
	}

	@Override
	public IModuleArtifact getModuleArtifact(final Object obj) {
		if (!(obj instanceof IAdaptable)) {
			return null;
		}

		final IFile file = (IFile) ((IAdaptable) obj).getAdapter(IFile.class);
		if (null == file) {
			return null;
		}

		if (!GwtUtil.isModuleDescriptor(file)) {
			return null;
		}

		final GwtModule gwtModule = GwtCore.create(file);
		if (null == gwtModule) {
			return null;
		}

		final IModule webModule = getWebModule(gwtModule.getProjectResource());
		if (null == webModule) {
			return null;
		}

		try {
			final IPath path = GwtUtil.getDeploymentPath(gwtModule.getProject()).append(GwtLaunchUtil.computeDefaultUrl(gwtModule));
			return new GwtBrowserResource(webModule, path, gwtModule);
		} catch (final CoreException e) {
			return null;
		}
	}
}
