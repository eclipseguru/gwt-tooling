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
package org.eclipseguru.gwt.core.builder;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.GwtRemoteService;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.internal.codegen.AsyncServiceCodeGenerator;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.core.utils.ResourceUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.osgi.util.NLS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the builder for GWT projects.
 */
@SuppressWarnings("restriction")
public class GwtProjectBuilder extends IncrementalProjectBuilder {

	/**
	 * Finds remote services by visiting a resource delta.
	 */
	public static final class FindRemoteServicesVisitor implements IResourceDeltaVisitor {

		private final List<IType> changed;

		private GwtModule[] currentProjectModules;

		private GwtProject gwtProject;

		/**
		 * @param remoteServices
		 */
		public FindRemoteServicesVisitor(final List<IType> remoteServices) {
			changed = remoteServices;
		}

		/**
		 * @return the result
		 */
		public List<IType> getResult() {
			return changed;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		public boolean visit(final IResourceDelta delta) throws CoreException {
			// ignore removed resources
			switch (delta.getKind()) {
				case IResourceDelta.REMOVED:
				case IResourceDelta.REMOVED_PHANTOM:
					return false;
			}

			final IResource resource = delta.getResource();

			switch (resource.getType()) {
				case IResource.PROJECT:
					final IProject project = (IProject) resource;
					gwtProject = null;
					currentProjectModules = null;

					// check project nature
					if (!GwtProject.hasGwtNature(project)) {
						return false;
					}

					gwtProject = GwtCore.create(project);
					if (null != gwtProject) {
						currentProjectModules = gwtProject.getModules();
						return currentProjectModules.length > 0;
					}

					return false;

				case IResource.FOLDER:
					return true;

				case IResource.FILE:
					if (null == currentProjectModules) {
						return false;
					}

					if (JavaCore.isJavaLikeFileName(resource.getName()) && gwtProject.getJavaProject().isOnClasspath(resource)) {
						final ICompilationUnit cu = (ICompilationUnit) JavaCore.create(resource);
						if ((null != cu) && cu.exists()) {
							for (final GwtModule module : currentProjectModules) {
								if (module.isModuleResource(resource)) {
									GwtRemoteService.findRemoteServices(cu, changed);
								}
							}
						}
					}
					return false;
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IProject[] build(final int kind, final Map args, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			// initialize
			final boolean isIncrementalBuild = (kind == AUTO_BUILD) || (kind == INCREMENTAL_BUILD);
			final IProject project = getProject();
			monitor.beginTask(NLS.bind("Building project{0} ...", project.getName()), 10);

			// check for our nature
			if (!GwtProject.hasGwtNature(project)) {
				return null;
			}

			if (isIncrementalBuild) {
				// remove current project markers
				project.deleteMarkers(GwtCore.PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
				monitor.worked(1);
			} else {
				// do a complete clean on full builds
				clean(ProgressUtil.subProgressMonitor(monitor, 1));
			}

			// check for Java nature
			if (!project.isNatureEnabled(JavaCore.NATURE_ID)) {
				ResourceUtil.createProblem(project, MessageFormat.format("Project {0} is not properly configured as a Java project.", project.getName()));
				forgetLastBuiltState();
				return null;
			}

			// build list of included projects
			final GwtProject gwtProject = GwtCore.create(project);
			final GwtModule[] includedModules = gwtProject.getIncludedModules();
			final List<IProject> includedModulesProjects = new ArrayList<IProject>(includedModules.length);
			for (final GwtModule module : includedModules) {
				final IProject includedProject = module.getProjectResource();
				if (!includedModulesProjects.contains(includedProject)) {
					includedModulesProjects.add(includedProject);
				}
			}

			// find project modules
			final GwtModule[] projectModules = gwtProject.getModules();

			monitor.subTask("Building Remote services ...");

			// update/generate RemoteServiceAsync interfaces
			final IResourceDelta delta = isIncrementalBuild ? getDelta(project) : null;
			final List<IType> remoteServices = findRemoteServiceFiles(gwtProject, projectModules, delta, ProgressUtil.subProgressMonitor(monitor, 1));
			if (!remoteServices.isEmpty()) {
				updateAsyncFiles(remoteServices, ProgressUtil.subProgressMonitor(monitor, 1));
			}

			// compile modules if enabled
			if (GwtUtil.isAutoBuildModules(gwtProject)) {
				monitor.subTask("Compiling modules ...");
				compileProjectModules(gwtProject, projectModules, delta, ProgressUtil.subProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			// check modules
			monitor.subTask("Validating modules ...");
			checkProjectModules(projectModules, ProgressUtil.subProgressMonitor(monitor, 1));

			return includedModulesProjects.toArray(new IProject[includedModulesProjects.size()]);

		} catch (final Exception e) {
			forgetLastBuiltState();
			throw new CoreException(new Status(IStatus.ERROR, GwtCore.PLUGIN_ID, IResourceStatus.BUILD_FAILED, "An error occured during building: " + e.toString(), e));
		} finally {
			monitor.done();
		}
	}

	private void checkProjectModules(final GwtModule[] projectModules, IProgressMonitor monitor) throws GwtModelException, CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Validating", projectModules.length);
			for (final GwtModule gwtModule : projectModules) {
				monitor.setTaskName(gwtModule.getName());
				final IStorage moduleDescriptor = gwtModule.getModuleDescriptor();
				if (moduleDescriptor instanceof IResource) {
					try {
						final String entryPointTypeName = gwtModule.getEntryPointTypeName();
						if (null != entryPointTypeName) {
							final IType entryPointType = gwtModule.getEntryPointType();
							if (null == entryPointType) {
								ResourceUtil.createProblem((IResource) moduleDescriptor, MessageFormat.format("Entry point \"{0}\" could not be found on the project build path.", entryPointTypeName));
							}
						}
					} catch (final GwtModelException e) {
						ResourceUtil.createProblem((IResource) moduleDescriptor, MessageFormat.format("Error while analyzing module \"{0}\": {1} ", gwtModule.getModuleId(), e.getMessage()));
					}
				}
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			// initialize
			final IProject project = getProject();
			monitor.beginTask(NLS.bind("Cleaning project {0}", project.getName()), 10);

			// check for our nature
			if (!GwtProject.hasGwtNature(project)) {
				return;
			}

			// remove all markers
			project.deleteMarkers(GwtCore.PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			monitor.worked(1);

			// remove created build output
			final IPath outputLocation = GwtUtil.getOutputLocation(GwtCore.create(project));
			if (!outputLocation.makeRelative().isEmpty()) {
				final IFolder targetFolder = project.getFolder(outputLocation);
				if (targetFolder.exists()) {
					ResourceUtil.removeFolderContent(targetFolder, ProgressUtil.subProgressMonitor(monitor, 1));
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void compileProjectModules(final GwtProject gwtProject, final GwtModule[] projectModules, final IResourceDelta delta, final IProgressMonitor monitor) throws CoreException {
		// TODO we should be smart and support incremental builds
		try {
			new GwtProjectPublisher(gwtProject).runInWorkspace(monitor);
		} catch (final CoreException e) {
			ResourceUtil.createProblem(gwtProject.getProjectResource(), MessageFormat.format("Error while compiling modules: {0} ", e.getMessage()));
		}
	}

	private List<IType> findRemoteServiceFiles(final GwtProject gwtProject, final GwtModule[] projectModules, final IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Looking for Remote Service Files", 1);

			if (delta == null) {
				// full build
				return GwtRemoteService.findRemoteServices(projectModules);
			} else {
				// incremental build
				final List<IType> remoteServices = new ArrayList<IType>();
				delta.accept(new FindRemoteServicesVisitor(remoteServices));
				return remoteServices;
			}

		} finally {
			monitor.done();
		}
	}

	/**
	 * @param remoteService
	 * @param monitor
	 * @throws CoreException
	 */
	private void generateAsynchServiceInterface(final IType remoteService, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		ICompilationUnit asyncServiceCu = null;
		try {
			final IPackageFragment pack = remoteService.getPackageFragment();
			final String asyncServiceTypeName = AsyncServiceCodeGenerator.getAsyncTypeNameWithoutParameters(remoteService);
			final String asyncServiceCuName = asyncServiceTypeName.concat(".java");
			final AsyncServiceCodeGenerator generator = new AsyncServiceCodeGenerator(remoteService);

			monitor.beginTask(NLS.bind("{0}...", asyncServiceCuName), 10);

			// check that file is writable
			final IFile asyncServiceFile = ((IContainer) pack.getResource()).getFile(new Path(asyncServiceCuName));
			final IStatus canWrite = Resources.makeCommittable(asyncServiceFile, null);
			if (!canWrite.isOK()) {
				throw new CoreException(canWrite);
			}

			ProgressUtil.checkCanceled(monitor);

			// get the compilation unit
			asyncServiceCu = pack.getCompilationUnit(asyncServiceCuName);

			// check if overwrite is allowed
			if (!AsyncServiceCodeGenerator.isAllowedToGenerateAsyncServiceType(asyncServiceCu, asyncServiceTypeName)) {
				return;
			}

			// make cu a working copy
			asyncServiceCu.becomeWorkingCopy(ProgressUtil.subProgressMonitor(monitor, 1));

			// create the async service type
			generator.createType(asyncServiceCu, ProgressUtil.subProgressMonitor(monitor, 10));

			// save
			asyncServiceCu.commitWorkingCopy(true, ProgressUtil.subProgressMonitor(monitor, 1));

			// mark the resource as derived
			// TODO: this should be a preference
			// asyncServiceCu.getResource().setDerived(true);
		} finally {
			if (asyncServiceCu != null) {
				asyncServiceCu.discardWorkingCopy();
			}
			monitor.done();
		}
	}

	private void updateAsyncFiles(final List<IType> remoteServices, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Generating Async Remote Service files...", remoteServices.size());
			for (final IType remoteService : remoteServices) {
				ProgressUtil.checkCanceled(monitor);
				try {
					generateAsynchServiceInterface(remoteService, ProgressUtil.subProgressMonitor(monitor, 1));
				} catch (final CoreException e) {
					ResourceUtil.createProblem(remoteService.getResource(), NLS.bind("Could not generate async service interface for ''{0}'': ''{1}''", remoteService.getElementName(), e.getMessage()));
				}
			}
		} finally {
			monitor.done();
		}
	}
}
