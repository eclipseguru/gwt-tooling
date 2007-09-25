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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author TG. (techieguy@gmail.com)
 * 
 */
public class NewGwtProjectWizardPage1 extends NewElementWizardPage {

	Label projectNameLabel;
	Text projectNameText;

	boolean firstLoad = true;
	String projectName;

	public NewGwtProjectWizardPage1() {

		super("New GWT Project");
		setTitle("Create a Gwt project");
		setDescription("Create a Gwt project in the workspace with default source and output folders.");
		setPageComplete(false);

	}

	public void createControl(Composite parent) {

		Composite newProjectPanel = new Composite(parent, SWT.NONE);
		newProjectPanel.setFont(parent.getFont());
		newProjectPanel.setLayout(new GridLayout(2, false));
		newProjectPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		projectNameLabel = new Label(newProjectPanel, SWT.None);
		projectNameLabel.setText("Project Name");
		projectNameLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));

		projectNameText = new Text(newProjectPanel, SWT.BORDER);
		projectNameText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		projectNameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				validate();
			}
		});

		setControl(newProjectPanel);
	}

	public String getProjectName() {

		return projectName;
	}

	@Override
	public void setVisible(boolean visible) {

		super.setVisible(visible);
		if (visible) {
			projectNameText.setFocus();
			if (firstLoad) {
				setPageComplete(false);
				firstLoad = false;
			}
		}

	}

	public void validate() {

		projectName = projectNameText.getText();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IStatus status = workspace.validateName(projectName, IResource.PROJECT);
		if (!status.isOK()) {
			setErrorMessage(status.getMessage());
			setPageComplete(false);
		} else {
			setPageComplete(true);
			setErrorMessage(null);
			setMessage(null);
		}
	}

}