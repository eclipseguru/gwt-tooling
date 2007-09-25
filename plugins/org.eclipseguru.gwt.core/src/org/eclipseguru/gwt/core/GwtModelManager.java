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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

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
	public static GwtModule createBinaryModule(IStorage resource, IPackageFragment packageFragment) {
		if (resource == null)
			return null;

		if (null == GwtUtil.getSimpleName(resource))
			return null;

		GwtProject project = getModelManager().getModel().createProject(packageFragment.getJavaProject().getProject());
		if (null == project)
			return null;

		return project.createBinaryModule(resource, packageFragment);
	}

	/**
	 * Returns the GWT module corresponding to the given file, its project being
	 * the given project. Returns <code>null</code> if unable to associate the
	 * given file with a GWT module.
	 */
	public static GwtModule createModule(IFile file, GwtProject project) {
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
	public static GwtRemoteService createRemoteService(IType type, GwtModule module) {
		if (type == null)
			return null;

		try {
			if (!GwtRemoteService.isRemoteService(type))
				return null;

			if (module == null) {
				IResource resource = type.getResource();
				if (null == resource)
					// TODO: support types in external archives
					return null;

				GwtProject project = getModelManager().getModel().createProject(resource);
				GwtModule[] modules = project.getModules();
				for (GwtModule module2 : modules) {
					if (module2.isModuleResource(resource)) {
						module = module2;
						break;
					}
				}

			}

			// no module found
			if (null == module)
				return null;

			return module.createRemoteService(type);
		} catch (CoreException e) {
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
	private static boolean findModuleInPackage(IPackageFragment packageFragment, String moduleId, List<GwtModule> modules) throws JavaModelException {
		// check package first
		if (!packageFragment.getElementName().equals(GwtUtil.getPackageName(moduleId)))
			return false;

		// find module source
		String gwtFileName = GwtUtil.getSimpleName(moduleId).concat(GwtUtil.GWT_MODULE_SOURCE_EXTENSION);
		Object[] nonJavaResources = packageFragment.getNonJavaResources();
		for (Object nonJavaResource : nonJavaResources)
			// project resource
			if (nonJavaResource instanceof IResource) {
				IResource resource = (IResource) nonJavaResource;
				if (GwtUtil.isModuleDescriptor(resource) && resource.getName().equals(gwtFileName)) {
					GwtModule module = createModule((IFile) resource, getModelManager().getModel().createProject(resource));
					if (null != module) {
						modules.add(module);
						return true;
					}
				}
			}

			// jar entry
			else if (nonJavaResource instanceof IStorage) {
				IStorage resource = (IStorage) nonJavaResource;
				if (gwtFileName.equals(resource.getName())) {
					GwtModule module = createBinaryModule(resource, packageFragment);
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
	public static GwtModule[] findModules(String[] moduleIds, GwtProject project) throws GwtModelException {
		if ((null == moduleIds) || (null == project))
			return null;

		if (moduleIds.length == 0)
			return NO_MODULES;

		List<GwtModule> modules = new ArrayList<GwtModule>(moduleIds.length);
		IJavaProject javaProject = project.getJavaProject();

		try {
			IPackageFragment[] packageFragments = javaProject.getPackageFragments();
			if (packageFragments.length > moduleIds.length)
				for (IPackageFragment fragment : packageFragments)
					for (String moduleId : moduleIds)
						findModuleInPackage(fragment, moduleId, modules);
			else
				for (String moduleId : moduleIds)
					for (IPackageFragment fragment : packageFragments)
						findModuleInPackage(fragment, moduleId, modules);

		} catch (JavaModelException e) {
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
	private GwtModel model = new GwtModel();

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
