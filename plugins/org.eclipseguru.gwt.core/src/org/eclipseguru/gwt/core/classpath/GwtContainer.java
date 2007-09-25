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
package org.eclipseguru.gwt.core.classpath;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipseguru.gwt.core.runtimes.GwtRuntime;

/**
 * The GWT classpath container
 */
public class GwtContainer implements IClasspathContainer {

	/**
	 * Computes the classpath entries for the specified GWT.
	 * 
	 * @param gwtRuntime
	 * @return the classpath entries
	 */
	private static IClasspathEntry[] computeClasspathEntries(GwtRuntime gwtRuntime) {
		return gwtRuntime.computeClasspathEntries();
	}

	/**
	 * GWT installation
	 */
	private final GwtRuntime gwtRuntime;

	/**
	 * Container path used to resolve to this GWT
	 */
	private final IPath containerPath;

	/** computedEntries */
	private IClasspathEntry[] computedEntries;

	/**
	 * @param gwtRuntime
	 * @param containerPath
	 */
	public GwtContainer(GwtRuntime gwtRuntime, IPath containerPath) {
		this.gwtRuntime = gwtRuntime;
		this.containerPath = containerPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (null == computedEntries)
			computedEntries = computeClasspathEntries(gwtRuntime);
		return computedEntries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return MessageFormat.format("Google Web Toolkit [{0}]", gwtRuntime.getLocation().toOSString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return containerPath;
	}

}
