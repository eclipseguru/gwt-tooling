/*
 * Copyright 2006 TG. (techieguy@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.googlipse.gwt.wizards;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.googlipse.gwt.common.Util;

/**
 * @author TG. (techieguy@gmail.com)
 */
public class NewGwtProjectWizardPage2 extends JavaCapabilityConfigurationPage {

	public static final String SRC_FOLDER = "src";
	public static final String BIN_FOLDER = "bin";
	private final NewGwtProjectWizardPage1 firstPage;
	private IJavaProject javaProject;
	private IProject gwtProject;

	public NewGwtProjectWizardPage2(final NewGwtProjectWizardPage1 firstPage, final IWorkbench workbench) {
		this.firstPage = firstPage;
	}

	private void addGwtNature() throws CoreException {

		final IProjectDescription description = gwtProject.getDescription();
		final String[] prevNatures = description.getNatureIds();
		final String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = GwtCore.NATURE_ID;
		description.setNatureIds(newNatures);
		gwtProject.setDescription(description, IResource.FORCE, null);
	}

	protected void createProject(IProgressMonitor monitor) throws CoreException {

		monitor = ProgressUtil.monitor(monitor);

		try {
			monitor.beginTask("Creating basic Java Project ...", 1);

			if (javaProject == null) {

				gwtProject = ResourcesPlugin.getWorkspace().getRoot().getProject(firstPage.getProjectName());
				createProject(gwtProject, (URI) null, new SubProgressMonitor(monitor, 1));
				final IPath src = new Path(NewGwtProjectWizardPage2.SRC_FOLDER);
				final IPath bin = new Path(NewGwtProjectWizardPage2.BIN_FOLDER);
				final IFolder srcFolder = gwtProject.getFolder(src);
				final IFolder binFolder = gwtProject.getFolder(bin);
				if (!srcFolder.exists())
					srcFolder.create(true, true, null);
				if (!binFolder.exists())
					binFolder.create(true, true, null);
				final IPath projectPath = gwtProject.getFullPath();
				final IClasspathEntry[] cpEntries = getDefaultClasspathEntry(projectPath.append(src));
				javaProject = JavaCore.create(gwtProject);
				init(javaProject, projectPath.append(bin), cpEntries, false);

			} else
				;// do nothing. Project already created
		} finally {
			monitor.done();
		}

	}

	private void destroy(IProgressMonitor monitor) throws CoreException {

		monitor = ProgressUtil.monitor(monitor);

		try {
			if (javaProject == null)
				;// do nothing. There is no project to delete
			else {
				javaProject.getProject().delete(true, true, null);
				javaProject = null;
			}
		} finally {
			monitor.done();
		}
	}

	public void doFinish(IProgressMonitor monitor) throws CoreException, InterruptedException, IOException {

		monitor = ProgressUtil.monitor(monitor);

		try {
			monitor.beginTask("Creating GWT Project ...", 2);

			createProject(new SubProgressMonitor(monitor, 1));
			configureJavaProject(new SubProgressMonitor(monitor, 2));
			addGwtNature();
		} finally {
			monitor.done();
		}
	}

	public IJavaElement getCreatedElement() {
		return javaProject;
	}

	public IJavaProject getCreatedProject() {
		return javaProject;
	}

	private IClasspathEntry[] getDefaultClasspathEntry(final IPath srcPath) {

		final List<IClasspathEntry> cpEntries = new ArrayList<IClasspathEntry>();

		cpEntries.add(JavaCore.newSourceEntry(srcPath));
		cpEntries.add(JavaCore.newLibraryEntry(Util.getGwtUserLibPath(), null, null));
		cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));

		return cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
	}

	public void performCancel() {

		final IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					destroy(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};

		try {
			getContainer().run(true, true, new WorkspaceModifyDelegatingOperation(op));
		} catch (final InvocationTargetException e) {
			ErrorDialog.openError(getShell(), "Error", "We are sorry, an error occured while creating the module.", GwtUi.newErrorStatus(e));
		} catch (final InterruptedException e) {
			// cancel pressed
		}
	}

	@Override
	public void setVisible(final boolean visible) {

		try {
			if (visible)
				createProject(null);
			else
				destroy(null);
		} catch (final CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "We are sorry, an error occured while creating the module.", GwtUi.newErrorStatus(e));
		}
		super.setVisible(visible);
	}
}