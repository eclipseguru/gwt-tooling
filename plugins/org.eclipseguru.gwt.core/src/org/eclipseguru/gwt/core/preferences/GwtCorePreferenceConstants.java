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
package org.eclipseguru.gwt.core.preferences;

import org.eclipseguru.gwt.core.launch.GwtLaunchConstants;

/**
 * Interface with core preference constants.
 */
public interface GwtCorePreferenceConstants {

	/** the node containing the runtime definitions */
	String PREF_GWT_RUNTIMES = "runtimes";

	/** PREF_HOSTED_DEPLOY_MODE */
	String PREF_HOSTED_DEPLOY_MODE = "hostedDeployMode";

	/** PREF_OUTPUT_LOCATION */
	String PREF_OUTPUT_LOCATION = "outputLocation";

	/** PREF_INCLUDED_MODULES */
	String PREF_INCLUDED_MODULES = "includedModules";

	/** PREF_DEPLOYMENT_PATH */
	String PREF_DEPLOYMENT_PATH = "deploymentPath";

	/** PREF_VM_ARGS */
	String PREF_COMPILER_VM_ARGS = "compilerVmArgs";

	/**
	 * PREF_COMPILER_STYLE
	 * 
	 * @see GwtLaunchConstants#JAVSCRIPT_STYLES
	 */
	String PREF_COMPILER_JAVASCRIPT_STYLE = "compilerJavascriptStyle";

	String PREF_AUTO_BUILD_MODULES = "autoBuildModules";

	/** the location key of a GWT runtime */
	String PREF_LOCATION = "location";
}
