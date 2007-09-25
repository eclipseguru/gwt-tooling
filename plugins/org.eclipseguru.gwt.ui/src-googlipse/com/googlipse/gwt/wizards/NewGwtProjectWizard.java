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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.ui.GwtUi;

/**
 * @author TG. (techieguy@gmail.com)
 * 
 */
@SuppressWarnings("restriction")
public class NewGwtProjectWizard extends NewElementWizard {

	NewGwtProjectWizardPage1 firstPage;
	NewGwtProjectWizardPage2 secondPage;
	private IWorkbench workbench;

	public NewGwtProjectWizard() {

		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle("New GWT Project");
	}

	@Override
	public void addPages() {

		super.addPages();
		firstPage = new NewGwtProjectWizardPage1();
		secondPage = new NewGwtProjectWizardPage2(firstPage, workbench);
		addPage(firstPage);
		addPage(secondPage);

	}

	@Override
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {

		monitor = ProgressUtil.monitor(monitor);
		try {
			secondPage.doFinish(monitor);
		} catch (Exception e) {
			ErrorDialog.openError(getShell(), "Error", "We are sorry, an error occured while creating the module.", GwtUi.newErrorStatus(e));
			monitor.setCanceled(true);
		}
	}

	@Override
	public IJavaElement getCreatedElement() {
		return secondPage.getCreatedElement();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
	}

	@Override
	public boolean performCancel() {
		secondPage.performCancel();
		return super.performCancel();
	}

	@Override
	public boolean performFinish() {
		boolean finished = super.performFinish();
		if (finished) {
			// selectAndReveal(secondPage.getJavaProject().getProject());
		}
		return finished;
	}
}
