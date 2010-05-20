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
package org.eclipseguru.gwt.core.project;

import org.eclipseguru.gwt.core.GwtCore;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.List;

/**
 * The project nature for GWT projects.
 */
public class GwtProjectNature implements IProjectNature {

	/**
	 * Indicates if the project is a possible GWT project, i.e. has the Java
	 * nature attached.
	 * 
	 * @param project
	 * @return <code>true</code> if the project is a possible GWT project,
	 *         <code>false</code> otherwise
	 */
	public static boolean isPossibleGwtProject(final IProject project) {
		if (!project.isAccessible())
			return false;
		try {
			return project.isNatureEnabled(JavaCore.NATURE_ID);
		} catch (final CoreException e) {
			// project is closed or does not exists
			return false;
		}
	}

	/** project */
	private IProject project;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		if (!isPossibleGwtProject(getProject())) {
			throw new CoreException(GwtCore.newErrorStatus("Project must be a Java project!"));
		}

		final IProjectDescription desc = getProject().getDescription();
		final ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i)
			if (commands[i].getBuilderName().equals(GwtCore.BUILDER_ID)) {
				found = true;
				break;
			}
		if (!found) {
			// add builder to project
			final ICommand gwtBuildCommand = desc.newCommand();
			gwtBuildCommand.setBuilderName(GwtCore.BUILDER_ID);

			// Add it after the Java builder.
			final ICommand[] oldBuilders = desc.getBuildSpec();
			final List<ICommand> newBuilders = new ArrayList<ICommand>(oldBuilders.length + 1);
			for (final ICommand command : oldBuilders) {
				newBuilders.add(command);
				if (command.getBuilderName().equals(JavaCore.BUILDER_ID) && !newBuilders.contains(gwtBuildCommand)) {
					newBuilders.add(gwtBuildCommand);
				}
			}
			desc.setBuildSpec(newBuilders.toArray(new ICommand[newBuilders.size()]));
			getProject().setDescription(desc, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		final IProjectDescription desc = getProject().getDescription();
		final ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i)
			if (commands[i].getBuilderName().equals(GwtCore.BUILDER_ID)) {
				found = true;
				break;
			}
		if (found) {
			// remove builder from project
			final ICommand[] newCommands = new ICommand[commands.length - 1];
			int j = 0;
			for (final ICommand element : commands) {
				if (element.getBuilderName().equals(GwtCore.BUILDER_ID)) {
					continue;
				}
				newCommands[j] = element;
				j++;
			}

			desc.setBuildSpec(newCommands);
			getProject().setDescription(desc, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(final IProject project) {
		this.project = project;
	}

}
