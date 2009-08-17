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
package com.googlipse.gwt.common;

import org.eclipseguru.gwt.core.GwtCore;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class Constants {

	public static final String PLUGIN_ID = GwtCore.PLUGIN_ID;

	// Attributes for Lauch configuration
	public static final String LAUNCH_ATTR_HEADLESS = PLUGIN_ID + ".launchAttrHeadless";
	public static final String LAUNCH_ATTR_PORT = PLUGIN_ID + ".launchAttrPort";
	public static final String LAUNCH_ATTR_LOGLEVEL = PLUGIN_ID + ".launchAttrLogLevel";
	public static final String LAUNCH_ATTR_OUTDIR = PLUGIN_ID + ".launchAttrOutDir";
	public static final String LAUNCH_ATTR_STYLE = PLUGIN_ID + ".launchAttrStyle";

	public static final int UNEXPECTED_ERROR = 50001;

	public static final String GWT_SHELL_CLASS = "com.google.gwt.dev.GWTShell";
	public static final String GWT_COMPILER_CLASS = "com.google.gwt.dev.Compiler";

	public static final String GWT_XML_EXT = "gwt.xml";

	public static final String PUBLIC_FOLDER = "public";

	public static final String SERVER_PACKAGE = "server";

	public static final String CLIENT_PACKAGE = "client";
}
