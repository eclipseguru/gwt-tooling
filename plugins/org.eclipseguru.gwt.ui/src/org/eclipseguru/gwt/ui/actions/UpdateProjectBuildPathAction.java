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

import org.eclipseguru.gwt.core.classpath.GwtClasspathUtil;
import org.eclipseguru.gwt.core.project.GwtProjectNature;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

/**
 * Updates the classpath of a GWT project.
 */
public class UpdateProjectBuildPathAction implements IObjectActionDelegate {

	private IProject selectedProject;

	private IWorkbenchWindow workbenchWindow;

	protected IWorkspaceRunnable createUpdateClasspathRunnable(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor = ProgressUtil.monitor(monitor);
				try {
					monitor.beginTask("Updating classpath...", 2);

					// add GWT container
					GwtClasspathUtil.addGwtContainer(project, ProgressUtil.subProgressMonitor(monitor, 1));
					// update JRE container
					GwtClasspathUtil.updateJREContainer(project, ProgressUtil.subProgressMonitor(monitor, 1), true);

				} finally {
					monitor.done();
				}
			}
		};
	}

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
		final IProject project = getSelectedProject();
		if ((null != project) && GwtProjectNature.isPossibleGwtProject(project)) {
			final IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						ResourcesPlugin.getWorkspace().run(createUpdateClasspathRunnable(project), null, IWorkspace.AVOID_UPDATE, monitor);
					} catch (final CoreException e) {
						throw new InvocationTargetException(e);
					} catch (final OperationCanceledException e) {
						throw new InterruptedException();
					}
				}
			};
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnableWithProgress);
				MessageDialog.openInformation(shell, "GWT Classpath", MessageFormat.format("Updated classpath of project {0}.", selectedProject.getName()));
			} catch (final InvocationTargetException e) {
				GwtUi.logError("Error while updating classpath.", e.getCause());
				ErrorDialog.openError(shell, "Error", "An error occured while updating the project classpath.", GwtUi.newErrorStatus(e.getCause()));
			} catch (final InterruptedException e) {
				// canceled
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
