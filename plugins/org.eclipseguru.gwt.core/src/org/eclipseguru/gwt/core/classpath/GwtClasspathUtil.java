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
package org.eclipseguru.gwt.core.classpath;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.internal.classpath.AccessRulesUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

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
	 *            if the project already had a container entry
	 * @throws CoreException
	 */
	public static boolean addGwtContainer(IProject project, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(MessageFormat.format("Adding GWT classpath container to project {0}", project.getName()), 20);

			// get current classpath
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();

			// check if entry is already present
			List<IClasspathEntry> newClasspath = new ArrayList<IClasspathEntry>(oldClasspath.length + 1);
			for (IClasspathEntry entry : oldClasspath) {
				if (isGwtContainer(entry))
					return false;
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

	/**
	 * @param entry
	 * @param containerId
	 * @return
	 */
	private static boolean isContainerEntry(IClasspathEntry entry, String containerId) {
		return (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) && (entry.getPath().segmentCount() > 0) && containerId.equals(entry.getPath().segment(0));
	}

	/**
	 * Indicates if the specified entry is a GWT container entry.
	 * 
	 * @param entry
	 * @return <code>true</code> if the specified entry is a GWT container
	 *         entry, <code>false</code> otherwise
	 */
	public static boolean isGwtContainer(IClasspathEntry entry) {
		return isContainerEntry(entry, GwtCore.GWT_CONTAINER);
	}

	/**
	 * Indicates if the specified entry is a JRE container entry.
	 * 
	 * @param entry
	 * @return <code>true</code> if the specified entry is a JRE container
	 *         entry, <code>false</code> otherwise
	 */
	public static boolean isJREContainer(IClasspathEntry entry) {
		return isContainerEntry(entry, JavaRuntime.JRE_CONTAINER);
	}

	/**
	 * Sets the compilance option for the specified project.
	 * 
	 * @param project
	 * @param compliance
	 */
	@SuppressWarnings("unchecked")
	private static void setComplianceOptions(IJavaProject project, String compliance) {
		Map map = project.getOptions(false);
		if (compliance == null) {
			if (map.size() > 0) {
				map.remove(JavaCore.COMPILER_COMPLIANCE);
				map.remove(JavaCore.COMPILER_SOURCE);
				map.remove(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
				map.remove(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER);
				map.remove(JavaCore.COMPILER_PB_ENUM_IDENTIFIER);
			} else
				return;
		} else if (JavaCore.VERSION_1_6.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
		} else if (JavaCore.VERSION_1_5.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
		} else if (JavaCore.VERSION_1_4.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);
		} else if (JavaCore.VERSION_1_3.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_1);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
		}
		project.setOptions(map);
	}

	/**
	 * Updates the JRE container of the specified project to match the GWT
	 * execution environment.
	 * 
	 * @param project
	 * @param monitor
	 * @throws CoreException
	 */
	public static void updateJREContainer(IProject project, IProgressMonitor monitor, boolean setAccessRules) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(MessageFormat.format("Updateing JRE container of project {0}", project.getName()), 20);

			// get project
			IJavaProject javaProject = JavaCore.create(project);

			// set access rules if necessary
			if (setAccessRules) {
				// get current classpath
				IClasspathEntry[] oldClasspath = javaProject.getRawClasspath();

				// update existing JRE entry
				List<IClasspathEntry> newClasspath = new ArrayList<IClasspathEntry>(oldClasspath.length + 1);
				for (IClasspathEntry entry : oldClasspath) {
					if (isJREContainer(entry))
						entry = JavaCore.newContainerEntry(entry.getPath(), AccessRulesUtil.getJREAccessRules(), new IClasspathAttribute[0], false);
					newClasspath.add(entry);
					ProgressUtil.checkCanceled(monitor);
				}

				// set new classpath
				javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]), ProgressUtil.subProgressMonitor(monitor, 10));

				ProgressUtil.checkCanceled(monitor);
			}

			// update compliance options
			monitor.subTask("Setting compliance options...");
			setComplianceOptions(javaProject, JavaCore.VERSION_1_4);

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
