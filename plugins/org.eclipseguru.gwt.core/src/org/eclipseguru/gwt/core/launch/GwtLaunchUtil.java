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
package org.eclipseguru.gwt.core.launch;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.GwtUtil;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility object for launching GWT.
 */
public class GwtLaunchUtil implements GwtLaunchConstants {

	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 * @throws CoreException
	 *             the "abort" core exception
	 */
	public static void abort(final String message, final Throwable exception, final int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, GwtCore.PLUGIN_ID, code, message, exception));
	}

	/**
	 * Adds the source folder from the specified project to the classpath list.
	 * 
	 * @param gwtProject
	 * @param classpath
	 * @param processReferencedProjects
	 * @throws JavaModelException
	 */
	public static void addSourceFolderToClasspath(final GwtProject gwtProject, final List<String> classpath, final boolean processReferencedProjects) throws JavaModelException {
		if ((null == gwtProject) || !gwtProject.exists()) {
			return;
		}

		addSourceFolderToClasspath(gwtProject.getJavaProject(), classpath);

		if (processReferencedProjects) {
			final String[] requiredProjectNames = gwtProject.getJavaProject().getRequiredProjectNames();
			if (requiredProjectNames.length > 0) {
				final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (final String requiredProjectName : requiredProjectNames) {
					final IJavaProject project = JavaCore.create(root.getProject(requiredProjectName));
					addSourceFolderToClasspath(project, classpath);
				}
			}
		}
	}

	/**
	 * Adds the source folder from the specified project to the classpath list.
	 * 
	 * @param gwtProject
	 * @param classpath
	 * @throws JavaModelException
	 */
	private static void addSourceFolderToClasspath(final IJavaProject project, final List<String> classpath) throws JavaModelException {
		if ((null == project) || !project.exists()) {
			return;
		}

		final IPackageFragmentRoot[] packageFragmentRoots = project.getPackageFragmentRoots();
		for (final IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
			if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
				classpath.add(packageFragmentRoot.getResource().getLocation().toOSString());
			}
		}
	}

	/**
	 * Returns a default URL for the module with the specified id.
	 * <p>
	 * Note, the URL is not guaranteed to work on all systems and for all
	 * modules.
	 * </p>
	 * 
	 * @param module
	 *            the GWT module
	 * @return the default URL (maybe <code>null</code> if the specified module
	 *         was <code>null</code>)
	 * @throws CoreException
	 */
	public static String computeDefaultUrl(final GwtModule module) throws CoreException {
		if (null == module) {
			return null;
		}
		final String alternateName = module.getAlternateName();
		if ((null != alternateName) && (alternateName.trim().length() > 0)) {
			return alternateName + "/" + module.getSimpleName() + ".html";
		}

		return module.getModuleId() + "/" + module.getSimpleName() + ".html";
	}

	private static String computeStartupUrl(final ILaunchConfiguration configuration, final GwtProject project, final String moduleId) throws CoreException, GwtModelException {
		final boolean customUrl = getCustomUrl(configuration);
		String url = customUrl ? getUrl(configuration) : null;
		if ((null == url) || (url.trim().length() == 0)) {
			final GwtModule module = project.getModule(moduleId);
			if (module == null) {
				abort("Module not found", null, GwtLaunchConstants.ERR_UNSPECIFIED_MODULE_ID);
			}
			url = computeDefaultUrl(module);
		}
		return url;
	}

	/**
	 * Creates and returns a new GWT browser launch configuration for the
	 * specified module.
	 * 
	 * @param module
	 *            the module to launch
	 * @param url
	 *            the url to launch (optional, maybe <code>null</code>)
	 * @param noserver
	 *            <code>true</code> if the GWT integrated server should not be
	 *            started, <code>false</code> otherwise
	 * @return the creates launch configuration
	 */
	public static ILaunchConfiguration createGwtBrowserLaunchConfiguration(final GwtModule module, final String url, final boolean noserver) {
		ILaunchConfiguration config = null;
		try {
			final ILaunchConfigurationType configType = getGwtBrowserLaunchConfigurationType();
			final ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(module.getSimpleName()));
			wc.setAttribute(ATTR_PROJECT_NAME, module.getProjectName());
			wc.setAttribute(ATTR_MODULE_ID, module.getModuleId());
			wc.setAttribute(ATTR_NOSERVER, noserver);
			if ((null != url) && (url.trim().length() > 0)) {
				wc.setAttribute(ATTR_CUSTOM_URL, true);
				wc.setAttribute(ATTR_URL, url);
			} else {
				wc.setAttribute(ATTR_CUSTOM_URL, false);
			}
			config = wc.doSave();
		} catch (final CoreException e) {
			GwtCore.logError(MessageFormat.format("Error while creating GWT Browser launch configuration for module {0}", module.getModuleId()), e);
		}
		return config;
	}

	/**
	 * Returns the custom url option specified by the given launch
	 * configuration, defaults to <code>false</code>.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the custom url option specified by the given launch
	 *         configuration, or <code>false</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static boolean getCustomUrl(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_CUSTOM_URL, false);
	}

	/**
	 * Returns the {@link GwtLaunchConstants#TYPE_GWT_BROWSER} launch
	 * configuration type.
	 * 
	 * @return the {@link GwtLaunchConstants#TYPE_GWT_BROWSER} launch
	 *         configuration type
	 */
	public static ILaunchConfigurationType getGwtBrowserLaunchConfigurationType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(GwtLaunchConstants.TYPE_GWT_BROWSER);
	}

	/**
	 * Returns the arguments for launching the GWT Shell with the specified
	 * configuration.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the command line arguments
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static String[] getGwtDevShellArguments(final ILaunchConfiguration configuration) throws CoreException {
		final List<String> args = new ArrayList<String>();

		// read project settings
		final GwtProject project = verifyProject(configuration);
		final String moduleId = verifyModuleId(configuration);
		final IPath outputLocation = GwtUtil.getOutputLocation(project);
		final IFolder targetFolder = project.getProjectResource().getFolder(outputLocation);

		// log level
		final String logLevel = getLogLevel(configuration);
		if ((null != logLevel) && (logLevel.trim().length() > 0)) {
			args.add("-logLevel");
			args.add(logLevel);
		}

		// java script style
		//		final String style = getStyle(configuration);
		//		if ((null != style) && (style.trim().length() > 0)) {
		//			args.add("-style");
		//			args.add(style);
		//		}

		// port or noserver option
		final boolean noserver = getNoServer(configuration);
		if (noserver) {
			args.add("-noserver");
		} else {
			// port
			args.add("-port");
			args.add(String.valueOf(getPort(configuration)));
		}

		// folder for generated stuff
		args.add("-gen");
		args.add(targetFolder.getLocation().append(".gen").toOSString());

		// output folder
		args.add("-war");
		args.add(targetFolder.getLocation().toOSString());

		// url
		args.add("-startupUrl");
		args.add(computeStartupUrl(configuration, project, moduleId));

		// module id
		args.add(moduleId);

		return args.toArray(new String[args.size()]);
	}

	/**
	 * Returns the log level specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the log level specified by the given launch configuration, or
	 *         <code>null</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static String getLogLevel(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_LOG_LEVEL, (String) null);
	}

	/**
	 * Returns the GWT module id specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @param project
	 *            the GWT project to lookup the module id
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static String getModuleId(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_MODULE_ID, (String) null);
	}

	/**
	 * Returns the noserver option specified by the given launch configuration,
	 * or <code>false</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the noserver option specified by the given launch configuration,
	 *         or <code>false</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static boolean getNoServer(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_NOSERVER, false);
	}

	/**
	 * Returns the port specified by the given launch configuration, defaults to
	 * <code>8888</code>.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the port specified by the given launch configuration, defaults to
	 *         <code>8888</code>
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static int getPort(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_PORT, 8888);
	}

	/**
	 * Returns the GWT project specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the GWT project specified by the given launch configuration, or
	 *         <code>null</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static GwtProject getProject(final ILaunchConfiguration configuration) throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if (projectName != null) {
			projectName = projectName.trim();
			if (projectName.length() > 0) {
				final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				final GwtProject gwtProject = GwtCore.create(project);
				if ((gwtProject != null) && gwtProject.exists()) {
					return gwtProject;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the style specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the style specified by the given launch configuration, or
	 *         <code>null</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static String getStyle(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_STYLE, (String) null);
	}

	/**
	 * Returns the url specified by the given launch configuration, or
	 * <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the port specified by the given launch configuration, or
	 *         <code>null</code> if none
	 * @throws CoreException
	 *             if unable to retrieve the attribute
	 */
	public static String getUrl(final ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_URL, (String) null);
	}

	/**
	 * Verifies a module id is specified by the given launch configuration, and
	 * returns the module id.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the module id specified by the given launch configuration
	 * @throws CoreException
	 *             if unable to retrieve the attribute or the attribute is
	 *             unspecified
	 */
	public static String verifyModuleId(final ILaunchConfiguration configuration) throws CoreException {
		final String moduleId = getModuleId(configuration);
		if ((moduleId == null) || (moduleId.trim().length() == 0)) {
			abort("Module id not specified", null, GwtLaunchConstants.ERR_UNSPECIFIED_MODULE_ID);
		}
		return moduleId;
	}

	/**
	 * Verifies a project is specified by the given launch configuration, and
	 * returns the project.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the project specified by the given launch configuration
	 * @throws CoreException
	 *             if unable to retrieve the attribute or the attribute is
	 *             unspecified
	 */
	public static GwtProject verifyProject(final ILaunchConfiguration configuration) throws CoreException {
		final GwtProject project = getProject(configuration);
		if (project == null) {
			abort("Project not specified", null, GwtLaunchConstants.ERR_UNSPECIFIED_PROJECT);
		}
		return project;
	}

	/**
	 * Hidden constructor
	 */
	private GwtLaunchUtil() {
		// hidden
	}
}
