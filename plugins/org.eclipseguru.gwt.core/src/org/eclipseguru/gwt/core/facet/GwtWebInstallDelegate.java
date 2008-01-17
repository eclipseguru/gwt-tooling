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

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.classpath.GwtClasspathUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import java.text.MessageFormat;

/**
 * The delegate for installing a web facet.
 */
public class GwtWebInstallDelegate implements IDelegate {

	/**
	 * Indicates if access rules should be added to the build path.
	 * 
	 * @return <code>true</code> if access rules should be added,
	 *         <code>false</code> otherwise
	 */
	protected boolean addAccessRules() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.common.project.facet.core.IDelegate#execute(org.eclipse.core.resources.IProject,
	 *      org.eclipse.wst.common.project.facet.core.IProjectFacetVersion,
	 *      java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(final IProject project, final IProjectFacetVersion fv, final Object config, IProgressMonitor monitor) throws CoreException {
		// get monitor
		monitor = ProgressUtil.monitor(monitor);
		try {
			// begin
			monitor.beginTask(MessageFormat.format("Installing GWT {0}...", getFacetName()), 4);

			// add nature
			GwtProject.addGwtNature(project, ProgressUtil.subProgressMonitor(monitor, 1));

			// update build path
			updateBuildPath(project, monitor);

		} catch (final CoreException e) {
			// log exception & cancel
			GwtCore.logError(MessageFormat.format("Error while installing GWT {0}: {1}", getFacetName(), e.getMessage()), e);
			monitor.setCanceled(true);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns a human readable name of the facet to install.
	 * 
	 * @return the facet name
	 */
	protected String getFacetName() {
		return "Web Facet";
	}

	/**
	 * Updates the build path of the specified project.
	 * 
	 * @param project
	 *            the project
	 * @param monitor
	 *            the monitor (already started, allowed to consume two ticks)
	 * @throws CoreException
	 *             if an error occurred
	 */
	protected void updateBuildPath(final IProject project, final IProgressMonitor monitor) throws CoreException {
		GwtClasspathUtil.addGwtContainer(project, ProgressUtil.subProgressMonitor(monitor, 2));
		GwtClasspathUtil.updateJREContainer(project, ProgressUtil.subProgressMonitor(monitor, 2), addAccessRules());
	}

}
