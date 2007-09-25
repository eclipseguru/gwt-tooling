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
package org.eclipseguru.gwt.ui.server;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ClientDelegate;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.launch.GwtLaunchConstants;
import org.eclipseguru.gwt.core.launch.GwtLaunchUtil;
import org.eclipseguru.gwt.core.server.GwtBrowserLaunchable;
import org.eclipseguru.gwt.ui.GwtUi;

/**
 * This is the launchable GWT browser.
 */
public class GwtBrowserClient extends ClientDelegate {

	/**
	 * Prompts the user with a list of launch configurations to select from.
	 * 
	 * @param configList
	 * @param mode
	 * @return the selected configuration or <code>null</code> if the user
	 *         clicked cancel
	 */
	protected static ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(GwtUi.getActiveWorkbenchShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle("Select a Browser Configuration");
		if (mode.equals(ILaunchManager.DEBUG_MODE))
			dialog.setMessage("Select GWT Browser configuration to debug");
		else
			dialog.setMessage("Select GWT Browser configuration to run");

		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK)
			return (ILaunchConfiguration) dialog.getFirstResult();

		return null;
	}

	private static ILaunchConfiguration findGwtBrowserLaunchConfiguration(GwtModule module, String mode) {
		String moduleId = module.getModuleId();
		String projectName = module.getProjectName();
		ILaunchConfigurationType configType = GwtLaunchUtil.getGwtBrowserLaunchConfigurationType();
		List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);
			for (ILaunchConfiguration config : configs) {
				if ((config.getAttribute(GwtLaunchConstants.ATTR_MODULE_ID, "").equals(moduleId)) && //$NON-NLS-1$
						(config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(projectName)))
					candidateConfigs.add(config);
			}
		} catch (CoreException e) {
			GwtUi.logError(MessageFormat.format("Error while searching for existing GWT Browser launch configurations for module {0}", module.getModuleId()), e);
		}

		/*
		 * If there are no existing configs associated with the module, create
		 * one. If there is exactly one config associated with the module,
		 * return it. Otherwise, if there is more than one config associated
		 * with the module, prompt the user to choose one.
		 */
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1)
			return null;
		else if (candidateCount == 1)
			return candidateConfigs.get(0);
		else {
			/*
			 * Prompt the user to choose a config. A null result means the user
			 * cancelled the dialog, in which case this method returns null,
			 * since cancelling the dialog should also cancel launching
			 * anything.
			 */
			ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
			if (config != null)
				return config;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.ClientDelegate#launch(org.eclipse.wst.server.core.IServer,
	 *      java.lang.Object, java.lang.String, org.eclipse.debug.core.ILaunch)
	 */
	@Override
	public IStatus launch(IServer server, Object launchable, String launchMode, ILaunch launch) {
		if (!(launchable instanceof GwtBrowserLaunchable))
			return Status.CANCEL_STATUS;

		GwtBrowserLaunchable browserLaunchable = (GwtBrowserLaunchable) launchable;
		GwtModule module = browserLaunchable.getGwtModule();
		URL url = browserLaunchable.getURL();

		ILaunchConfiguration config = findGwtBrowserLaunchConfiguration(module, launchMode);
		if (config == null)
			config = GwtLaunchUtil.createGwtBrowserLaunchConfiguration(module, url.toString(), true);

		DebugUITools.launch(config, launchMode);
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.ClientDelegate#supports(org.eclipse.wst.server.core.IServer,
	 *      java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean supports(IServer server, Object launchable, String launchMode) {
		return launchable instanceof GwtBrowserLaunchable;
	}
}
