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
package org.eclipseguru.gwt.core;

import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * A core utility
 */
public class GwtUtil {

	/** DEFAULT_PACKAGE */
	private static final String DEFAULT_PACKAGE_NAME = ""; //$NON-NLS-1$

	/** SYSTEM_LINE_SEPARATOR */
	public static String SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	/** DEFAULT_OUTPUT_LOCATION */
	public static final String DEFAULT_OUTPUT_LOCATION = "bin.gwt"; //$NON-NLS-1$

	/** GWT_MODULE_SOURCE_EXTENSION */
	public static final String GWT_MODULE_SOURCE_EXTENSION = ".gwt.xml"; //$NON-NLS-1$

	/** NO_VM_ARGS */
	private static final String NO_VM_ARGS = ""; //$NON-NLS-1$

	/**
	 * Returns the GWT compiler Java VM arguments for the specified project.
	 * <p>
	 * Note, the contents are added to the GWT compiler Java command as
	 * additional VM arguments
	 * </p>
	 * 
	 * @param project
	 * @return the GWT compiler VM arguments deployment for the specified
	 *         project
	 */
	public static String getCompilerVmArgs(final GwtProject project) {
		final IEclipsePreferences projectPreferences = project.getProjectPreferences();
		if (null == projectPreferences) {
			return NO_VM_ARGS;
		}

		return projectPreferences.get(GwtCorePreferenceConstants.PREF_COMPILER_VM_ARGS, NO_VM_ARGS);
	}

	/**
	 * Returns the deployment path for the specified project.
	 * <p>
	 * Note, the deployment path is a virtual directory where the GWT will be
	 * deployed in the web application.
	 * </p>
	 * 
	 * @param project
	 * @return the deployment path for the specified project
	 */
	public static IPath getDeploymentPath(final GwtProject project) {
		final IEclipsePreferences projectPreferences = project.getProjectPreferences();
		final String deploymentPathString = null != projectPreferences ? projectPreferences.get(GwtCorePreferenceConstants.PREF_DEPLOYMENT_PATH, null) : null;
		if (null != deploymentPathString) {
			return new Path(deploymentPathString).makeAbsolute();
		}

		return new Path("/gwt");
	}

	/**
	 * Returns the line separator for the given project. If the project is null,
	 * returns the line separator for the workspace. If still null, return the
	 * system line separator.
	 */
	public static String getLineSeparator(final IProject project) {
		String lineSeparator = null;

		// line delimiter in project preference
		IScopeContext[] scopeContext;
		if (project != null) {
			scopeContext = new IScopeContext[] { new ProjectScope(project) };
			lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineSeparator != null) {
				return lineSeparator;
			}
		}

		// line delimiter in workspace preference
		scopeContext = new IScopeContext[] { new InstanceScope() };
		lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
		if (lineSeparator != null) {
			return lineSeparator;
		}

