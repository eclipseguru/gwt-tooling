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
package org.eclipseguru.gwt.ui.preferences;

import org.eclipseguru.gwt.core.runtimes.GwtRuntime;
import org.eclipseguru.gwt.core.runtimes.GwtRuntimeManager;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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
	 * @param currentDirectory
	 * @return absolute path or an empty string if cancel.
	 */
	private String browseForGwtHomeDirectory(final String currentDirectory) {
		final DirectoryDialog fileDialog = new DirectoryDialog(getShell());
		fileDialog.setFilterPath(currentDirectory);
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
					final String directory = browseForGwtHomeDirectory(gwtHomeDirectoryDialogField.getText());
					gwtHomeDirectoryDialogField.setText(directory);
				}
			}
		});
		gwtHomeDirectoryDialogField.setLabelText("GWT Home Directory:");
		gwtHomeDirectoryDialogField.setButtonLabel("Browse...");
		gwtHomeDirectoryDialogField.setText(getPathFromPreferences());

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

	String getPathFromPreferences() {
		final GwtRuntime runtime = GwtRuntimeManager.findInstalledRuntime(null);
		if (runtime == null) {
			return "";
		}

		return runtime.getLocation().toOSString();
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
		// ensure we are up to date
		updateGwtHomeDirectoryStatus();

		// read value
		if ((null == gwtHomeDirectory) || !gwtHomeDirectoryStatus.isOK()) {
			return false;
		}

		// update the installed runtime
		try {
			GwtRuntimeManager.setActiveRuntime(new GwtRuntime(gwtHomeDirectory.lastSegment(), gwtHomeDirectory));
		} catch (final Exception e) {
			GwtUi.logError("Error saving preferences: " + e.getMessage(), e);
			return false;
		}

		return true;
	}

	@Override
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
