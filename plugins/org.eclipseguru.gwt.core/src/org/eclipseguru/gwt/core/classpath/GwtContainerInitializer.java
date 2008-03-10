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
package org.eclipseguru.gwt.core.classpath;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.runtimes.GwtRuntime;
import org.eclipseguru.gwt.core.runtimes.GwtRuntimeManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * The GWT classpath copntainer initializer.
 */
public class GwtContainerInitializer extends ClasspathContainerInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getDescription(org.eclipse.core.runtime.IPath,
	 *      org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public String getDescription(final IPath containerPath, final IJavaProject project) {
		// TODO: container specific toolkit
		return "Google Webtoolkit";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse.core.runtime.IPath,
	 *      org.eclipse.jdt.core.IJavaProject)
	 */
	@Override
	public void initialize(final IPath containerPath, final IJavaProject project) throws CoreException {
		final int size = containerPath.segmentCount();
		if (size > 0)
			if (containerPath.segment(0).equals(GwtCore.GWT_CONTAINER)) {
				final GwtRuntime gwtRuntime = GwtRuntimeManager.getInstalledRuntimes()[0];
				GwtContainer container = null;
				if (!gwtRuntime.getLocation().isEmpty()) {
					container = new GwtContainer(gwtRuntime, containerPath);
				}
				JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container }, null);
			}
	}
}
