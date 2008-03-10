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
package org.eclipseguru.gwt.core.launch;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.runtimes.GwtRuntime;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A launch delegate for launching the GWT browser.
 */
public class GwtBrowserLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate, GwtLaunchConstants {

	/** CLASS_NAME_GWTSHELL */
	private static final String CLASS_NAME_GWTSHELL = "com.google.gwt.dev.GWTShell";

	/**
	 * Computes the classpath for the specified project and configuration.
	 * 
	 * @param project
	 * @param configuration
	 * @return
	 * @throws CoreException
	 */
	private String[] computeClasspath(final GwtProject project, final ILaunchConfiguration configuration) throws CoreException {
		final List<String> classpath = new ArrayList<String>();

		// add the source folder to the classpath
		GwtLaunchUtil.addSourceFolderToClasspath(project, classpath, true);

		// GWT runtime entries
		final GwtRuntime runtime = GwtCore.getRuntime(project);
		final String[] entries = runtime.getGwtRuntimeClasspath();
		classpath.addAll(Arrays.asList(entries));

		// launch config classpath
		classpath.addAll(Arrays.asList(getClasspath(configuration)));

		return classpath.toArray(new String[classpath.size()]);
	}

	/**
	 * Computes the classpath for the specified project and configuration.
	 * 
	 * @param project
	 * @param configuration
	 * @return
	 * @throws CoreException
	 */
	private String[] computeVmArgs(final GwtProject project, final ILaunchConfiguration configuration) throws CoreException {
		final List<String> vmArgs = new ArrayList<String>();

		// launch config arguments first
		vmArgs.addAll(Arrays.asList(DebugPlugin.parseArguments(getVMArguments(configuration))));

		// GWT runtime entries only if not already present
		final GwtRuntime runtime = GwtCore.getRuntime(project);
		final String[] runtimeVmArgs = runtime.getGwtRuntimeVmArgs();
		for (final String arg : runtimeVmArgs) {
			if (!vmArgs.contains(arg)) {
				vmArgs.add(arg);
			}
		}

		return vmArgs.toArray(new String[vmArgs.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration,
	 *      java.lang.String, org.eclipse.debug.core.ILaunch,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {

		monitor = ProgressUtil.monitor(monitor);
		try {

			monitor.beginTask(MessageFormat.format("{0}...", configuration.getName()), 3); //$NON-NLS-1$

			// check for cancellation
			if (monitor.isCanceled())
				return;

			monitor.subTask("Verifying launch attributes...");

			// verify project
			final GwtProject project = GwtLaunchUtil.verifyProject(configuration);
			final IVMRunner runner = getVMRunner(configuration, mode);

			// working directory
			final File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}

			// Environment variables
			final String[] envp = getEnvironment(configuration);

			// Program & VM arguments
			final String[] pgmArgs = GwtLaunchUtil.getGwtShellArguments(configuration);
			final String[] vmArgs = computeVmArgs(project, configuration);

			// VM-specific attributes
			final Map vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			final String[] classpath = computeClasspath(project, configuration);

			// Create VM config
			final VMRunnerConfiguration runConfig = new VMRunnerConfiguration(CLASS_NAME_GWTSHELL, classpath);
			runConfig.setProgramArguments(pgmArgs);
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(vmArgs);
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled())
				return;

			// stop in main
			prepareStopInMain(configuration);

			// done the verification phase
			monitor.worked(1);

			monitor.subTask("Creating source locator...");
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled())
				return;

		} finally {
			monitor.done();
		}

	}

}
