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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipseguru.gwt.core.launch.GwtLaunchConstants;

/**
 * 
 */
public class GwtShellArgumentsTab extends JavaArgumentsTab implements GwtLaunchConstants {

	private static final String[] logLevelButtonNames = new String[] { "Error", "Warnings", "Info", "Trace", "Debug", "Spam", "All" };
	private static final String[] styleButtonNames = new String[] { "Obfuscated", "Pretty", "Detailed" };

	private SelectionButtonDialogFieldGroup javascriptStyleDialogFieldGroup;
	private SelectionButtonDialogFieldGroup logLevelsDialogFieldGroup;

	/**
	 * Creates a new instance.
	 */
	public GwtShellArgumentsTab() {
		javascriptStyleDialogFieldGroup = new SelectionButtonDialogFieldGroup(SWT.RADIO, styleButtonNames, 1, SWT.SHADOW_NONE);
		javascriptStyleDialogFieldGroup.setLabelText("JavaScript Style:");

		logLevelsDialogFieldGroup = new SelectionButtonDialogFieldGroup(SWT.RADIO, logLevelButtonNames, 3, SWT.SHADOW_NONE);
		logLevelsDialogFieldGroup.setLabelText("Log Level:");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		result.setLayout(layout);

		final IDialogFieldListener launchDialogUpdater = new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				updateLaunchConfigurationDialog();
			}
		};

		logLevelsDialogFieldGroup.doFillIntoGrid(result, 1);
		logLevelsDialogFieldGroup.getSelectionButtonsGroup(result).setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		logLevelsDialogFieldGroup.setDialogFieldListener(launchDialogUpdater);

		javascriptStyleDialogFieldGroup.doFillIntoGrid(result, 1);
		javascriptStyleDialogFieldGroup.getSelectionButtonsGroup(result).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		javascriptStyleDialogFieldGroup.setDialogFieldListener(launchDialogUpdater);

		fVMArgumentsBlock.createControl(result);
		fVMArgumentsBlock.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		fWorkingDirectoryBlock.createControl(result);
		fWorkingDirectoryBlock.getControl().setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

		Dialog.applyDialogFont(result);
		setControl(result);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateStyleFromConfig(configuration);
		updateLogLevelFromConfig(configuration);
		fVMArgumentsBlock.initializeFrom(configuration);
		fWorkingDirectoryBlock.initializeFrom(configuration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		for (int i = 0; i < LOG_LEVELS.length; i++)
			if (logLevelsDialogFieldGroup.isSelected(i)) {
				configuration.setAttribute(ATTR_LOG_LEVEL, LOG_LEVELS[i]);
				break;
			}
		for (int i = 0; i < JAVSCRIPT_STYLES.length; i++)
			if (javascriptStyleDialogFieldGroup.isSelected(i)) {
				configuration.setAttribute(ATTR_STYLE, JAVSCRIPT_STYLES[i]);
				break;
			}
		fVMArgumentsBlock.performApply(configuration);
		fWorkingDirectoryBlock.performApply(configuration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_LOG_LEVEL, LOG_LEVELS[2]);
		configuration.setAttribute(ATTR_STYLE, JAVSCRIPT_STYLES[1]);
		fVMArgumentsBlock.setDefaults(configuration);
		fWorkingDirectoryBlock.setDefaults(configuration);
	}

	protected void updateLogLevelFromConfig(ILaunchConfiguration configuration) {
		String logLevel;
		try {
			logLevel = configuration.getAttribute(ATTR_LOG_LEVEL, LOG_LEVELS[2]);
		} catch (CoreException e) {
			logLevel = LOG_LEVELS[2];
		}
		for (int i = 0; i < LOG_LEVELS.length; i++)
			logLevelsDialogFieldGroup.setSelection(i, LOG_LEVELS[i].equals(logLevel));
	}

	protected void updateStyleFromConfig(ILaunchConfiguration configuration) {
		String style;
		try {
			style = configuration.getAttribute(ATTR_STYLE, JAVSCRIPT_STYLES[1]);
		} catch (CoreException e) {
			style = JAVSCRIPT_STYLES[1];
		}
		for (int i = 0; i < JAVSCRIPT_STYLES.length; i++)
			javascriptStyleDialogFieldGroup.setSelection(i, JAVSCRIPT_STYLES[i].equals(style));
	}

}
