/***************************************************************************************************
 * Copyright (c) 2007,2006 Eclipse Guru and others.
 * All rights reserved. 
 *
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Eclipse Guru - initial API and implementation
 *               Eclipse.org - ideas, concepts and code from existing Eclipse projects
 *               Hugo Garcia - contribution for issue #9
 **************************************************************************************************/
package org.eclipseguru.gwt.ui.preferences;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

/**
 * The GWT Tooling preferences page.
 */
public class GwtPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage, IStatusChangeListener {

	public static final String ID = "com.googlipse.gwt.preferences.PreferencePage";

	/** gwtHomeDirectoryDialogField */
	private StringButtonDialogField gwtHomeDirectoryDialogField;

	/** gwtHomeDirectoryPath */
	private IPath gwtHomeDirectory;

	/** gwtHomeDirectoryPathStatus */
	private final StatusInfo gwtHomeDirectoryStatus = new StatusInfo();

	/**
	 * Field containing input from user for path to template.
	 */
	private StringButtonDialogField customModuleTemplatePathField;

	/**
	 * Path to the custom module template.
	 */
	private IPath customModuleTemplatePath;

	/** status info for the custom module template path */
	private final StatusInfo customModuleTemplatePathStatus = new StatusInfo();

	/**
	 * Creates a new instance.
	 */
	public GwtPreferencesPage() {
		setDescription("General settings for GWT development:");
	}

	private void browseForCustomModuleTemplateFile() {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
		fileDialog.setText("Select Module Template");
		String currentPath = customModuleTemplatePathField.getText();
		if (currentPath.length() > 0)
			fileDialog.setFileName(currentPath);
		String directory = fileDialog.open();
		if (directory != null)
			customModuleTemplatePathField.setText(directory);
	}

	/**
	 * Helper that opens the directory chooser dialog.
	 * 
	 * @return absolute path or an empty string if cancel.
	 * 
	 */
	private String browseForGwtHomeDirectory() {
		DirectoryDialog fileDialog = new DirectoryDialog(getShell());
		String directory = fileDialog.open();
		if (directory != null) {
			directory = directory.trim();
			if (directory.length() > 0)
				return directory;
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		gwtHomeDirectoryDialogField = new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				if (gwtHomeDirectoryDialogField == field) {
					String directory = browseForGwtHomeDirectory();
					gwtHomeDirectoryDialogField.setText(directory);
				}
			}
		});
		gwtHomeDirectoryDialogField.setLabelText("GWT Home Directory:");
		gwtHomeDirectoryDialogField.setButtonLabel("Browse...");
		gwtHomeDirectoryDialogField.setText(getPathFromPreferencesFor(GwtCorePreferenceConstants.PREF_GWT_HOME));

		customModuleTemplatePathField = new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				if (field == customModuleTemplatePathField)
					browseForCustomModuleTemplateFile();
			}
		});
		customModuleTemplatePathField.setLabelText("Custom Module Template:");
		customModuleTemplatePathField.setButtonLabel("Browse...");
		customModuleTemplatePathField.setText(getPathFromPreferencesFor(GwtCorePreferenceConstants.PREF_CUSTOM_MODULE_TEMPLATE_PATH));

		// we add the update listeners last to avoid coming up with errors
		gwtHomeDirectoryDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				if (field == gwtHomeDirectoryDialogField)
					updateGwtHomeDirectoryStatus();
				doStatusLineUpdate();
			}
		});
		customModuleTemplatePathField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				if (field == customModuleTemplatePathField) {
					updateCustomModuleTemplatePath();
					doStatusLineUpdate();
				}
			}
		});

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { gwtHomeDirectoryDialogField, customModuleTemplatePathField }, true);
		LayoutUtil.setHorizontalGrabbing(gwtHomeDirectoryDialogField.getTextControl(composite));
		LayoutUtil.setHorizontalGrabbing(customModuleTemplatePathField.getTextControl(composite));
		return composite;
	}

	private void doStatusLineUpdate() {
		if (Display.getCurrent() != null) {
			IStatus res = findMostSevereStatus();
			statusChanged(res);
		}
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { gwtHomeDirectoryStatus, customModuleTemplatePathStatus });
	}

	IPath getCustomModuleTemplatePath() {
		return customModuleTemplatePath;
	}

	StringButtonDialogField getCustomModuleTemplatePathField() {
		return customModuleTemplatePathField;
	}

	IPath getCustomModuleTemplatePathFromTextField() {
		String text = customModuleTemplatePathField.getText();
		if (text.trim().length() != 0)
			return new Path(customModuleTemplatePathField.getText()).makeAbsolute();
		return null;
	}

	IPath getGwtHomeDirectory() {
		return gwtHomeDirectory;
	}

	StringButtonDialogField getGwtHomeDirectoryDialogField() {
		return gwtHomeDirectoryDialogField;
	}

	/**
	 * Returns the current GWT home directory location entered by the user.
	 * 
	 * @return null if not set else an absolute IPath
	 */
	IPath getGwtHomeDirectoryPathFromTextField() {
		String text = gwtHomeDirectoryDialogField.getText();
		if (text.length() != 0)
			return new Path(gwtHomeDirectoryDialogField.getText()).makeAbsolute();
		return null;
	}

	String getPathFromPreferencesFor(String preference) {
		Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
		String preferenceValue = pluginPreferences.getString(preference);
		if ((null == preferenceValue) || (preferenceValue.trim().length() == 0))
			return "";

		IPath path = Path.fromPortableString(preferenceValue);
		if (path.isEmpty())
			return "";

		return path.toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// empty
	}

	boolean isGwtJarFound() {
		String gwtJarPath = gwtHomeDirectory.toOSString() + File.separator + "gwt-user.jar";
		File gwtJarFile = new File(gwtJarPath);
		return gwtJarFile.exists();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (null == gwtHomeDirectory)
			return false;

		Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
		pluginPreferences.setValue(GwtCorePreferenceConstants.PREF_GWT_HOME, gwtHomeDirectory.toPortableString());
		if (null != customModuleTemplatePath)
			pluginPreferences.setValue(GwtCorePreferenceConstants.PREF_CUSTOM_MODULE_TEMPLATE_PATH, customModuleTemplatePath.toPortableString());
		else
			pluginPreferences.setToDefault(GwtCorePreferenceConstants.PREF_CUSTOM_MODULE_TEMPLATE_PATH);

		GwtCore.getGwtCore().savePluginPreferences();

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged(org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	private void updateCustomModuleTemplatePath() {
		customModuleTemplatePath = getCustomModuleTemplatePathFromTextField();
		if ((customModuleTemplatePath != null) && !gwtHomeDirectory.toFile().exists()) {
			customModuleTemplatePathStatus.setError("The specified module template does not exists.");
			return;
		}
		customModuleTemplatePathStatus.setOK();
	}

	private void updateGwtHomeDirectoryStatus() {
		gwtHomeDirectory = getGwtHomeDirectoryPathFromTextField();

		if (gwtHomeDirectory == null) {
			gwtHomeDirectoryStatus.setError("The GWT home directoy must be entered.");
			return;
		}

		if (!gwtHomeDirectory.toFile().exists()) {
			gwtHomeDirectoryStatus.setError("The GWT home directoy does not exists.");
			return;
		}

		if (!gwtHomeDirectory.toFile().isDirectory()) {
			gwtHomeDirectoryStatus.setError("The GWT home directoy must be a directory.");
			return;
		}

		if (!isGwtJarFound()) {
			gwtHomeDirectoryStatus.setError("The GWT jars are not found. Please change directory.");
			return;
		}

		gwtHomeDirectoryStatus.setOK();
	}

}
