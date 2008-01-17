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
package org.eclipseguru.gwt.ui.launch;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.launch.GwtLaunchConstants;
import org.eclipseguru.gwt.core.launch.GwtLaunchUtil;
import org.eclipseguru.gwt.ui.GwtUi;
import org.eclipseguru.gwt.ui.GwtUiImages;
import org.eclipseguru.gwt.ui.dialogs.ModuleSelectionDialog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.net.MalformedURLException;
import java.net.URL;

import com.googlipse.gwt.common.Constants;

/**
 * A launch configuration tab that displays and edits GWT module related launch
 * configuration attributes.
 */
public class GwtMainTab extends AbstractLaunchConfigurationTab implements GwtLaunchConstants {

	private final class ModuleDialogFieldAdapter implements IStringButtonAdapter {
		public void changeControlPressed(final DialogField field) {
			if (field == moduleDialogField)
				handleModuleDialogFieldSearchButtonPressed();
		}
	}

	private final class ProjectDialogFieldAdapter implements IStringButtonAdapter {
		public void changeControlPressed(final DialogField field) {
			if (field == projectDialogField)
				handleProjectDialogFieldSearchButtonPressed();
		}
	}

	private static final String EMPTY_STRING = "";

	private final IDialogFieldListener sharedDialogFieldAdapter = new IDialogFieldListener() {
		private boolean inUpdate;
		private boolean restoreCustomUrl;

		public void dialogFieldChanged(DialogField field) {
			if (inUpdate)
				return;
			try {
				inUpdate = true;

				if ((field == launchInternalASDialogField) || (field == doNotLaunchInternalASDialogField)) {
					launchInternalASDialogField.setSelection(!doNotLaunchInternalASDialogField.isSelected());
					if (launchInternalASDialogField.isSelected()) {
						if (!customUrlDialogField.isEnabled() && restoreCustomUrl)
							customUrlDialogField.setSelection(false);
						customUrlDialogField.setEnabled(true);
					} else {
						restoreCustomUrl = !customUrlDialogField.isSelected() && customUrlDialogField.isEnabled();
						customUrlDialogField.setSelection(true);
						customUrlDialogField.setEnabled(false);
					}
				}

				// we update this all the time (just to keep it in sync)
				if (initialized && !customUrlDialogField.isSelected())
					urlDialogField.setTextWithoutUpdate(GwtLaunchUtil.computeDefaultUrl(moduleDialogField.getText()));

				updateLaunchConfigurationDialog();
			} finally {
				inUpdate = false;
			}
		}
	};

