/*
 * Copyright 2006 TG. (techieguy@gmail.com)
 * Copyright 2006 Eclipse Guru (eclipseguru@gmail.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;
import org.eclipseguru.gwt.core.utils.ResourceUtil;
import org.eclipseguru.gwt.ui.GwtUi;
import org.eclipseguru.gwt.ui.dialogs.ModuleSelectionDialog;

import com.googlipse.gwt.common.Constants;
import com.googlipse.gwt.common.Util;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class NewGwtRemoteServiceWizardPage extends NewTypeWizardPage {

	private class DialogFieldAdapter implements IStringButtonAdapter, IDialogFieldListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			if (field == moduleDialogField)
				handleModuleDialogFieldSearchButtonPressed();

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == moduleDialogField)
				handleModuleDialogFieldChanged();
			else if (field == serviceUriDialogField)
				handleServiceUriDialogFieldChanged();

		}

	}

	/** MODULE */
	protected static final String MODULE = "NewGwtRemoteServicePageWizardPage.ModuleField";

	/** SERVICE_URI */
	protected static final String SERVICE_URI = "NewGwtRemoteServicePageWizardPage.ServiceUriField";

	/** PAGE_NAME */
	private static final String PAGE_NAME = "NewGwtRemoteServicePageWizardPage";

	/** SETTINGS_CREATEMAIN */
	private final static String SETTINGS_CREATESERVLET = "create_servlet"; //$NON-NLS-1$

	private Map<String, String> templateVars;

	protected IStatus moduleStatus;
	protected IStatus serviceUriStatus;

	private SelectionButtonDialogFieldGroup serviceStubsButtons;
	private StringDialogField serviceUriDialogField;
	private StringButtonDialogField moduleDialogField;

	public NewGwtRemoteServiceWizardPage() {
		super(false, PAGE_NAME);
		setTitle("GWT Remote Service");
		setDescription("Create a new RemoteService along with corresponding Async and Impl files");

		DialogFieldAdapter adapter = new DialogFieldAdapter();

		moduleDialogField = new StringButtonDialogField(adapter);
		moduleDialogField.setLabelText("Module Location:");
		moduleDialogField.setButtonLabel("Browse...");
		moduleDialogField.setDialogFieldListener(adapter);

		serviceUriDialogField = new StringDialogField();
		serviceUriDialogField.setLabelText("Service URI:");
		serviceUriDialogField.setDialogFieldListener(adapter);

		String[] buttonNames = new String[] { "Servlet &Implementation" };
		serviceStubsButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames, 1);
		serviceStubsButtons.setLabelText("Which service stubs would you like to create?");
	}

	// -------- Initialization ---------

	private void addServiceUriToGwtXml(IProgressMonitor monitor) {
		// noops
	}

	/**
	 * Apply GWT default values.
	 */
	private void applyGwtTypeDefaults() {
		// type modifiers
		setModifiers(F_PUBLIC, false);

		// package
		setPackageFragment(null, false);

		// super interfaces
		List<String> superInterfaces = new ArrayList<String>(1);
		setSuperInterfaces(superInterfaces, true);
	}

	/*
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		int nColumns = 4;

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		// pick & choose the wanted UI components

		createModuleControls(composite, nColumns);
		createPackageControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createServiceUriControls(composite, nColumns);
		// createModifierControls(composite, nColumns);

		createServiceStubSelectionControls(composite, nColumns);

		// configure the type defaults
		applyGwtTypeDefaults();

		setControl(composite);

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
	}

	/**
	 * Creates the GWT Module.
	 * 
	 * @param monitor
	 */
	public void createGwtRemoteService(IProgressMonitor monitor) throws CoreException, InterruptedException, IOException {

		monitor = ProgressUtil.monitor(monitor);
		try {

			monitor.beginTask("Creating RemoteService ...", 10);

			// initialize template vars
			initTemplateVars();

			// add service uri to xml
			addServiceUriToGwtXml(ProgressUtil.subProgressMonitor(monitor, 1));

			// create service interface
			createRemoteService(ProgressUtil.subProgressMonitor(monitor, 1));

			// create service implementation
			if (isCreateServlet())
				createRemoteServiceImpl(ProgressUtil.subProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	private void createModuleControls(Composite parent, int nColumns) {
		moduleDialogField.doFillIntoGrid(parent, nColumns);
		LayoutUtil.setWidthHint(moduleDialogField.getTextControl(null), getMaxFieldWidth());
	}

	private void createRemoteService(IProgressMonitor monitor) throws IOException, CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Monitor message", 2);

			// get base package
			IContainer basePackage = (IContainer) getPackageFragment().getResource();

			// create client package
			IFolder clientPackage = basePackage.getFolder(new Path(Constants.CLIENT_PACKAGE));
			if (!clientPackage.exists())
				ResourceUtil.createFolderHierarchy(clientPackage, ProgressUtil.subProgressMonitor(monitor, 1));

			// create file
			IFile remoteService = clientPackage.getFile(getServiceName() + ".java");
			Util.writeFileFromTemplate("RemoteService.Service.template", remoteService, templateVars);
		} finally {
			monitor.done();
		}
	}

	private void createRemoteServiceImpl(IProgressMonitor monitor) throws IOException, CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask("Monitor message", 2);

			// get base package
			IContainer basePackage = (IContainer) getPackageFragment().getResource();

			// create server package
			IFolder serverPackage = basePackage.getFolder(new Path(Constants.SERVER_PACKAGE));
			if (!serverPackage.exists())
				ResourceUtil.createFolderHierarchy(serverPackage, ProgressUtil.subProgressMonitor(monitor, 1));

			// create file
			IFile remoteService = serverPackage.getFile(getServiceName() + "Impl.java");
			Util.writeFileFromTemplate("RemoteService.ServiceImpl.template", remoteService, templateVars);
		} finally {
			monitor.done();
		}
	}

	// ------ UI --------

	private void createServiceStubSelectionControls(Composite parent, int nColumns) {
		Control labelControl = serviceStubsButtons.getLabelControl(parent);
		LayoutUtil.setHorizontalSpan(labelControl, nColumns);

		DialogField.createEmptySpace(parent);

		Control buttonGroup = serviceStubsButtons.getSelectionButtonsGroup(parent);
		LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
	}

	private void createServiceUriControls(Composite parent, int nColumns) {
		serviceUriDialogField.doFillIntoGrid(parent, nColumns);
		LayoutUtil.setWidthHint(serviceUriDialogField.getTextControl(null), getMaxFieldWidth());
	}

	// ------ validation --------
	private void doStatusUpdate() {

		moduleStatus = moduleChanged();
		fPackageStatus = packageChanged();
		fTypeNameStatus = typeNameChanged();
		fContainerStatus = containerChanged();
		serviceUriStatus = serviceUriChanged();

		// status of all used components
		IStatus[] status = new IStatus[] { moduleStatus, fTypeNameStatus, serviceUriStatus };

		// the most severe status will be displayed and the OK button
		// enabled/disabled.
		updateStatus(status);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#getModifiedResource()
	 */
	@Override
	public IResource getModifiedResource() {
		IPackageFragment pack = getPackageFragment();
		if (pack != null) {
			IContainer packageFolder = (IContainer) pack.getResource();
			if (null != packageFolder)
				return packageFolder.getFile(new Path(Constants.CLIENT_PACKAGE).append(getServiceName() + ".java"));
		}
		return null;
	}

	/**
	 * Returns the GWT module based on the content in the module dialog field.
	 * 
	 * @return the GWT module (maybe <code>null</code>)
	 */
	protected GwtModule getModule() {
		try {
			IResource file = ResourcesPlugin.getWorkspace().getRoot().findMember(moduleDialogField.getText());
			if (GwtUtil.isModuleDescriptor(file))
				return GwtCore.create((IFile) file);
		} catch (Exception e) {
			// fail gracefully
		}
		return null;
	}

	/**
	 * Returns the entered service name.
	 * 
	 * @return the entered service name
	 */
	public String getServiceName() {
		return getTypeName();
	}

	/**
	 * Returns the entered service uri.
	 * 
	 * @return the entered service uri
	 */
	public String getServiceUriText() {
		return serviceUriDialogField.getText();
	}

	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();
	}

	private void handleModuleDialogFieldChanged() {
		GwtModule module = getModule();
		IPackageFragment modulePackage = null;
		if (null != module)
			modulePackage = module.getModulePackage();

		setPackageFragmentRoot(null != modulePackage ? (IPackageFragmentRoot) modulePackage.getParent() : null, false);
		setPackageFragment(modulePackage, false);

		handleFieldChanged(MODULE);
	}

	private void handleModuleDialogFieldSearchButtonPressed() {
		try {
			ModuleSelectionDialog dialog = new ModuleSelectionDialog(getShell(), GwtCore.getModel().getProjects());
			if (dialog.open() == Window.OK) {
				GwtModule module = dialog.getSelectedModule();
				setModule(module, true);
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Error", "We are sorry, an unknown error occured.", GwtUi.newErrorStatus(e));
		}
	}

	private void handleServiceUriDialogFieldChanged() {
		handleFieldChanged(SERVICE_URI);
	}

	/**
	 * The wizard owning this page is responsible for calling this method with
	 * the current selection. The selection is used to initialize the fields of
	 * the wizard page.
	 * 
	 * @param selection
	 *            used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem = getInitialJavaElement(selection);
		initContainerPage(jelem);
		initModule(jelem);
		doStatusUpdate();

		boolean createServlet = true;
		IDialogSettings dialogSettings = getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section = dialogSettings.getSection(PAGE_NAME);
			if (section != null)
				createServlet = section.getBoolean(SETTINGS_CREATESERVLET);
		}

		setServiceStubSelection(createServlet, true);
	}

	/**
	 * Initializes all fields provided by the page with a given selection.
	 * 
	 * @param elem
	 *            the selection used to initialize this page or <code>
	 * null</code>
	 *            if no selection was available
	 */
	protected void initModule(IJavaElement elem) {
		if (null == elem)
			return;

		IResource resource = elem.getResource();
		if (null == resource)
			return;

		if (GwtUtil.isModuleDescriptor(resource))
			setModule(GwtCore.create((IFile) resource), true);
	}

	private void initTemplateVars() {
		templateVars = new HashMap<String, String>();
		templateVars.put("@serviceName", getServiceName());
		templateVars.put("@basePackage", getPackageText());
		templateVars.put("@serviceUri", getServiceUriText());
	}

	/**
	 * Returns the current selection state of the 'Servlet Implementation'
	 * checkbox.
	 * 
	 * @return the selection state of the 'Servlet Implementation' checkbox
	 */
	public boolean isCreateServlet() {
		return serviceStubsButtons.isSelected(0);
	}

	/**
	 * A hook method that gets called when the module field has changed.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus moduleChanged() {
		StatusInfo status = new StatusInfo();
		String module = moduleDialogField.getText();
		if (module.trim().length() == 0) {
			status.setError("Please select a GWT module.");
			return status;
		}

		IStatus pathStatus = ResourcesPlugin.getWorkspace().validatePath(module, IResource.FILE);
		if (!pathStatus.isOK()) {
			status.setError(pathStatus.getMessage());
			return status;
		}

		IResource moduleDescriptor = ResourcesPlugin.getWorkspace().getRoot().findMember(module);
		if (null == moduleDescriptor) {
			status.setError("No GWT module found at the location entered.");
			return status;
		} else if (!GwtUtil.isModuleDescriptor(moduleDescriptor)) {
			status.setError("The entered module path is not a valid GWT modul descriptor.");
			return status;
		}

		GwtModule gwtModule = GwtCore.create((IFile) moduleDescriptor);
		if (null == gwtModule.getModulePackage()) {
			status.setError("The selected module is not on the classpath");
			return status;
		}

		return status;
	}

	// ---- creation ----------------

	/**
	 * A hook method that gets called when the status uri field has changed.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus serviceUriChanged() {
		StatusInfo status = new StatusInfo();
		String uri = getServiceUriText();
		if (uri.startsWith("/")) {
			status.setError("The service uri must not start with '/'.");
			return status;
		}

		return status;
	}

	/**
	 * Sets the module location.
	 * 
	 * @param module
	 *            the module descriptor
	 * @param canBeModified
	 *            if <code>true</code> the method stub checkboxes can be
	 *            changed by the user. If <code>false</code> the buttons are
	 *            "read-only"
	 */
	public void setModule(GwtModule module, boolean canBeModified) {
		moduleDialogField.setText(module.getModuleDescriptor().getFullPath().toString());
		moduleDialogField.setEnabled(canBeModified);
	}

	/**
	 * Sets the selection state of the service stub checkboxes.
	 * 
	 * @param createServlet
	 *            initial selection state of the 'Servlet Implementation'
	 *            checkbox.
	 * @param canBeModified
	 *            if <code>true</code> the method stub checkboxes can be
	 *            changed by the user. If <code>false</code> the buttons are
	 *            "read-only"
	 */
	public void setServiceStubSelection(boolean createServlet, boolean canBeModified) {
		serviceStubsButtons.setSelection(0, createServlet);
		serviceStubsButtons.setEnabled(canBeModified);
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			setFocus();
		else {
			IDialogSettings dialogSettings = getDialogSettings();
			if (dialogSettings != null) {
				IDialogSettings section = dialogSettings.getSection(PAGE_NAME);
				if (section == null)
					section = dialogSettings.addNewSection(PAGE_NAME);
				section.put(SETTINGS_CREATESERVLET, isCreateServlet());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#typeNameChanged()
	 */
	@Override
	protected IStatus typeNameChanged() {
		IStatus typeNameStatus = super.typeNameChanged();
		if (!typeNameStatus.isOK())
			return typeNameStatus;

		StatusInfo status = new StatusInfo();
		String typeName = getTypeName();
		if (typeName.indexOf('<') != -1) {
			status.setError("Parametric types are not supported by GWT.");
			return status;
		}

		return status;
	}
}