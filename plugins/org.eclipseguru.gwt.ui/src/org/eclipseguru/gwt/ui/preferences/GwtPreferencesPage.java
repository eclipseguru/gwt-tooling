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
package org.eclipseguru.gwt.ui.preferences;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.File;

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
	 * Creates a new instance.
	 */
	public GwtPreferencesPage() {
		setDescription("General settings for GWT development:");
	}

	/**
	 * Helper that opens the directory chooser dialog.
	 * 
	 * @return absolute path or an empty string if cancel.
	 */
	private String browseForGwtHomeDirectory() {
		final DirectoryDialog fileDialog = new DirectoryDialog(getShell());
		String directory = fileDialog.open();
		if (directory != null) {
			directory = directory.trim();
			if (directory.length() > 0) {
				return directory;
			}
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);

		gwtHomeDirectoryDialogField = new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(final DialogField field) {
				if (gwtHomeDirectoryDialogField == field) {
					final String directory = browseForGwtHomeDirectory();
					gwtHomeDirectoryDialogField.setText(directory);
				}
			}
		});
		gwtHomeDirectoryDialogField.setLabelText("GWT Home Directory:");
		gwtHomeDirectoryDialogField.setButtonLabel("Browse...");
		gwtHomeDirectoryDialogField.setText(getPathFromPreferencesFor(GwtCorePreferenceConstants.PREF_GWT_HOME));

		// we add the update listeners last to avoid coming up with errors
		gwtHomeDirectoryDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(final DialogField field) {
				if (field == gwtHomeDirectoryDialogField) {
					updateGwtHomeDirectoryStatus();
				}
				doStatusLineUpdate();
			}
		});

		LayoutUtil.doDefaultLayout(composite, new DialogField[] { gwtHomeDirectoryDialogField }, true);
		LayoutUtil.setHorizontalGrabbing(gwtHomeDirectoryDialogField.getTextControl(composite));
		return composite;
	}

	private void doStatusLineUpdate() {
		if (Display.getCurrent() != null) {
			final IStatus res = findMostSevereStatus();
			statusChanged(res);
		}
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { gwtHomeDirectoryStatus });
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
		final String text = gwtHomeDirectoryDialogField.getText();
		if (text.length() != 0) {
			return new Path(gwtHomeDirectoryDialogField.getText()).makeAbsolute();
		}
		return null;
	}

	String getPathFromPreferencesFor(final String preference) {
		final Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
		final String preferenceValue = pluginPreferences.getString(preference);
		if ((null == preferenceValue) || (preferenceValue.trim().length() == 0)) {
			return "";
		}

		final IPath path = Path.fromPortableString(preferenceValue);
		if (path.isEmpty()) {
			return "";
		}

		return path.toOSString();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(final IWorkbench workbench) {
		// empty
	}

	boolean isGwtJarFound() {
		final String gwtJarPath = gwtHomeDirectory.toOSString() + File.separator + "gwt-user.jar";
		final File gwtJarFile = new File(gwtJarPath);
		return gwtJarFile.exists();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (null == gwtHomeDirectory) {
			return false;
		}

		final Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
		pluginPreferences.setValue(GwtCorePreferenceConstants.PREF_GWT_HOME, gwtHomeDirectory.toPortableString());

		GwtCore.getGwtCore().savePluginPreferences();

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener#statusChanged
	 * (org.eclipse.core.runtime.IStatus)
	 */
	public void statusChanged(final IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
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
