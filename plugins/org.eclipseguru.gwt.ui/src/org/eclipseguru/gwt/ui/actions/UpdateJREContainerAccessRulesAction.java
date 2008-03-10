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
package org.eclipseguru.gwt.ui.actions;

import org.eclipseguru.gwt.core.classpath.GwtClasspathUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Updates the access rules of the JRE container.
 */
public class UpdateJREContainerAccessRulesAction extends UpdateProjectBuildPathAction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.ui.actions.UpdateProjectBuildPathAction#createUpdateClasspathRunnable(org.eclipse.core.resources.IProject)
	 */
	@Override
	protected IWorkspaceRunnable createUpdateClasspathRunnable(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor = ProgressUtil.monitor(monitor);
				try {
					monitor.beginTask("Updating classpath...", 2);

					// update JRE container
					GwtClasspathUtil.updateJREContainer(project, ProgressUtil.subProgressMonitor(monitor, 1), true);

				} finally {
					monitor.done();
				}
			}
		};
	}

}