		// system line delimiter
		return SYSTEM_LINE_SEPARATOR;
	}

	/**
	 * Returns the location for output produced during builds for the specified
	 * project.
	 * <p>
	 * The returned path is relativ to the project.
	 * </p>
	 * 
	 * @param gwtProject
	 * @return the location for build output
	 */
	public static IPath getOutputLocation(final GwtProject gwtProject) {
		// prefere configures output location
		final IEclipsePreferences projectPreferences = gwtProject.getProjectPreferences();
		final String outputPathString = null != projectPreferences ? projectPreferences.get(GwtCorePreferenceConstants.PREF_OUTPUT_LOCATION, null) : null;
		if (null != outputPathString) {
			return new Path(outputPathString).makeRelative();
		}

		// fallback to default
		return new Path(DEFAULT_OUTPUT_LOCATION).makeRelative();
	}

	/**
	 * Returns the package name part (everything before the last dot) of the
	 * specified module id.
	 * 
	 * @param moduleId
	 * @return the package name part
	 */
	public static String getPackageName(final String moduleId) {
		if (null == moduleId) {
			return null;
		}

		final int lastDot = moduleId.lastIndexOf('.');
		if (lastDot != -1) {
			return moduleId.substring(0, lastDot);
		}

		return DEFAULT_PACKAGE_NAME;
	}

	/**
	 * Returns the simple name (everything after the last dot) of the specified
	 * storage.
	 * <p>
	 * Returns <code>null</code>l if the storage name does not appear to be a
	 * valid GWT module source name.
	 * </p>
	 * 
	 * @param storage
	 * @return the simple name (maybe <code>null</code>)
	 */
	public static String getSimpleName(final IStorage storage) {
		if (null == storage) {
			return null;
		}

		final String name = storage.getName();
		if ((null == name) || !name.endsWith(GWT_MODULE_SOURCE_EXTENSION)) {
			return null;
		}

		return name.substring(0, name.length() - GWT_MODULE_SOURCE_EXTENSION.length());
	}

	/**
	 * Returns the simple name (everything after the last dot) of the specified
	 * module id.
	 * 
	 * @param moduleId
	 * @return the simple name
	 */
	public static String getSimpleName(final String moduleId) {
		if (null == moduleId) {
			return null;
		}

		final int lastDot = moduleId.lastIndexOf('.');
		if ((lastDot != -1) && (moduleId.length() > lastDot)) {
			return moduleId.substring(lastDot + 1);
		}

		return moduleId;
	}

	/**
	 * Returns the type name without any parameters of generic type information.
	 * 
	 * @param typeName
	 * @return
	 */
	public static String getTypeNameWithoutParameters(final String typeName) {
		final String typeNameWithParameters = typeName;
		final int angleBracketOffset = typeNameWithParameters.indexOf('<');
		if (angleBracketOffset == -1) {
			return typeNameWithParameters;
		} else {
			return typeNameWithParameters.substring(0, angleBracketOffset);
		}
	}

	/**
	 * Indicates if a project has the specified facet assigned.
	 * 
	 * @param project
	 * @return <code>true</code> if a project has the facet assigned,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasFacet(final IProject project, final String facteId) {
		try {
			final IFacetedProject facetsManager = ProjectFacetsManager.create(project);
			if (null == facetsManager) {
				return false;
			}

			return facetsManager.hasProjectFacet(ProjectFacetsManager.getProjectFacet(facteId));
		} catch (final CoreException e) {
			// fail gracefully
			return false;
		}
	}

	/**
	 * Indicates if the project is configured to build modules automatically.
	 * 
	 * @param project
	 * @return <code>true</code> if the project is configured to build modules
	 *         automatically, <code>false</code> if in compiled mode
	 */
	public static boolean isAutoBuildModules(final GwtProject project) {
		final IEclipsePreferences projectPreferences = project.getProjectPreferences();
		if (null == projectPreferences) {
			return false;
		}

		return projectPreferences.getBoolean(GwtCorePreferenceConstants.PREF_AUTO_BUILD_MODULES, false);
	}

	/**
	 * Indicates if the project is deployed in hosted mode.
	 * 
	 * @param project
	 * @return <code>true</code> if the project is deployed in hosted mode,
	 *         <code>false</code> if in compiled mode
	 */
	public static boolean isHostedDeploymentMode(final GwtProject project) {
		final IEclipsePreferences projectPreferences = project.getProjectPreferences();
		if (null == projectPreferences) {
			return false;
		}

		return projectPreferences.getBoolean(GwtCorePreferenceConstants.PREF_HOSTED_DEPLOY_MODE, true);
	}

	/**
	 * Indicates if the resource is a GWT module descriptor.
	 * 
	 * @param resource
	 * @return <code>true</code> if the resource is a GWT module descriptor,
	 *         <code>false</code> otherwise
	 */
	public static boolean isModuleDescriptor(final IResource resource) {
		if ((resource != null) && (resource.getType() == IResource.FILE) && resource.getName().toLowerCase().endsWith(GWT_MODULE_SOURCE_EXTENSION)) {
			return true;
		}
		return false;
	}

	/**
	 * Hidden constructor.
	 */
	private GwtUtil() {
		// ignore
	}
}
