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
package org.eclipseguru.gwt.core.runtimes;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.classpath.GwtContainer;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A manager for GWT runtimes.
 */
public class GwtRuntimeManager implements GwtCorePreferenceConstants {
	private static String PREF_GWT_HOME = "gwtHome";

	private static final boolean logProblems = false;
	private static final INodeChangeListener listener = new INodeChangeListener() {

		public void added(final NodeChangeEvent event) {
			flush();
		}

		public void removed(final NodeChangeEvent event) {
			flush();
		}
	};

	private static AtomicReference<GwtRuntime[]> installedRuntimesRef = new AtomicReference<GwtRuntime[]>();
	private static final Lock initializationLock = new ReentrantLock();

	/**
	 * Returns the installed runtime with the specified name.
	 * 
	 * @param name
	 * @return the installed runtime (maybe <code>null</code>)
	 */
	public static GwtRuntime findInstalledRuntime(final String name) {
		final GwtRuntime[] runtimes = getInstalledRuntimes();
		for (final GwtRuntime gwtRuntime : runtimes) {
			if (name == null) {
				return gwtRuntime;
			} else if (gwtRuntime.getName().equals(name)) {
				return gwtRuntime;
			}
		}
		return null;
	}

	static void flush() {
		try {
			installedRuntimesRef.set(null);
			rebindClasspathEntries();
		} catch (final CoreException e) {
			if (logProblems) {
				GwtCore.logError("Exception while rebinding GWT runtime libraries. " + e.getMessage(), e);
			}
		}
	}

	private static IEclipsePreferences getGwtRuntimePreferencesNode() {
		return (IEclipsePreferences) new InstanceScope().getNode(GwtCore.PLUGIN_ID).node(GwtCorePreferenceConstants.PREF_GWT_RUNTIMES);
	}

	/**
	 * Returns a list of all installed runtimes.
	 * 
	 * @return a list of all installed runtimes
	 */
	public static GwtRuntime[] getInstalledRuntimes() {
		GwtRuntime[] installedRuntimes = installedRuntimesRef.get();
		while (null == installedRuntimes) {
			installedRuntimesRef.compareAndSet(null, loadInstalledRuntimes());
			installedRuntimes = installedRuntimesRef.get();
		}
		return installedRuntimes;
	}

	private static GwtRuntime[] loadInstalledRuntimes() {
		initializationLock.lock();
		try {
			// check if already initialized
			final GwtRuntime[] gwtRuntimes = installedRuntimesRef.get();
			if (null != gwtRuntimes) {
				return gwtRuntimes;
			}

			migrateOldPreferences();

			final IEclipsePreferences preferences = getGwtRuntimePreferencesNode();

			// hook change listener
			preferences.addNodeChangeListener(listener); // ok to be called multiple times

			// read runtimes
			String[] runtimeNames;
			try {
				runtimeNames = preferences.childrenNames();
			} catch (final BackingStoreException e) {
				GwtCore.logError("Error while loading GWT runtimes from preferences. " + e.getMessage(), e);
				return new GwtRuntime[0];
			}
			final List<GwtRuntime> runtimes = new ArrayList<GwtRuntime>(runtimeNames.length);
			for (final String name : runtimeNames) {
				final String location = preferences.node(name).get(GwtCorePreferenceConstants.PREF_LOCATION, null);
				if (null != location) {
					try {
						runtimes.add(new GwtRuntime(name, Path.fromPortableString(location)));
					} catch (final Exception e) {
						// ignore bogus entry
					}
				}
			}

			return runtimes.toArray(new GwtRuntime[runtimes.size()]);

		} finally {
			initializationLock.unlock();
		}
	}

	/**
	 * Migrates old preferences if necessary.
	 */
	private static void migrateOldPreferences() {
		final IEclipsePreferences preferences = new InstanceScope().getNode(GwtCore.PLUGIN_ID);
		final String oldGwtHome = preferences.get(PREF_GWT_HOME, null);
		if (null != oldGwtHome) {
			final IPath oldLocation = Path.fromPortableString(oldGwtHome);
			GwtRuntime gwtRuntime;
			try {
				gwtRuntime = new GwtRuntime(oldLocation.lastSegment(), oldLocation);
			} catch (final IllegalArgumentException e) {
				// invalid location
				gwtRuntime = new GwtRuntime("Migrated GWT", oldLocation);
			}

			try {
				saveRuntime(gwtRuntime);

				// remove old node
				preferences.remove(PREF_GWT_HOME);
				try {
					preferences.flush();
				} catch (final BackingStoreException e) {
					// don't fail, we may issue a warning in this case
				}
			} catch (final BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void rebindClasspathEntries() throws CoreException {
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IJavaProject[] projects = JavaCore.create(root).getJavaProjects();
		final IPath containerPath = new Path(GwtCore.GWT_CONTAINER);

		final List<IJavaProject> affectedProjects = new ArrayList<IJavaProject>();
		final List<IClasspathContainer> newContainers = new ArrayList<IClasspathContainer>();

		for (final IJavaProject project : projects) {
			final IClasspathEntry[] entries = project.getRawClasspath();
			for (final IClasspathEntry curr : entries) {
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					final IPath currPath = curr.getPath();
					if (containerPath.isPrefixOf(currPath)) {
						affectedProjects.add(project);
						GwtContainer container = null;
						if (currPath.segmentCount() > 1) {
							final GwtRuntime runtime = findInstalledRuntime(currPath.segment(1));
							if ((null != runtime) && !runtime.getLocation().isEmpty()) {
								container = new GwtContainer(runtime, containerPath);
							}
						}
						newContainers.add(container);
						break;
					}
				}
			}
		}
		if (!affectedProjects.isEmpty()) {
			final IJavaProject[] affected = affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
			final IClasspathContainer[] containers = newContainers.toArray(new IClasspathContainer[newContainers.size()]);
			JavaCore.setClasspathContainer(containerPath, affected, containers, null);

		}
	}

	private static void saveRuntime(final GwtRuntime runtime) throws BackingStoreException {
		final Preferences runtimeNode = getGwtRuntimePreferencesNode().node(runtime.getName());
		runtimeNode.put(GwtCorePreferenceConstants.PREF_LOCATION, runtime.getLocation().toPortableString());
		runtimeNode.flush();
	}

	/**
	 * Sets the active runtime
	 * 
	 * @param runtime
	 */
	public static void setActiveRuntime(final GwtRuntime runtime) throws Exception {
		// remove all existing runtimes
		final Preferences runtimeNode = getGwtRuntimePreferencesNode();
		for (final String name : runtimeNode.childrenNames()) {
			runtimeNode.node(name).removeNode();
		}

		// save new runtime
		saveRuntime(runtime);
	}

	/**
	 * Hidden
	 */
	private GwtRuntimeManager() {
		// empty
	}
}
