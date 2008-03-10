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
package org.eclipseguru.gwt.core.runtimes;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.classpath.GwtContainer;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

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

import java.util.ArrayList;
import java.util.List;

/**
 * A manager for GWT runtimes.
 */
public class GwtRuntimeManager implements GwtCorePreferenceConstants {

	private final static String CP_HOME_PREFERENCES_PREFIX = GwtCore.PLUGIN_ID + "." + PREF_GWT_HOME;
	private static final boolean logProblems = false;
	private static IPropertyChangeListener listener = new IPropertyChangeListener() {

		public void propertyChange(final PropertyChangeEvent event) {
			final String key = event.getProperty();
			if (PREF_GWT_HOME.equals(key)) {
				try {
					installedRuntimes = null;
					rebindClasspathEntries();
				} catch (final CoreException e) {
					if (logProblems) {
						GwtCore.logError("Exception while rebinding GWT runtime library '" + key.substring(CP_HOME_PREFERENCES_PREFIX.length()) + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
					}

				}
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
			final Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
			pluginPreferences.addPropertyChangeListener(listener);
			installedRuntimes = new GwtRuntime[] { new GwtRuntime(Path.fromPortableString(pluginPreferences.getString(GwtCorePreferenceConstants.PREF_GWT_HOME))) };
		}
		return installedRuntimes;
	}

	private static void rebindClasspathEntries() throws CoreException {
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IJavaProject[] projects = JavaCore.create(root).getJavaProjects();
		final IPath containerPath = new Path(GwtCore.GWT_CONTAINER);

		final List<IJavaProject> affectedProjects = new ArrayList<IJavaProject>();
		final List<IClasspathContainer> newContainers = new ArrayList<IClasspathContainer>();

		final GwtRuntime runtime = getInstalledRuntimes()[0];
		final GwtContainer container = new GwtContainer(runtime, containerPath);

		for (final IJavaProject project : projects) {
			final IClasspathEntry[] entries = project.getRawClasspath();
			for (final IClasspathEntry curr : entries)
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					if (containerPath.equals(curr.getPath())) {
						affectedProjects.add(project);
						newContainers.add(container);
						break;
					}
		}
		if (!affectedProjects.isEmpty()) {
			final IJavaProject[] affected = affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
			final IClasspathContainer[] containers = newContainers.toArray(new IClasspathContainer[newContainers.size()]);
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
