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
package org.eclipseguru.gwt.ui.actions;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.project.GwtProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import java.text.MessageFormat;

/**
 * Associates a project with the GWT project nature.
 */
public class ConnectProjectWithGwtToolingAction implements IObjectActionDelegate {

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
				final IProjectDescription description = selectedProject.getDescription();
				final String[] natures = description.getNatureIds();
				final String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = GwtCore.NATURE_ID;
				description.setNatureIds(newNatures);
				selectedProject.setDescription(description, null);
				MessageDialog.openInformation(shell, "GWT Project", MessageFormat.format("Successfully associated project {0} with GWT Tooling.", selectedProject.getName()));
			} catch (final CoreException e) {
				ErrorDialog.openError(shell, "Error", "An error occured while updating the project information.", e.getStatus());
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
