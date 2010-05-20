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
package org.eclipseguru.gwt.core.launch;

import org.eclipseguru.gwt.core.GwtCore;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Shared launch constants.
 */
public interface GwtLaunchConstants {

	/** the GWT Browser launch type */
	String TYPE_GWT_BROWSER = GwtCore.PLUGIN_ID + ".launch.browser";

	/** prefix for all launch attributes */
	String ATTR_PREFIX = GwtCore.PLUGIN_ID + ".launch.attribute.";

	/** the project name */
	String ATTR_PROJECT_NAME = IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME;

	/** the module id */
	String ATTR_MODULE_ID = ATTR_PREFIX + "moduleId";

	/** indicates if the internal server should be started */
	String ATTR_NOSERVER = ATTR_PREFIX + "noserver";

	/** the url to open */
	String ATTR_URL = ATTR_PREFIX + "url";

	/** the log level */
	String ATTR_LOG_LEVEL = ATTR_PREFIX + "loglevel";

	/** the style */
	String ATTR_STYLE = ATTR_PREFIX + "style";

	/** the port */
	String ATTR_PORT = ATTR_PREFIX + "port";

	/**
	 * a boolean attribute that indicates if a custom url is configure or a
	 * default one is to be used
	 */
	String ATTR_CUSTOM_URL = ATTR_PREFIX + "customURL";

	/**
	 * Status code indicating a launch configuration does not specify a project
	 * to launch.
	 */
	int ERR_UNSPECIFIED_PROJECT = -100;

	/**
	 * Status code indicating a launch configuration does not specify a module
	 * id to launch.
	 */
	int ERR_UNSPECIFIED_MODULE_ID = -101;

	/**
	 * Status code indicating a launch configuration specifies a module which
	 * could not be found in the project it specifies.
	 */
	int ERR_MODULE_NOT_FOUND = -102;

	/**
	 * The available values for the {@link #ATTR_LOG_LEVEL log level attribute}
	 * (values <code>ERROR</code>, <code>WARN</code>, <code>INFO</code>,
	 * <code>TRACE</code>, <code>DEBUG</code>, <code>SPAM</code> and
	 * <code>ALL</code>).
	 */
	String[] LOG_LEVELS = new String[] { "ERROR", "WARN", "INFO", "TRACE", "DEBUG", "SPAM", "ALL" };

	/** Javascript style (value <code>OBFUSCATED</code>) */
	String JAVSCRIPT_STYLE_OBFUSCATED = "OBFUSCATED";

	/** Javascript style (value <code>PRETTY</code>) */
	String JAVSCRIPT_STYLE_PRETTY = "PRETTY";

	/** Javascript style (value <code>DETAILED</code>) */
	String JAVSCRIPT_STYLE_DETAILED = "DETAILED";

	/**
	 * The available values for the {@link #ATTR_STYLE style attribute} (values
	 * {@value #JAVSCRIPT_STYLE_OBFUSCATED}, {@value #JAVSCRIPT_STYLE_PRETTY}
	 * and {@value #JAVSCRIPT_STYLE_DETAILED}).
	 */
	String[] JAVSCRIPT_STYLES = new String[] { JAVSCRIPT_STYLE_OBFUSCATED, JAVSCRIPT_STYLE_PRETTY, JAVSCRIPT_STYLE_DETAILED };
}
