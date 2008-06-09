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

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.classpath.GwtClasspathUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

	private void copyWebAppLibraries(final IProject project, final IProgressMonitor monitor) throws CoreException {
		final IPath[] webAppLibraries = GwtCore.getRuntime(GwtCore.create(project)).getWebAppLibraries();
		for (final IPath webAppLib : webAppLibraries) {
			// get the flex project component
			final IVirtualComponent flexProject = ComponentCore.createComponent(project);
			if (!flexProject.exists()) {
				flexProject.create(IResource.FORCE, null);
			}

			// get the WEB-INF/lib folder
			final IVirtualFolder webAppLibFolder = flexProject.getRootFolder().getFolder(ClasspathDependencyUtil.getDefaultRuntimePath(true));
			if (!webAppLibFolder.exists()) {
				webAppLibFolder.create(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			final IFile targetFile = webAppLibFolder.getUnderlyingFolder().getFile(new Path(webAppLib.lastSegment()));
			// be aggressive: remove any old resource first
			if (targetFile.exists()) {
				targetFile.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			try {
				targetFile.create(new FileInputStream(webAppLib.toFile()), IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			} catch (final FileNotFoundException e) {
				throw new CoreException(GwtCore.newErrorStatus(MessageFormat.format("Could not create {0}: {1}", targetFile.getFullPath().toOSString(), e.getMessage()), e));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.wst.common.project.facet.core.IDelegate#execute(org.eclipse
	 * .core.resources.IProject,
	 * org.eclipse.wst.common.project.facet.core.IProjectFacetVersion,
	 * java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(final IProject project, final IProjectFacetVersion fv, final Object config, IProgressMonitor monitor) throws CoreException {
		// get monitor
		monitor = ProgressUtil.monitor(monitor);
		try {
			// begin
			monitor.beginTask(MessageFormat.format("Installing GWT {0}...", getFacetName()), 6);

			// add nature
			GwtProject.addGwtNature(project, ProgressUtil.subProgressMonitor(monitor, 1));

			ProgressUtil.checkCanceled(monitor);

			// update build path (consumes 2 ticks)
			updateBuildPath(project, monitor);

			ProgressUtil.checkCanceled(monitor);

			// copy necessary libraries (consumes 3 ticks)
			copyWebAppLibraries(project, monitor);

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
		GwtClasspathUtil.addGwtContainer(project, ProgressUtil.subProgressMonitor(monitor, 1));
		GwtClasspathUtil.updateJREContainer(project, ProgressUtil.subProgressMonitor(monitor, 1), addAccessRules());
	}

}
