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
package org.eclipseguru.gwt.core.runtimes;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

/**
 * The GWT Runtime.
 */
public class GwtRuntime {

	/** NO_ATTRIBUTES */
	private static final IClasspathAttribute[] NO_ATTRIBUTES = new IClasspathAttribute[0];

	/** NO_RULES */
	private static final IAccessRule[] NO_RULES = new IAccessRule[0];

	/** Constant value indicating if the current platform is Windows */
	private static final boolean WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);

	/** path */
	private final IPath location;

	/**
	 * Creates a new instance
	 * 
	 * @param location
	 */
	public GwtRuntime(final IPath location) {
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
		if (WINDOWS)
			return "gwt-dev-windows.jar";
		return "gwt-dev-linux.jar";
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
	 * Returns the file system location where this runtime is installed.
	 * 
	 * @return the runtime install location
	 */
	public IPath getLocation() {
		return location;
	}

	private IPath getUserJar() {
		return location.append("gwt-user.jar");
	}
}
