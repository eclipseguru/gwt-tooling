/***************************************************************************************************
 * Copyright (c) 2006 Gunnar Wagenknecht, Truition and others.
 * All rights reserved. 
 *
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Gunnar Wagenknecht - initial API and implementation
 *               Eclipse.org - ideas, concepts and code from existing Eclipse projects
 **************************************************************************************************/
package org.eclipseguru.gwt.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The GWT UI plug-in.
 */
public class GwtUi extends AbstractUIPlugin {

	/** the plugin id */
	public static final String PLUGIN_ID = "org.eclipseguru.gwt.ui"; //$NON-NLS-1$

	/** sharedInstance */
	private static GwtUi sharedInstance;

	/**
	 * Returns the active workbench page.
	 * 
	 * @return the active workbench page (maybe <code>null</code>)
	 */
	public static IWorkbenchPage getActiveWorkbenchPage() {
		IWorkbenchWindow activeWorkbenchWindow = getPlugin().getWorkbench().getActiveWorkbenchWindow();
		if (null != activeWorkbenchWindow)
			return activeWorkbenchWindow.getActivePage();

		return null;
	}

	/**
	 * The active workbench shell.
	 * 
	 * @return the active workbench shell (maybe <code>null</code>)
	 */
	public static Shell getActiveWorkbenchShell() {
		// TODO Auto-generated method stub
		IWorkbenchWindow activeWorkbenchWindow = getPlugin().getWorkbench().getActiveWorkbenchWindow();
		if (null != activeWorkbenchWindow)
			return activeWorkbenchWindow.getShell();

		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static GwtUi getPlugin() {
		return sharedInstance;
	}

	/**
	 * Logs the specified {@link IStatus}.
	 * 
	 * @param status
	 */
	public static void log(IStatus status) {
		getPlugin().getLog().log(status);
	}

	/**
	 * Logs the specified error
	 * 
	 * @param message
	 */
	public static void logError(String message, Throwable cause) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, -1, message, cause));
	}

	/**
	 * Logs the specified error message
	 * 
	 * @param message
	 */
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, -1, message, null));
	}

	/**
	 * Creates an error status
	 * 
	 * @param message
	 * @return the error status
	 */
	public static IStatus newErrorStatus(String message) {
		return new Status(IStatus.ERROR, PLUGIN_ID, -1, message, null);
	}

	/**
	 * Creates an error status
	 * 
	 * @param message
	 * @return the error status
	 */
	public static IStatus newErrorStatus(Throwable e) {
		if (e instanceof CoreException)
			return ((CoreException) e).getStatus();
		if (null == e)
			e = new Exception();
		return new Status(IStatus.ERROR, PLUGIN_ID, -1, null != e.getMessage() ? e.getMessage() : e.toString(), e);
	}

	/**
	 * Creates a new instance.
	 */
	public GwtUi() {
		super();
		sharedInstance = this;
	}
}
