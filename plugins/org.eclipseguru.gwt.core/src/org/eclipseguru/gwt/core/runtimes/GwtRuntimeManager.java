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
package org.eclipseguru.gwt.core.runtimes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.classpath.GwtContainer;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

/**
 * A manager for GWT runtimes.
 */
public class GwtRuntimeManager implements GwtCorePreferenceConstants {

	private final static String CP_HOME_PREFERENCES_PREFIX = GwtCore.PLUGIN_ID + "." + PREF_GWT_HOME;
	private static final boolean logProblems = false;
	private static IPropertyChangeListener listener = new IPropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent event) {
			String key = event.getProperty();
			if (PREF_GWT_HOME.equals(key))
				try {
					installedRuntimes = null;
					rebindClasspathEntries();
				} catch (CoreException e) {
					if (logProblems)
						GwtCore.logError("Exception while rebinding GWT runtime library '" + key.substring(CP_HOME_PREFERENCES_PREFIX.length()) + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$

				}
		}
	};

	private static GwtRuntime[] installedRuntimes;

	/**
	 * Returns a list of all installed runtimes.
	 * 
	 * @return a list of all installed runtimes
	 */
	public static GwtRuntime[] getInstalledRuntimes() {
		if (null == installedRuntimes) {
			Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
			pluginPreferences.addPropertyChangeListener(listener);
			installedRuntimes = new GwtRuntime[] { new GwtRuntime(Path.fromPortableString(pluginPreferences.getString(GwtCorePreferenceConstants.PREF_GWT_HOME))) };
		}
		return installedRuntimes;
	}

	private static void rebindClasspathEntries() throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IJavaProject[] projects = JavaCore.create(root).getJavaProjects();
		IPath containerPath = new Path(GwtCore.GWT_CONTAINER);

		List<IJavaProject> affectedProjects = new ArrayList<IJavaProject>();
		List<IClasspathContainer> newContainers = new ArrayList<IClasspathContainer>();

		GwtRuntime runtime = getInstalledRuntimes()[0];
		GwtContainer container = new GwtContainer(runtime, containerPath);

		for (IJavaProject project : projects) {
			IClasspathEntry[] entries = project.getRawClasspath();
			for (IClasspathEntry curr : entries) {
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					if (containerPath.equals(curr.getPath())) {
						affectedProjects.add(project);
						newContainers.add(container);
						break;
					}
			}
		}
		if (!affectedProjects.isEmpty()) {
			IJavaProject[] affected = affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
			IClasspathContainer[] containers = newContainers.toArray(new IClasspathContainer[newContainers.size()]);
			JavaCore.setClasspathContainer(containerPath, affected, containers, null);

		}
	}

	/**
	 * Hidden
	 */
	private GwtRuntimeManager() {
		// empty
	}

}
