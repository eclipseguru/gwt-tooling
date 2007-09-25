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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipseguru.gwt.core.runtimes.GwtRuntime;
import org.eclipseguru.gwt.core.runtimes.GwtRuntimeManager;
import org.osgi.framework.BundleContext;

/**
 * Provides access to the plugin.
 */
public class GwtCore extends Plugin {

	/** the plugin id */
	public static final String PLUGIN_ID = "org.eclipseguru.gwt.core"; //$NON-NLS-1$

	/** PROBLEM_MARKER */
	public static final String PROBLEM_MARKER = PLUGIN_ID + ".problem"; //$NON-NLS-1$

	/** NATURE_ID */
	public static final String NATURE_ID = PLUGIN_ID + ".nature"; //$NON-NLS-1$

	/** BUILDER_ID */
	public static final String BUILDER_ID = PLUGIN_ID + ".builder"; //$NON-NLS-1$

	/** GWT_CONTAINER */
	public static final String GWT_CONTAINER = PLUGIN_ID + ".classpath.container"; //$NON-NLS-1$

	/** MODULE_SOURCE_CONTENT_TYPE_ID */
	public static final String MODULE_SOURCE_CONTENT_TYPE_ID = PLUGIN_ID + ".modulesource"; //$NON-NLS-1$

	/** status constant for internal errors */
	private static final int INTERNAL_ERROR = -1;

	/** the shared instance */
	private static GwtCore instance;

	/**
	 * Returns the GWT module corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file with a GWT
	 * module.
	 * 
	 * @param moduleDescriptor
	 *            the module descriptor
	 * @return the module (maybe <code>null</code>)
	 */
	public static GwtModule create(IFile moduleDescriptor) {
		if (null == moduleDescriptor)
			return null;

		return GwtModelManager.createModule(moduleDescriptor, null);
	}

	/**
	 * Returns the GWT project corresponding to the specified project.
	 * 
	 * @param project
	 * @return the GWT project
	 */
	public static GwtProject create(IProject project) {
		if (null == project)
			return null;

		GwtModel model = GwtModelManager.getModelManager().getModel();
		return model.createProject(project);
	}

	/**
	 * Returns the GWT remote service corresponding to the given type, or
	 * <code>null</code> if unable to associate the given type with a GWT
	 * remote service.
	 * 
	 * @param type
	 *            the remote service type
	 * @return the module (maybe <code>null</code>)
	 * @param type
	 */
	public static GwtRemoteService create(IType type) {
		if (null == type)
			return null;

		return GwtModelManager.createRemoteService(type, null);
	}

	/**
	 * Returns the GWT model for the specified workspace root.
	 * 
	 * @param root
	 * @return the GWT model
	 */
	public static GwtModel create(IWorkspaceRoot root) {
		if (null == root)
			return null;

		return GwtModelManager.getModelManager().getModel();
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance (maybe <code>null</code> if the plugin was
	 *         not initialized properly by Eclipse)
	 */
	public static GwtCore getGwtCore() {
		return instance;
	}

	/**
	 * Returns the GWT model.
	 * 
	 * @return the GWT model
	 */
	public static GwtModel getModel() {
		return GwtModelManager.getModelManager().getModel();
	}

	/**
	 * Returns the runtime of the specified project.
	 * 
	 * @param project
	 * @return the runtime of the specified project
	 */
	public static GwtRuntime getRuntime(GwtProject project) {
		// for now we don't have project specific runtims
		return GwtRuntimeManager.getInstalledRuntimes()[0];
	}

	/**
	 * Logs the specified {@link IStatus}.
	 * 
	 * @param status
	 */
	public static void log(IStatus status) {
		getGwtCore().getLog().log(status);
	}

	/**
	 * Logs the specified error
	 * 
	 * @param message
	 */
	public static void logError(String message, Throwable cause) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, cause));
	}

	/**
	 * Logs the specified error message
	 * 
	 * @param message
	 */
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, null));
	}

	/**
	 * Creates an error status
	 * 
	 * @param message
	 * @return the error status
	 */
	public static IStatus newErrorStatus(String message) {
		return newErrorStatus(message, null);
	}

	/**
	 * Creates an error status
	 * 
	 * @param message
	 * @param cause
	 * @return the error status
	 */
	public static IStatus newErrorStatus(String message, Throwable cause) {
		return new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, cause);
	}

	/**
	 * This constructor is called by the Eclipse framework. <b>DO NOT CALL IT.</b>
	 */
	public GwtCore() {
		super();
		instance = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		// super
		super.start(context);

		// startup performed in model manager
		GwtModelManager.getModelManager().startup();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		// shutdown model manager
		GwtModelManager.getModelManager().shutdown();

		// super
		super.stop(context);
	}
}
