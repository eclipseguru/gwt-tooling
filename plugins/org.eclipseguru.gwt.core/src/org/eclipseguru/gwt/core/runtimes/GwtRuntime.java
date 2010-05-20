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
package org.eclipseguru.gwt.core.runtimes;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * The GWT Runtime.
 */
public class GwtRuntime {

	/** EMPTY_PATH */
	private static final IPath EMPTY_PATH = new Path("");

	/** STRINGS */
	private static final String[] DEFAULT_VMARGS_MACOSX = new String[] { "-XstartOnFirstThread", "-Xmx256m" };

	/** NO_VMARGS */
	private static final String[] DEFAULT_VMARGS = new String[] { "-Xmx256m" };

	/** NO_ATTRIBUTES */
	private static final IClasspathAttribute[] NO_ATTRIBUTES = new IClasspathAttribute[0];

	/** NO_RULES */
	private static final IAccessRule[] NO_RULES = new IAccessRule[0];

	/** Constant value indicating if the current platform is Windows */
	//private static final boolean WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);

	/** Constant value indicating if the current platform is MacOSX */
	private static final boolean MACOSX = Platform.getOS().equals(Platform.OS_MACOSX);

	/** path */
	private final IPath location;

	/** name */
	private final String name;

	/**
	 * Creates a new instance
	 * 
	 * @param location
	 */
	public GwtRuntime(final String name, final IPath location) {

		if (null == name) {
			throw new IllegalArgumentException("name must not be null");
		}
		if (!EMPTY_PATH.isValidSegment(name)) {
			throw new IllegalArgumentException("name must be a valid file name and may not contain path separator characters");
		}
		if (null == location) {
			throw new IllegalArgumentException("name must not be null");
		}

		this.name = name;
		this.location = location;
	}

	/**
	 * Computes the classpath that should be contributed to a project classpath.
	 * 
	 * @return the classpath entries
	 */
	public IClasspathEntry[] computeClasspathEntries() {
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(getUserJar(), getUserJar(), null, NO_RULES, NO_ATTRIBUTES, false) };
	}

	private IPath getDevJar() {
		return location.append(getDevJarName());
	}

	/**
	 * Returns the jar name of the development library according to the platform
	 * the VM are running on.
	 * 
	 * @return the jar name
	 */
	private String getDevJarName() {
		// since GWT 2.0 it's one jar for all platforms
		return "gwt-dev.jar";
	}

	/**
	 * Returns the classpath necessary for launching this GWT runtime.
	 * 
	 * @return the classpath
	 */
	public String[] getGwtRuntimeClasspath() {
		return new String[] { getUserJar().toOSString(), getDevJar().toOSString() };
	}

	/**
	 * Returns the VM arguments necessary for launching this GWT runtime.
	 * 
	 * @return the VM arguments
	 */
	public String[] getGwtRuntimeVmArgs() {
		if (MACOSX) {
			return DEFAULT_VMARGS_MACOSX;
		}
		return DEFAULT_VMARGS;
	}

	/**
	 * Returns the file system location where this runtime is installed.
	 * 
	 * @return the runtime install location
	 */
	public IPath getLocation() {
		return location;
	}

	/**
	 * Returns the runtime name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	private IPath getServletJar() {
		return location.append("gwt-servlet.jar");
	}

	private IPath getUserJar() {
		return location.append("gwt-user.jar");
	}

	/**
	 * Returns the absolute path to GWT libraries which should be deployed into
	 * the <code>WEB-INF/lib</code> folder of web applications.
	 * 
	 * @return the absolute path to GWT libraries which should be deployed into
	 *         the <code>WEB-INF/lib</code> folder
	 */
	public IPath[] getWebAppLibraries() {
		return new IPath[] { getServletJar() };
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("GwtRuntime [name=").append(name).append(", location=").append(location).append("]");
		return builder.toString();
	}

}
