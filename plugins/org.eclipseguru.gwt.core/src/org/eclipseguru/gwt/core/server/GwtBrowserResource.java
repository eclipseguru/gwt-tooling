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
package org.eclipseguru.gwt.core.server;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipseguru.gwt.core.GwtModule;

/**
 * A GWT Browser resource
 */
public class GwtBrowserResource implements IModuleArtifact {

	/** module */
	private final IModule module;

	/** path */
	private final IPath path;

	/** gwtModule */
	private final GwtModule gwtModule;

	/**
	 * Creates a new instance.
	 * 
	 * @param module
	 * @param path
	 */
	public GwtBrowserResource(IModule module, IPath path, GwtModule gwtModule) {
		this.module = module;
		this.path = path;
		this.gwtModule = gwtModule;
	}

	/**
	 * Returns the GWT module.
	 * 
	 * @return the gwtModule
	 */
	public GwtModule getGwtModule() {
		return gwtModule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.IModuleArtifact#getModule()
	 */
	public IModule getModule() {
		return module;
	}

	/**
	 * Return the relative path to the artifact within the module.
	 * 
	 * @return the relative path
	 */
	public IPath getPath() {
		return path;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "GwtBrowserResource [module=" + module + ", path=" + path + ", gwtmodule=" + gwtModule.getModuleId() + "]";
	}
}
