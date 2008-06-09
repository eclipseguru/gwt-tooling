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
import org.eclipseguru.gwt.core.internal.classpath.AccessRulesUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A classpath util.
 */
public class GwtClasspathUtil {

	/**
	 * Adds the GWT classpath container to the specified project.
	 * 
	 * @param project
	 * @param monitor
	 * @param <code>true</code> if the container was added, <code>false</code>
	 *        if the project already had a container entry
	 * @throws CoreException
	 */
	public static boolean addGwtContainer(final IProject project, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(MessageFormat.format("Adding GWT classpath container to project {0}", project.getName()), 20);

			// get current classpath
			final IJavaProject javaProject = JavaCore.create(project);
			final IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();

			// check if entry is already present
			final List<IClasspathEntry> newClasspath = new ArrayList<IClasspathEntry>(oldClasspath.length + 1);
			for (final IClasspathEntry entry : oldClasspath) {
				if (isGwtContainer(entry)) {
					return false;
				}
				newClasspath.add(entry);
			}

			ProgressUtil.checkCanceled(monitor);

			// add container entry
			newClasspath.add(JavaCore.newContainerEntry(new Path(GwtCore.GWT_CONTAINER)));

			// set new classpath
			javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]), ProgressUtil.subProgressMonitor(monitor, 10));

			return true; // added
		} finally {
			monitor.done();
		}
	}

	private static boolean isContainerEntry(final IClasspathEntry entry, final String containerId) {
		return (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) && (entry.getPath().segmentCount() > 0) && containerId.equals(entry.getPath().segment(0));
	}

	/**
	 * Indicates if the specified entry is a GWT container entry.
	 * 
	 * @param entry
	 * @return <code>true</code> if the specified entry is a GWT container
	 *         entry, <code>false</code> otherwise
	 */
	public static boolean isGwtContainer(final IClasspathEntry entry) {
		return isContainerEntry(entry, GwtCore.GWT_CONTAINER);
	}

	/**
	 * Indicates if the specified entry is a JRE container entry.
	 * 
	 * @param entry
	 * @return <code>true</code> if the specified entry is a JRE container
	 *         entry, <code>false</code> otherwise
	 */
	public static boolean isJREContainer(final IClasspathEntry entry) {
		return isContainerEntry(entry, JavaRuntime.JRE_CONTAINER);
	}

	/**
	 * Updates the JRE container of the specified project to match the GWT
	 * execution environment.
	 * 
	 * @param project
	 * @param monitor
	 * @param setAccessRules
	 * @throws CoreException
	 */
	public static void updateJREContainer(final IProject project, IProgressMonitor monitor, final boolean setAccessRules) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(MessageFormat.format("Updateing JRE container of project {0}", project.getName()), 20);

			// get project
			final IJavaProject javaProject = JavaCore.create(project);

			// set access rules if necessary
			if (setAccessRules) {
				// get current classpath
				final IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();

				// update existing JRE entry
				final List<IClasspathEntry> newClasspath = new ArrayList<IClasspathEntry>(oldClasspath.length + 1);
				for (IClasspathEntry entry : oldClasspath) {
					if (isJREContainer(entry)) {
						entry = JavaCore.newContainerEntry(entry.getPath(), AccessRulesUtil.getJREAccessRules(), new IClasspathAttribute[0], false);
					}
					newClasspath.add(entry);
					ProgressUtil.checkCanceled(monitor);
				}

				// set new classpath
				javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]), ProgressUtil.subProgressMonitor(monitor, 10));

				ProgressUtil.checkCanceled(monitor);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * hidden
	 */
	private GwtClasspathUtil() {
		// empty
	}
}
