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

import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlipse.gwt.common.Constants;
import com.googlipse.gwt.common.Util;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class NewGwtModuleWizardPage extends NewTypeWizardPage {

	/** JAVA_LANG_OBJECT */
	private static final String JAVA_LANG_OBJECT = "java.lang.Object";

	/** COM_GOOGLE_GWT_CORE_CLIENT_ENTRY_POINT */
	private static final String COM_GOOGLE_GWT_CORE_CLIENT_ENTRY_POINT = "com.google.gwt.core.client.EntryPoint";

	private Map<String, String> templateVars;

	private IPackageFragment packageFragment;

	public NewGwtModuleWizardPage() {
		super(true, "NewGwtModuleWizardPage");

		setTitle("GWT Module");
		setDescription("Create a new GWT Module in an existing project");
	}

	// -------- Initialization ---------

	/**
	 * Apply GWT default values.
	 */
	private void applyGwtTypeDefaults() {
		// type modifiers
		setModifiers(F_PUBLIC, false);

		// super class
		setSuperClass(JAVA_LANG_OBJECT, false);

		// super interfaces
		final List<String> superInterfaces = new ArrayList<String>(1);
		superInterfaces.add(COM_GOOGLE_GWT_CORE_CLIENT_ENTRY_POINT);
		setSuperInterfaces(superInterfaces, false);
	}

	/*
	 * @see WizardPage#createControl
	 */
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		final int nColumns = 4;

		final GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		// pick & choose the wanted UI components

		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);

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
	public void createGwtModule(IProgressMonitor monitor) throws CoreException, InterruptedException, IOException {

		monitor = ProgressUtil.monitor(monitor);
		try {

			monitor.beginTask("Creating new module ...", 10);

			final IPackageFragmentRoot root = getPackageFragmentRoot();
			packageFragment = getPackageFragment();
			if (packageFragment == null)
				packageFragment = root.getPackageFragment(""); //$NON-NLS-1$

			if (!packageFragment.exists()) {
				final String packName = packageFragment.getElementName();
				packageFragment = root.createPackageFragment(packName, true, ProgressUtil.subProgressMonitor(monitor, 1));
			} else
				monitor.worked(1);

			// create client package
			root.createPackageFragment(packageFragment.getElementName().concat(".").concat(Constants.CLIENT_PACKAGE), true, ProgressUtil.subProgressMonitor(monitor, 1));

			// create public folder (note, public is a reserved keyword)
			final IFolder moduleFolder = (IFolder) packageFragment.getResource();
			final IFolder publicFolder = moduleFolder.getFolder(new Path(Constants.PUBLIC_FOLDER));
			if (!publicFolder.exists())
				publicFolder.create(IResource.FORCE, true, ProgressUtil.subProgressMonitor(monitor, 1));

			// create files
			initTemplateVars();
			createGwtXmlFile(ProgressUtil.subProgressMonitor(monitor, 1));
			createJavaFile(ProgressUtil.subProgressMonitor(monitor, 1));
			createHtmlFile(ProgressUtil.subProgressMonitor(monitor, 1));
			// createLaunchFile(new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	// ------ UI --------

	private void createGwtXmlFile(final IProgressMonitor monitor) throws IOException, CoreException {
		try {
			monitor.beginTask("Creating gwt.xml file ...", 1);

			final IContainer packageFolder = (IContainer) packageFragment.getCorrespondingResource();
			final IFile moduleXml = packageFolder.getFile(new Path(getTypeNameWithoutParameters() + '.' + Constants.GWT_XML_EXT));
			Util.writeFileFromTemplate("Module.gwt.xml.template", moduleXml, templateVars);
		} finally {
			monitor.done();
		}
	}

	private void createHtmlFile(final IProgressMonitor monitor) throws IOException, CoreException {
		try {
			monitor.beginTask("Creating Html file ...", 1);

			final IContainer packageFolder = (IContainer) packageFragment.getCorrespondingResource();
			final IFile moduleHtml = packageFolder.getFile(new Path(Constants.PUBLIC_FOLDER).append(getTypeNameWithoutParameters() + ".html"));
			Util.writeFileFromTemplate("Module.html.template", moduleHtml, templateVars);
		} finally {
			monitor.done();
		}
	}

	private void createJavaFile(final IProgressMonitor monitor) throws IOException, CoreException {
		try {
			monitor.beginTask("Creating Java file ...", 1);

			final IContainer packageFolder = (IContainer) packageFragment.getCorrespondingResource();
			final IFile moduleJava = packageFolder.getFile(new Path(Constants.CLIENT_PACKAGE).append(getTypeNameWithoutParameters() + ".java"));
			Util.writeFileFromTemplate("Module.java.template", moduleJava, templateVars);
		} finally {
			monitor.done();
		}
	}

	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		final IStatus[] status = new IStatus[] { fContainerStatus, isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus, fTypeNameStatus, fModifierStatus, fSuperClassStatus, fSuperInterfacesStatus };

		// the most severe status will be displayed and the OK button
		// enabled/disabled.
		updateStatus(status);
	}

	// ---- creation ----------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.ui.wizards.NewTypeWizardPage#getModifiedResource()
	 */
	@Override
	public IResource getModifiedResource() {
		final IPackageFragment pack = getPackageFragment();
		if (pack != null) {
			final IContainer packageFolder = (IContainer) pack.getResource();
			if (null != packageFolder)
				return packageFolder.getFile(new Path(Constants.CLIENT_PACKAGE).append(getTypeNameWithoutParameters() + ".java"));
		}
		return null;
	}

	private String getTypeNameWithoutParameters() {
		final String typeNameWithParameters = getTypeName();
		final int angleBracketOffset = typeNameWithParameters.indexOf('<');
		if (angleBracketOffset == -1)
			return typeNameWithParameters;
		else
			return typeNameWithParameters.substring(0, angleBracketOffset);
	}

	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(final String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();
	}

	/**
	 * The wizard owning this page is responsible for calling this method with
	 * the current selection. The selection is used to initialize the fields of
	 * the wizard page.
	 * 
	 * @param selection
	 *            used to initialize the fields
	 */
	public void init(final IStructuredSelection selection) {
		final IJavaElement jelem = getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();
	}

	private void initTemplateVars() throws CoreException {
		templateVars = new HashMap<String, String>();
		templateVars.put("@className", getTypeName());
		templateVars.put("@basePackage", packageFragment.getElementName());
		templateVars.put("@clientPackage", packageFragment.getElementName() + '.' + Constants.CLIENT_PACKAGE);
		templateVars.put("@gwtUserPath", Util.getGwtUserLibPath().toString());
		templateVars.put("@gwtDevPath", Util.getGwtDevLibPath().toString());
		templateVars.put("@shellClass", "com.google.gwt.dev.GWTShell");
		templateVars.put("@projectName", getPackageFragment().getJavaProject().getElementName());
		templateVars.put("@startupUrl", packageFragment.getElementName() + '.' + getTypeNameWithoutParameters() + '/' + getTypeNameWithoutParameters() + ".html");
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible)
			setFocus();
	}
}
