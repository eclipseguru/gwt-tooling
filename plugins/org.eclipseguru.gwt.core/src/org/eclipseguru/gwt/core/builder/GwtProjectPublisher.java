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
 *     dobesv - contributed patch for issue 58
 *******************************************************************************/
package org.eclipseguru.gwt.core.builder;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.launch.GwtLaunchUtil;
import org.eclipseguru.gwt.core.runtimes.GwtRuntime;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.core.utils.ResourceUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.util.NLS;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Compiles and publishes GWT projects.
 */
@SuppressWarnings("restriction")
public class GwtProjectPublisher extends WorkspaceJob {

	/**
	 * Parses the output of the GWT compiler for compile errors.
	 */
	private final class CompileErrorParser implements IStreamListener {
		/** compileErrors */
		private final List<String> compileErrors = new ArrayList<String>();

		private final StringBuilder collectedOutput = new StringBuilder();

		private void analyzeOutput() {
			compileErrors.clear();
			try {
				final BufferedReader reader = new BufferedReader(new StringReader(collectedOutput.toString()));
				String text = null;
				while ((text = reader.readLine()) != null) {
					if (text.trim().startsWith("[ERROR] ")) {
						String errorMsg = text.trim().substring("[ERROR] ".length());
						errorMsg = stripNewlines(errorMsg);
						if (!ignoreError(errorMsg)) {
							compileErrors.add(errorMsg);
						}
					}
				}
			} catch (final IOException e) {
				compileErrors.add("Internal error while analyzing compile output: " + e.getMessage());
			}

		}

		/**
		 * Returns the collected compile errors.
		 * 
		 * @return the compile errors
		 */
		public List<String> getCompileErrors() {
			analyzeOutput();
			return Collections.unmodifiableList(compileErrors);
		}

		private boolean ignoreError(final String errorMsg) {
			if (errorMsg.equals("Build failed")) {
				return true;
			}
			if (errorMsg.equals("Failure while parsing XML")) {
				return true;
			}
			if (errorMsg.contains("Unexpected exception while processing element")) {
				return true;
			}
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.debug.core.IStreamListener#streamAppended(java.lang.String
		 * , org.eclipse.debug.core.model.IStreamMonitor)
		 */
		public void streamAppended(final String text, final IStreamMonitor monitor) {
			collectedOutput.append(text);
		}

		private String stripNewlines(final String errorMsg) {
			return errorMsg.replace('\r', ' ').replace('\n', ' ').trim();
		}
	}

	/** GWT_DEV_COMPILER */
	private static final String GWT_DEV_COMPILER_CLASS = "com.google.gwt.dev.Compiler";

	/** project */
	private final GwtProject project;

	public GwtProjectPublisher(final GwtModule module) {
		this(module.getProject());
	}

	public GwtProjectPublisher(final GwtProject project) {
		super(MessageFormat.format("GWT Compiling and Publishing {0}", project.getName()));
		this.project = project;

		// configure job
		setRule(ResourcesPlugin.getWorkspace().getRoot());
		setPriority(LONG);
	}

