/*******************************************************************************
 * Copyright (c) 2006, 2010 EclipseGuru and others.
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
 * The GWT classpath container initializer.
 */
public class GwtContainerInitializer extends ClasspathContainerInitializer {

	@Override
	public String getDescription(final IPath containerPath, final IJavaProject project) {
		final StringBuilder description = new StringBuilder(100);
		description.append("Google Web Toolkit");
		final int size = containerPath.segmentCount();
		if (size > 1) {
			description.append(" [").append(containerPath.segment(1)).append("]");
		}
		return description.toString();
	}

	@Override
	public void initialize(final IPath containerPath, final IJavaProject project) throws CoreException {
		final int size = containerPath.segmentCount();
		if (size > 0) {
			if (containerPath.segment(0).equals(GwtCore.GWT_CONTAINER)) {
				GwtContainer container = null;
				if (size > 1) {
					final GwtRuntime gwtRuntime = GwtRuntimeManager.findInstalledRuntime(containerPath.segment(1));
					if ((null != gwtRuntime) && !gwtRuntime.getLocation().isEmpty()) {
						container = new GwtContainer(gwtRuntime, containerPath);
					}
				} else {
					final GwtRuntime gwtRuntime = GwtRuntimeManager.findInstalledRuntime(null);
					if ((null != gwtRuntime) && !gwtRuntime.getLocation().isEmpty()) {
						container = new GwtContainer(gwtRuntime, containerPath);
					}
				}

				JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container }, null);
			}
		}
	}
}
