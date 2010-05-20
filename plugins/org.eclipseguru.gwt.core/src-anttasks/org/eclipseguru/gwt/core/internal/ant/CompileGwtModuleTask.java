/***************************************************************************************************
 * Copyright (c) 2006, 2008 Eclipse Guru and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Eclipse Guru - initial API and implementation
 *               Eclipse.org - ideas, concepts and code from existing Eclipse projects
 **************************************************************************************************/
package org.eclipseguru.gwt.core.internal.ant;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.builder.GwtProjectPublisher;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant Task for compiling a GWT module.
 */
public class CompileGwtModuleTask extends Task {

	private String moduleId;

	/*
	 * (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		final String moduleId = getModuleId();
		if (null == moduleId) {
			throw new BuildException("GWT module not specified");
		}

		// find module
		final GwtModule module = findModule(moduleId);
		if (null == module) {
			throw new BuildException("GWT module '" + moduleId + "' not found in workspace");
		}

		// publish project and block while publishing
		final GwtProjectPublisher publisher = new GwtProjectPublisher(module);
		try {
			publisher.schedule();
			publisher.join();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private GwtModule findModule(final String moduleId) throws BuildException {
		final GwtProject[] projects = GwtCore.getModel().getProjects();
		for (final GwtProject project : projects) {
			try {
				final GwtModule module = project.getModule(moduleId);
				if (null != module) {
					return module;
				}
			} catch (final GwtModelException e) {
				log("Error while reading modules in project " + project.getName(), e, 0);
			}
		}
		return null;
	}

	/**
	 * @return the moduleId
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * @param moduleId
	 *            the moduleId to set
	 */
	public void setModuleId(final String moduleId) {
		this.moduleId = moduleId;
	}
}
