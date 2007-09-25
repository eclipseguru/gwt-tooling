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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

/**
 * The GWT Tooling preferences page.
 */
public class GwtPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage, GwtCorePreferenceConstants, IStatusChangeListener {

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
		super();
		setDescription("General settings for GWT development:");

		gwtHomeDirectoryDialogField = new StringButtonDialogField(new IStringButtonAdapter() {
			public void changeControlPressed(DialogField field) {
				if (gwtHomeDirectoryDialogField == field)
					selectGwtHomeDirectoryButtonPressed();
			}
		});
		gwtHomeDirectoryDialogField.setLabelText("GWT Home Directory:");
		gwtHomeDirectoryDialogField.setButtonLabel("Browse...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		gwtHomeDirectoryDialogField.setText(getHomeDirectory());
		gwtHomeDirectoryDialogField.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				gwtHomeDirectoryDialogFieldChanged(field);
			}
		});
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { gwtHomeDirectoryDialogField }, true);
		LayoutUtil.setHorizontalGrabbing(gwtHomeDirectoryDialogField.getTextControl(composite));
		return composite;
	}

	private void doStatusLineUpdate() {
		if (Display.getCurrent() != null) {
			IStatus res = findMostSevereStatus();
			this.statusChanged(res);
		}
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { gwtHomeDirectoryStatus });
	}

	/**
	 * Helper that opens the directory chooser dialog.
	 * 
	 * @param startingDirectory
	 *            The directory the dialog will open in.
	 * @return File File or <code>null</code>.
	 * 
	 */
	private File getDirectory(File startingDirectory) {

		DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN);
		if (startingDirectory != null)
			fileDialog.setFilterPath(startingDirectory.getPath());
		String dir = fileDialog.open();
		if (dir != null) {
			dir = dir.trim();
			if (dir.length() > 0)
				return new File(dir);
		}

		return null;
	}

	/**
	 * Returns the current GWT home directory location. Note that the path
	 * returned must not be valid.
	 * 
	 * @return the current GWT home directory location
	 */
	public IPath getGwtHomeDirectory() {
		return new Path(gwtHomeDirectoryDialogField.getText()).makeAbsolute();
	}

	/**
	 * Returns the value of the home directory from the plugin preferences.
	 * 
	 * @return the value of the home directory from the plugin preferences
	 */
	private String getHomeDirectory() {
		Preferences pluginPreferences = GwtCore.getGwtCore().getPluginPreferences();
		String gwtHome = pluginPreferences.getString(GwtCorePreferenceConstants.PREF_GWT_HOME);
		if ((null == gwtHome) || (gwtHome.trim().length() == 0))
			return "";

		IPath path = Path.fromPortableString(gwtHome);
		if (path.isEmpty())
			return "";

		return path.toOSString();
	}

	private void gwtHomeDirectoryDialogFieldChanged(DialogField field) {
		if (field == gwtHomeDirectoryDialogField)
			updateGwtHomeDirectoryStatus();
		doStatusLineUpdate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// empty
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
		pluginPreferences.setValue(PREF_GWT_HOME, gwtHomeDirectory.toPortableString());
		GwtCore.getGwtCore().savePluginPreferences();

		return super.performOk();
	}

	void selectGwtHomeDirectoryButtonPressed() {
		File f = new File(gwtHomeDirectoryDialogField.getText());
		if (!f.exists())
			f = null;
		File d = getDirectory(f);
		if (d == null)
			return;

		gwtHomeDirectoryDialogField.setText(d.getAbsolutePath());
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

	private void updateGwtHomeDirectoryStatus() {
		gwtHomeDirectory = null;

		String text = gwtHomeDirectoryDialogField.getText();
		if ("".equals(text)) { //$NON-NLS-1$
			gwtHomeDirectoryStatus.setError("The GWT home directoy must be entered.");
			return;
		}

		gwtHomeDirectory = getGwtHomeDirectory();

		if (!gwtHomeDirectory.toFile().exists()) {
			gwtHomeDirectoryStatus.setError("The GWT home directoy does not exists.");
			return;
		}

		if (!gwtHomeDirectory.toFile().isDirectory()) {
			gwtHomeDirectoryStatus.setError("The GWT home directoy must be a directory.");
			return;
		}

		gwtHomeDirectoryStatus.setOK();
	}
}
