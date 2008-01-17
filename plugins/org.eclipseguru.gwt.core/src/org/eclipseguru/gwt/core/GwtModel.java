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
package org.eclipseguru.gwt.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The GWT model element.
 * <p>
 * This is the main model element. It has no parent and is the root of the GWT
 * model hierarchy.
 * </p>
 */
public class GwtModel extends GwtElement {

	/**
	 * The GWT model.
	 * 
	 * @param the
	 *            model manager
	 */
	GwtModel() {
		super(null);
	}

	/**
	 * Creates a new project from the specified resources
	 * 
	 * @param project
	 * @return
	 */
	GwtProject createProject(final IResource resource) {
		if (null == resource)
			return null;

		switch (resource.getType()) {
			case IResource.FILE:
				return new GwtProject(((IFile) resource).getProject(), this);
			case IResource.FOLDER:
				return new GwtProject(((IFolder) resource).getProject(), this);
			case IResource.PROJECT:
				return new GwtProject(((IProject) resource).getProject(), this);
			default:
				throw new IllegalArgumentException("The specified resource could not be resolved to a project!");
		}
	}

	/**
	 * Returns the GWT projects.
	 * 
	 * @return the GWT projects
	 */
	public GwtProject[] getProjects() {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects.length == 0)
			return new GwtProject[0];

		final List<GwtProject> gwtProjects = new ArrayList<GwtProject>(projects.length);
		for (final IProject project : projects)
			if (GwtProject.hasGwtNature(project))
				gwtProjects.add(createProject(project));
		return gwtProjects.toArray(new GwtProject[gwtProjects.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.GwtElement#getType()
	 */
	@Override
	public int getType() {
		return GWT_MODEL;
	}

}