	/**
	 * Compiles the module.
	 * 
	 * @param module
	 * @param targetFolder
	 * @param monitor
	 * @throws CoreException
	 */
	private void compileModule(final GwtProject gwtProject, final GwtModule module, final IFolder targetFolder, final IProgressMonitor monitor) throws CoreException {
		// check for local install
		if (null == targetFolder.getLocation()) {
			throw new CoreException(GwtCore.newErrorStatus("Target Folder must be on the local filesystem!"));
		}

		// determine the marker resource
		final IResource markerResource = (module.getModuleDescriptor() instanceof IResource) ? (IResource) module.getModuleDescriptor() : module.getProjectResource();

		// remove module descriptor markers
		markerResource.deleteMarkers(GwtCore.PROBLEM_MARKER, true, IResource.DEPTH_ZERO);

		// we don't compile modules without an entry point
		if (null == module.getEntryPointTypeName()) {
			final IMarker marker = markerResource.createMarker(GwtCore.PROBLEM_MARKER);
			final Map<String, Object> attributes = new HashMap<String, Object>(2);
			attributes.put(IMarker.MESSAGE, NLS.bind("Module {0} could not be compiled because it does not specify a module entry point.", module.getModuleId()));
			attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
			marker.setAttributes(attributes);
			return;
		}

		// the lock for the external process
		final Lock compilerLaunchLock = new ReentrantLock();
		final Condition compilerLaunchFinishes = compilerLaunchLock.newCondition();

		// get project specific VM
		IVMInstall vmInstall = JavaRuntime.getVMInstall(gwtProject.getJavaProject());
		if (vmInstall == null) {
			// fallback to default VM
			vmInstall = JavaRuntime.getDefaultVMInstall();

			// fail if no VM is available
			if (vmInstall == null) {
				ResourceUtil.createProblem(gwtProject.getProjectResource(), "No JRE installed for launching the GWT compiler.");
				return;
			}
		}

		// get VM runner for executing the compiler
		final IVMRunner vmRunner = vmInstall.getVMRunner(ILaunchManager.RUN_MODE);
		if (vmRunner == null) {
			ResourceUtil.createProblem(gwtProject.getProjectResource(), NLS.bind("JRE \"{0}\" does not support launching external Java applications.", vmInstall.getName()));
			return;
		}

		// setup classpath
		final List<String> classpath = new ArrayList<String>();
		try {
			// source folders
			GwtLaunchUtil.addSourceFolderToClasspath(gwtProject, classpath, true);

			// we must insert GWT libs before the project classpath
			// http://code.google.com/p/gwt-tooling/issues/detail?id=31

			// GWT libraries
			final GwtRuntime runtime = GwtCore.getRuntime(gwtProject);
			final String[] gwtRuntimeClasspath = runtime.getGwtRuntimeClasspath();
			for (final String element : gwtRuntimeClasspath) {
				classpath.add(element);
			}

			// regular classpath
			final String[] projectClassPath = JavaRuntime.computeDefaultRuntimeClassPath(gwtProject.getJavaProject());
			classpath.addAll(Arrays.asList(projectClassPath));
		} catch (final CoreException e) {
			// unable to compute classpath
			ResourceUtil.createProblem(markerResource, NLS.bind("Unable to compile module {0}: {1}", module.getSimpleName(), e.toString()));
			return;
		}

		// we collect compile error messages
		final CompileErrorParser compileErrorLogger = new CompileErrorParser();

		// launch
		if (!classpath.isEmpty()) {
			final VMRunnerConfiguration vmConfig = new VMRunnerConfiguration(GWT_DEV_COMPILER_CLASS, classpath.toArray(new String[classpath.size()]));
			vmConfig.setWorkingDirectory(targetFolder.getLocation().toOSString());
			vmConfig.setProgramArguments(prepareGwtCompileArguments(module, targetFolder));
			vmConfig.setVMArguments(prepareGwtCompilerVmArguments(module));
			final ILaunch gwtLaunch = new Launch(null, ILaunchManager.RUN_MODE, null);
			DebugPlugin.getDefault().getLaunchManager().addLaunch(gwtLaunch);
			DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {
				public void handleDebugEvents(final DebugEvent[] events) {
					for (final DebugEvent event : events) {
						final Object source = event.getSource();
						if ((source instanceof IProcess)) {
							final IProcess process = (IProcess) source;
							final ILaunch launch = process.getLaunch();
							if ((launch != null) && (launch == gwtLaunch)) {
								if (event.getKind() == DebugEvent.CREATE) {
									process.getStreamsProxy().getOutputStreamMonitor().addListener(compileErrorLogger);
								} else if (event.getKind() == DebugEvent.TERMINATE) {
									process.getStreamsProxy().getOutputStreamMonitor().removeListener(compileErrorLogger);
									DebugPlugin.getDefault().removeDebugEventListener(this);

									// wakeup the publisher thread
									compilerLaunchLock.lock();
									try {
										compilerLaunchFinishes.signal();
									} finally {
										compilerLaunchLock.unlock();
									}
								}
							}
						}
					}
				}

			});

			compilerLaunchLock.lock();
			try {
				// launch the compiler
				vmRunner.run(vmConfig, gwtLaunch, monitor);

				// try to wait for the compiler
				monitor.subTask(MessageFormat.format("Waiting for the GWT Compiler to finish compiling module ''{0}''...", module.getName()));
				int i = 0;
				while (!gwtLaunch.isTerminated() && (i < 1800)) {
					ProgressUtil.checkCanceled(monitor);
					try {
						compilerLaunchFinishes.await(1, TimeUnit.SECONDS);
						i++;
					} catch (final InterruptedException e) {
						// ok;
						Thread.interrupted();
					}
				}
				if (!gwtLaunch.isTerminated()) {
					ResourceUtil.createProblem(markerResource, "GWT Compiler: Took too long (>" + i + " seconds) to complete. Compile results might be undefined.");
				}
			} finally {
				compilerLaunchLock.unlock();
			}

			// create marker for error message
			final List<String> compileErrors = compileErrorLogger.getCompileErrors();
			if (compileErrors.size() > 0) {
				for (final String error : compileErrors) {
					if (module.getModuleDescriptor() instanceof IResource) {
						ResourceUtil.createProblem(markerResource, NLS.bind("GWT Compiler: {0}", error));
					} else {
						ResourceUtil.createProblem(markerResource, NLS.bind("GWT Compiler, module {0}: {1}", module.getModuleId(), error));
					}
				}
			}

			// refresh
			targetFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}

	}

	/**
	 * Makes a list of files editable.
	 * 
	 * @param resources
	 * @return a status indicating if all files could be made editable
	 */
	private IStatus makeEditable(final List<IResource> resources) {
		return Resources.makeCommittable(resources.toArray(new IResource[resources.size()]), null);
	}

	/**
	 * Builds the compile arguments for compiling the specified module into the
	 * specified target folder.
	 * 
	 * @param module
	 * @param targetFolder
	 * @return the program arguments
	 * @throws CoreException
	 */
	private String[] prepareGwtCompileArguments(final GwtModule module, final IFolder targetFolder) throws CoreException {

		final List<String> args = new ArrayList<String>();

		args.add("-logLevel");
		args.add("INFO");

		// TODO: what's about gen? another preference?
		args.add("-gen");
		args.add(targetFolder.getLocation().append(".gen").toOSString());

		args.add("-war");
		args.add(targetFolder.getLocation().toOSString());

		args.add("-style");
		args.add(GwtUtil.getCompilerJavascriptStyle(module.getProject()));

		args.add(module.getModuleId());

		return args.toArray(new String[args.size()]);
	}

	/**
	 * Builds the additional JavaVM arguments for compiling the specified
	 * module.
	 * 
	 * @param module
	 * @return the VM arguments
	 * @throws CoreException
	 */
	private String[] prepareGwtCompilerVmArguments(final GwtModule module) throws CoreException {
		final List<String> vmArgs = new ArrayList<String>();

		// configured arguments first
		vmArgs.addAll(Arrays.asList(DebugPlugin.parseArguments(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(GwtUtil.getCompilerVmArgs(module.getProject())))));

		// GWT runtime entries only if not already present
		final GwtRuntime runtime = GwtCore.getRuntime(module.getProject());
		final String[] runtimeVmArgs = runtime.getGwtRuntimeVmArgs();
		for (final String arg : runtimeVmArgs) {
			if (!vmArgs.contains(arg)) {
				vmArgs.add(arg);
			}
		}

		return vmArgs.toArray(new String[vmArgs.size()]);
	}

	/**
	 * Publishes included modules.
	 * 
	 * @param gwtProject
	 * @param targetFolder
	 * @param modules
	 * @param monitor
	 * @throws CoreException
	 */
	private void publishAndCompileModules(final GwtProject gwtProject, final IFolder targetFolder, final GwtModule[] modules, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("Publishing GWT modules ...", modules.length);

			// make sure that we can modify all resources in the target folder
			final List<IResource> resources = new ArrayList<IResource>();
			targetFolder.accept(new IResourceVisitor() {
				public boolean visit(final IResource resource) throws CoreException {
					resources.add(resource);
					return true;
				}
			});
			final IStatus canWrite = makeEditable(resources);
			if (!canWrite.isOK()) {
				throw new CoreException(canWrite);
			}

			// compile the modules
			for (final GwtModule module : modules) {
				final IPackageFragment modulePackage = module.getModulePackage();
				if (null != modulePackage) {
					monitor.subTask(module.getModuleId());
					// TODO: things changed in GWT 1.1
					// we need a "smart" publish that just generates
					// the (module.nocache.html)
					// http://groups.google.com/group/Google-Web-Toolkit/browse_thread/thread/aa3a8d942e493c26/69a1a5689cb56e2b#69a1a5689cb56e2b
					//if (false) {
					//	publishHostedModuleFull(module, targetFolder, ProgressUtil.subProgressMonitor(monitor, 1));
					//}
					compileModule(gwtProject, module, targetFolder, ProgressUtil.subProgressMonitor(monitor, 1));

				} else {
					ResourceUtil.createProblem(gwtProject.getProjectResource(), NLS.bind("Could not resolve module ''{0}''.", module.getModuleId()));
				}
			}

			// mark all generated resources as derived
			final String lineSeparator = GwtUtil.getLineSeparator(gwtProject.getProjectResource());
			targetFolder.accept(new IResourceVisitor() {
				public boolean visit(final IResource resource) throws CoreException {

					// mark derived
					resource.setDerived(true);

					// fix the line endings
					if (resource.getType() == IResource.FILE) {
						final IFile file = (IFile) resource;
						final String name = file.getName().toLowerCase();
						if (name.endsWith(".cache.html") || name.endsWith(".cache.xml") || name.endsWith(".nocache.html")) {
							final InputStream contents = file.getContents();
							try {
								final BufferedReader reader = new BufferedReader(new InputStreamReader(contents, file.getCharset()));
								final StringBuilder newContents = new StringBuilder(50000);
								while (reader.ready()) {
									newContents.append(reader.readLine()).append(lineSeparator);
								}
								file.setContents(new ByteArrayInputStream(newContents.toString().getBytes(file.getCharset())), IResource.NONE, null);
							} catch (final IOException e) {
								// ignore, we don't care if there is such a problem
							} finally {
								try {
									contents.close();
								} catch (final IOException e) {
									//  ignore
								}
							}
						}
					}

					return true;
				}
			});

		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Publishing GWT project " + project.getName(), 10);
			// initialize
			final GwtModule[] includedModules = project.getIncludedModules();
			final List<IProject> includedModulesProjects = new ArrayList<IProject>(includedModules.length);
			for (final GwtModule module : includedModules) {
				final IProject includedProject = module.getProjectResource();
				if (!includedModulesProjects.contains(includedProject)) {
					includedModulesProjects.add(includedProject);
				}
			}

			// remove project markers
			project.getProjectResource().deleteMarkers(GwtCore.PROBLEM_MARKER, true, IResource.DEPTH_ZERO);

			// find project modules
			final GwtModule[] projectModules = project.getModules();

			// publish modules
			if ((projectModules.length > 0) || (includedModules.length > 0)) {
				// check output folder
				final IPath outputLocation = GwtUtil.getOutputLocation(project);
				if (outputLocation.makeRelative().isEmpty()) {
					ResourceUtil.createProblem(project.getProjectResource(), "The GWT build output folder is mapped to the project root which is not yet supported!");
				} else {
					// initialize output folder
					final IFolder targetFolder = project.getProjectResource().getFolder(outputLocation);
					if (!targetFolder.exists()) {
						ResourceUtil.createFolderHierarchy(targetFolder, ProgressUtil.subProgressMonitor(monitor, 1));
					}

					monitor.subTask("Publishing Modules ...");

					// clean the target folder
					// ResourceUtil.removeFolderContent(targetFolder,
					// ProgressUtil.subProgressMonitor(monitor, 1));

					/*
					 * In hosted mode: - copy module public files only In
					 * compiled mode: - copy module public files and - compiled
					 * module output Always: - check that support files exist in
					 * build output directory (gwt-hosted.jsp, gwt.js,
					 * history.html, gwt-user.jar) - check that the build output
					 * folder and the gwt-user.jar is correctly registered with
					 * deployment path in flex project component file
					 */

					// publish my modules
					publishAndCompileModules(project, targetFolder, projectModules, ProgressUtil.subProgressMonitor(monitor, 1));

					// publish included module
					publishAndCompileModules(project, targetFolder, includedModules, ProgressUtil.subProgressMonitor(monitor, 1));
				}
			}

			return Status.OK_STATUS;

		} catch (final CoreException e) {
			return new MultiStatus(GwtCore.PLUGIN_ID, IResourceStatus.BUILD_FAILED, new IStatus[] { e.getStatus() }, MessageFormat.format("An error occured during publishing of project {0}.", project.getName()), null);
		} catch (final OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, GwtCore.PLUGIN_ID, IResourceStatus.BUILD_FAILED, MessageFormat.format("An error occured during publishing of project {0}.", project.getName()), e);
		} finally {
			monitor.done();
		}
	}

}
