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
package org.eclipseguru.gwt.ui.actions;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.builder.GwtProjectPublisher;
import org.eclipseguru.gwt.core.project.GwtProjectNature;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Compiles and publishes all project and included modules in the project.
 */
public class PublishProjectAction implements IObjectActionDelegate {

	private IProject selectedProject;

	private IWorkbenchWindow workbenchWindow;

	protected IProject getSelectedProject() {
		return selectedProject;
	}

	protected IWorkbenchWindow getWorkbenchWindow() {
		return workbenchWindow;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(final IAction action) {
		final Shell shell = null != workbenchWindow ? workbenchWindow.getShell() : new Shell();
		if ((null != selectedProject) && GwtProjectNature.isPossibleGwtProject(getSelectedProject())) {
			try {
				new GwtProjectPublisher(GwtCore.create(getSelectedProject())).schedule();
			} catch (final Exception e) {
				ErrorDialog.openError(shell, "Error", "An error occured while initilizing the project publishing process.", GwtUi.newErrorStatus(e));
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(final IAction action, final ISelection selection) {
		selectedProject = null;
		if (!(selection instanceof IStructuredSelection))
			return;

		final Object element = ((IStructuredSelection) selection).getFirstElement();
		if (element instanceof IProject) {
			selectedProject = (IProject) element;
		}

		if ((null == selectedProject) && (element instanceof IAdaptable)) {
			selectedProject = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
		}
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
		workbenchWindow = targetPart.getSite().getWorkbenchWindow();
	}

}
