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
package org.eclipseguru.gwt.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.List;

/**
 * A singletong model manager
 */
class GwtModelManager {

	/** NO_MODULES */
	static final GwtModule[] NO_MODULES = new GwtModule[0];

	/** MANAGER */
	private static final GwtModelManager MANAGER = new GwtModelManager();

	/**
	 * Creates a binary module.
	 * 
	 * @param resource
	 * @param packageFragment
	 * @return
	 */
	public static GwtModule createBinaryModule(final IStorage resource, final IPackageFragment packageFragment) {
		if (resource == null)
			return null;

		if (null == GwtUtil.getSimpleName(resource))
			return null;

		final GwtProject project = getModelManager().getModel().createProject(packageFragment.getJavaProject().getProject());
		if (null == project)
			return null;

		return project.createBinaryModule(resource, packageFragment);
	}

	/**
	 * Returns the GWT module corresponding to the given file, its project being
	 * the given project. Returns <code>null</code> if unable to associate the
	 * given file with a GWT module.
	 */
	public static GwtModule createModule(final IFile file, GwtProject project) {
		if (file == null)
			return null;

		if (!GwtUtil.isModuleDescriptor(file))
			return null;

		if (project == null)
			project = getModelManager().getModel().createProject(file);

		return project.createModule(file);
	}

	/**
	 * Returns the GWT remote service corresponding to the given type, its
	 * module being the given module. Returns <code>null</code> if unable to
	 * associate the given type with a GWT remote service.
	 */
	public static GwtRemoteService createRemoteService(final IType type, GwtModule module) {
		if (type == null)
			return null;

		try {
			if (!GwtRemoteService.isRemoteService(type))
				return null;

			if (module == null) {
				final IResource resource = type.getResource();
				if (null == resource)
					// TODO: support types in external archives
					return null;

				final GwtProject project = getModelManager().getModel().createProject(resource);
				final GwtModule[] modules = project.getModules();
				for (final GwtModule module2 : modules)
					if (module2.isModuleResource(resource)) {
						module = module2;
						break;
					}

			}

			// no module found
			if (null == module)
				return null;

			return module.createRemoteService(type);
		} catch (final CoreException e) {
			// unable to recognize
			return null;
		}
	}

	/**
	 * Finds a module in a given package.
	 * 
	 * @param packageFragment
	 * @param moduleId
	 * @param modules
	 * @throws JavaModelException
	 */
	private static boolean findModuleInPackage(final IPackageFragment packageFragment, final String moduleId, final List<GwtModule> modules) throws JavaModelException {
		// check package first
		if (!packageFragment.getElementName().equals(GwtUtil.getPackageName(moduleId)))
			return false;

		// find module source
		final String gwtFileName = GwtUtil.getSimpleName(moduleId).concat(GwtUtil.GWT_MODULE_SOURCE_EXTENSION);
		final Object[] nonJavaResources = packageFragment.getNonJavaResources();
		for (final Object nonJavaResource : nonJavaResources)
			// project resource
			if (nonJavaResource instanceof IResource) {
				final IResource resource = (IResource) nonJavaResource;
				if (GwtUtil.isModuleDescriptor(resource) && resource.getName().equals(gwtFileName)) {
					final GwtModule module = createModule((IFile) resource, getModelManager().getModel().createProject(resource));
					if (null != module) {
						modules.add(module);
						return true;
					}
				}
			}

			// jar entry
			else if (nonJavaResource instanceof IStorage) {
				final IStorage resource = (IStorage) nonJavaResource;
				if (gwtFileName.equals(resource.getName())) {
					final GwtModule module = createBinaryModule(resource, packageFragment);
					if (null != module) {
						modules.add(module);
						return true;
					}
				}
			}

		return false;
	}

	/**
	 * Finds the module with the specified id using the projects classpath.
	 * 
	 * @param moduleId
	 * @param project
	 * @return
	 * @throws GwtModelException
	 */
	public static GwtModule[] findModules(final String[] moduleIds, final GwtProject project) throws GwtModelException {
		if ((null == moduleIds) || (null == project))
			return null;

		if (moduleIds.length == 0)
			return NO_MODULES;

		final List<GwtModule> modules = new ArrayList<GwtModule>(moduleIds.length);
		final IJavaProject javaProject = project.getJavaProject();

		try {
			final IPackageFragment[] packageFragments = javaProject.getPackageFragments();
			if (packageFragments.length > moduleIds.length)
				for (final IPackageFragment fragment : packageFragments)
					for (final String moduleId : moduleIds)
						findModuleInPackage(fragment, moduleId, modules);
			else
				for (final String moduleId : moduleIds)
					for (final IPackageFragment fragment : packageFragments)
						findModuleInPackage(fragment, moduleId, modules);

		} catch (final JavaModelException e) {
			throw new GwtModelException(e.getStatus());
		}

		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns the singleton model manager instance.
	 * 
	 * @return the model manager instance
	 */
	public static GwtModelManager getModelManager() {
		return MANAGER;
	}

	/** model */
	private final GwtModel model = new GwtModel();

	/**
	 * @return the model
	 */
	public GwtModel getModel() {
		return model;
	}

	/**
	 * Stops the model manager
	 */
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	/**
	 * Starts the model manager
	 */
	public void startup() {
		// TODO Auto-generated method stub

	}
}