	private StringButtonDialogField moduleDialogField;
	private SelectionButtonDialogField customUrlDialogField;
	private StringDialogField urlDialogField;
	private StringButtonDialogField projectDialogField;
	private SelectionButtonDialogField launchInternalASDialogField;
	private StringDialogField portDialogField;
	private SelectionButtonDialogField doNotLaunchInternalASDialogField;
	private boolean initialized;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(final Composite parent) {
		final Composite result = new Composite(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout layout = new GridLayout();
		result.setLayout(layout);

		final Group projectGroup = new Group(result, SWT.NONE);
		projectGroup.setText("Project:");
		projectGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		final Group moduleGroup = new Group(result, SWT.NONE);
		moduleGroup.setText("Module:");
		moduleGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		final Group internalTomcatGroup = new Group(result, SWT.NONE);
		internalTomcatGroup.setText("Application Server:");
		internalTomcatGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		createDialogFields();
		initDialogFields();

		// module
		LayoutUtil.doDefaultLayout(projectGroup, new DialogField[] { projectDialogField }, false, 5, 5);
		LayoutUtil.setHorizontalGrabbing(projectDialogField.getTextControl(projectGroup));

		// url group
		LayoutUtil.doDefaultLayout(moduleGroup, new DialogField[] { moduleDialogField, customUrlDialogField, urlDialogField }, false, 5, 5);
		LayoutUtil.setHorizontalGrabbing(moduleDialogField.getTextControl(moduleGroup));
		LayoutUtil.setHorizontalGrabbing(urlDialogField.getTextControl(moduleGroup));

		// application server group (used a special layout)
		layout = new GridLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.numColumns = 4;
		internalTomcatGroup.setLayout(layout);
		launchInternalASDialogField.doFillIntoGrid(internalTomcatGroup, 2);
		portDialogField.doFillIntoGrid(internalTomcatGroup, 2);
		doNotLaunchInternalASDialogField.doFillIntoGrid(internalTomcatGroup, 4);
		LayoutUtil.setHorizontalGrabbing(launchInternalASDialogField.getSelectionButton(internalTomcatGroup));

		// limit the port control
		final Text portTextControl = portDialogField.getTextControl(internalTomcatGroup);
		portTextControl.setTextLimit(5);
		((GridData) portTextControl.getLayoutData()).widthHint = new PixelConverter(portTextControl).convertWidthInCharsToPixels(8);

		Dialog.applyDialogFont(result);
		setControl(result);
	}

	private void createDialogFields() {
		projectDialogField = new StringButtonDialogField(new ProjectDialogFieldAdapter());
		projectDialogField.setButtonLabel("Search...");
		projectDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		moduleDialogField = new StringButtonDialogField(new ModuleDialogFieldAdapter());
		moduleDialogField.setLabelText("Module:");
		moduleDialogField.setButtonLabel("Search...");
		moduleDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		customUrlDialogField = new SelectionButtonDialogField(SWT.CHECK);
		customUrlDialogField.setLabelText("Use custom URL");
		customUrlDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		urlDialogField = new StringDialogField();
		urlDialogField.setLabelText("URL:");
		urlDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		customUrlDialogField.attachDialogField(urlDialogField);

		launchInternalASDialogField = new SelectionButtonDialogField(SWT.RADIO);
		launchInternalASDialogField.setLabelText("Use GWT's internal application server");
		launchInternalASDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		portDialogField = new StringDialogField();
		portDialogField.setLabelText("Port:");
		portDialogField.setDialogFieldListener(sharedDialogFieldAdapter);

		launchInternalASDialogField.attachDialogField(portDialogField);

		doNotLaunchInternalASDialogField = new SelectionButtonDialogField(SWT.RADIO);
		doNotLaunchInternalASDialogField.setLabelText("Use external application server started by me");
		doNotLaunchInternalASDialogField.setDialogFieldListener(sharedDialogFieldAdapter);
	}

	/**
	 * Returns the current resource selected in the UI.
	 * 
	 * @return the current selected resource (maybe <code>null</code>)
	 */
	protected IResource getContext() {
		final IWorkbenchPage page = GwtUi.getActiveWorkbenchPage();
		if (page != null) {
			final ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection) {
				final IStructuredSelection ss = (IStructuredSelection) selection;
				if (!ss.isEmpty()) {
					final Object obj = ss.getFirstElement();
					if (obj instanceof IJavaElement) {
						final IJavaElement je = (IJavaElement) obj;
						return je.getResource();
					}
					if (obj instanceof IResource)
						return (IResource) obj;
				}
			}
			final IEditorPart part = page.getActiveEditor();
			if (part != null) {
				final IEditorInput input = part.getEditorInput();
				return (IResource) input.getAdapter(IResource.class);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return GwtUiImages.get(GwtUiImages.IMG_MODULE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Main";
	}

	private void handleModuleDialogFieldSearchButtonPressed() {
		try {
			final ModuleSelectionDialog dialog = new ModuleSelectionDialog(getShell(), GwtCore.getModel().getProjects());
			if (dialog.open() == Window.OK) {
				final GwtModule module = dialog.getSelectedModule();
				moduleDialogField.setText(module.getModuleId());
				projectDialogField.setText(module.getProject().getName());
			}
		} catch (final CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "We are sorry, an internal error occured.", e.getStatus());
		}
	}

	private void handleProjectDialogFieldSearchButtonPressed() {
		final ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new WorkbenchLabelProvider());
		dialog.setTitle("GWT Project");
		dialog.setMessage("Select a GWT project to run:");
		dialog.setElements(GwtCore.getModel().getProjects());
		if (dialog.open() == Window.OK) {
			final GwtProject project = (GwtProject) dialog.getFirstResult();
			projectDialogField.setText(project.getName());
		}
	}

	private void initDialogFields() {
		launchInternalASDialogField.setSelection(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(final ILaunchConfiguration config) {
		updateModuleFromConfig(config);
		updateUrlFromConfig(config);
		updateProjectFromConfig(config);
		updateLaunchInternalASFromConfig(config);
		initialized = true;
	}

	/**
	 * Initializes the specified launch configuration with values from the
	 * specified GWT module.
	 * 
	 * @param gwtModule
	 * @param config
	 */
	private void initializeGwtModule(final GwtModule gwtModule, final ILaunchConfigurationWorkingCopy config) {
		if (null == gwtModule)
			return;

		config.setAttribute(ATTR_PROJECT_NAME, gwtModule.getProjectName());
		config.setAttribute(ATTR_MODULE_ID, gwtModule.getModuleId());
		config.setAttribute(ATTR_CUSTOM_URL, false);
	}

	/**
	 * Initializes the specified launch configuration with values from the
	 * specified project.
	 * 
	 * @param project
	 * @param config
	 */
	private void initializeProject(final IProject project, final ILaunchConfigurationWorkingCopy config) {
		if (null == project)
			return;

		config.setAttribute(ATTR_PROJECT_NAME, project.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);

		// check project
		final String projectName = projectDialogField.getText();
		if (projectName.trim().length() == 0) {
			setErrorMessage("Please select a project.");
			return false;
		}

		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.exists()) {
			setErrorMessage("The selected project does not exist.");
			return false;
		} else if (!project.isOpen()) {
			setErrorMessage("The selected project is closed.");
			return false;
		} else if (!GwtProject.hasGwtNature(project)) {
			setErrorMessage("The selected project is not a GWT project.");
			return false;
		}

		// check module
		final String moduleId = moduleDialogField.getText();
		if (moduleId.trim().length() == 0) {
			setErrorMessage("Please select a GWT module.");
			return false;
		}

		final GwtProject gwtProject = GwtCore.create(project);
		GwtModule gwtModule = null;
		try {
			gwtModule = gwtProject.getModule(moduleId);
			if (null == gwtModule) {
				setErrorMessage("A modul with the specified id does not exist in the selected project.");
				return false;
			} else if (null == gwtModule.getModulePackage()) {
				setErrorMessage("The selected module is not on the classpath");
				return false;
			}
		} catch (final GwtModelException e) {
			setErrorMessage(NLS.bind("The modules of the selected project could not be accessed ({0}).", e.getMessage()));
			return false;
		}

		// check URL
		if (customUrlDialogField.isSelected()) {
			final String url = urlDialogField.getText();
			if (url.trim().length() == 0) {
				setErrorMessage("Please enter a URL.");
				return false;
			} else
				try {
					new URL(url);
				} catch (final MalformedURLException e) {
					setErrorMessage(NLS.bind("The entered URL is invalid ({0}).", e.getMessage()));
					return false;
				}
		}

		if (launchInternalASDialogField.isSelected()) {
			final String port = portDialogField.getText();
			if (port.trim().length() == 0) {
				setErrorMessage("Please enter a port number.");
				return false;
			} else
				try {
					final int portNbr = Integer.parseInt(port);
					if ((portNbr < 0) || (portNbr > 65535)) {
						setErrorMessage("The port number must be between 0 and 65535.");
						return false;
					}
				} catch (final NumberFormatException e) {
					setErrorMessage("The entered port string is not a valid number.");
					return false;
				}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(final ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(ATTR_MODULE_ID, moduleDialogField.getText());
		config.setAttribute(ATTR_PROJECT_NAME, projectDialogField.getText());
		config.setAttribute(ATTR_CUSTOM_URL, customUrlDialogField.isSelected());
		config.setAttribute(ATTR_URL, urlDialogField.getText());
		config.setAttribute(ATTR_NOSERVER, doNotLaunchInternalASDialogField.isSelected());
		try {
			config.setAttribute(Constants.LAUNCH_ATTR_PORT, Integer.parseInt(portDialogField.getText()));
		} catch (final NumberFormatException e) {
			ErrorDialog.openError(getShell(), "Error", "The entered port number seems to be invalid.", GwtUi.newErrorStatus(e));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
		// empty defaults
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
		config.setAttribute(ATTR_MODULE_ID, EMPTY_STRING);
		config.setAttribute(ATTR_URL, EMPTY_STRING);
		config.setAttribute(ATTR_CUSTOM_URL, false);
		config.setAttribute(ATTR_NOSERVER, false);
		config.setAttribute(Constants.LAUNCH_ATTR_PORT, 8888);

		// do we have a context?
		final IResource resource = getContext();
		if (null != resource)
			if (GwtUtil.isModuleDescriptor(resource))
				initializeGwtModule(GwtCore.create((IFile) resource), config);
			else
				initializeProject(resource.getProject(), config);
	}

	/**
	 * Updates the GWT internal AS attributes from the specified launch
	 * configuration.
	 * 
	 * @param config
	 */
	private void updateLaunchInternalASFromConfig(final ILaunchConfiguration config) {
		int port = 8888;
		boolean noserver = false;
		try {
			port = config.getAttribute(Constants.LAUNCH_ATTR_PORT, port);
			noserver = config.getAttribute(ATTR_NOSERVER, noserver);
		} catch (final CoreException e) {
			// ignore
		}
		portDialogField.setText(String.valueOf(port));
		doNotLaunchInternalASDialogField.setSelection(noserver);
	}

	/**
	 * Updates the module attributes from the specified launch configuration.
	 * 
	 * @param config
	 */
	private void updateModuleFromConfig(final ILaunchConfiguration config) {
		String moduleId = EMPTY_STRING;
		try {
			moduleId = config.getAttribute(ATTR_MODULE_ID, EMPTY_STRING);
		} catch (final CoreException e) {
			// ignore
		}
		moduleDialogField.setText(moduleId);
	}

	/**
	 * Updates the project attribute from the specified launch configuration.
	 * 
	 * @param config
	 */
	private void updateProjectFromConfig(final ILaunchConfiguration config) {
		String projectName = EMPTY_STRING;
		try {
			projectName = config.getAttribute(ATTR_PROJECT_NAME, projectName);
		} catch (final CoreException e) {
			// ignore
		}
		projectDialogField.setText(projectName);
	}

	/**
	 * Updates the url attributes from the specified launch configuration.
	 * 
	 * @param config
	 */
	private void updateUrlFromConfig(final ILaunchConfiguration config) {
		String url = EMPTY_STRING;
		boolean openUrl = false;
		try {
			url = config.getAttribute(ATTR_URL, url);
			openUrl = config.getAttribute(ATTR_CUSTOM_URL, openUrl);
		} catch (final CoreException e) {
			// ignore
		}
		urlDialogField.setText(url);
		customUrlDialogField.setSelection(openUrl);
	}
}
