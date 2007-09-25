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
package org.eclipseguru.gwt.core.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipseguru.gwt.core.GwtCore;

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
	public static boolean isPossibleGwtProject(IProject project) {
		if (!project.isAccessible())
			return false;
		try {
			return project.isNatureEnabled(JavaCore.NATURE_ID);
		} catch (CoreException e) {
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
		if (!isPossibleGwtProject(getProject()))
			throw new CoreException(GwtCore.newErrorStatus("Project must be a Java project!"));

		IProjectDescription desc = getProject().getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i)
			if (commands[i].getBuilderName().equals(GwtCore.BUILDER_ID)) {
				found = true;
				break;
			}
		if (!found) {
			// add builder to project
			ICommand gwtBuildCommand = desc.newCommand();
			gwtBuildCommand.setBuilderName(GwtCore.BUILDER_ID);

			// Add it after the Java builder.
			ICommand[] oldBuilders = desc.getBuildSpec();
			List<ICommand> newBuilders = new ArrayList<ICommand>(oldBuilders.length + 1);
			for (ICommand command : oldBuilders) {
				newBuilders.add(command);
				if (command.getBuilderName().equals(JavaCore.BUILDER_ID) && !newBuilders.contains(gwtBuildCommand))
					newBuilders.add(gwtBuildCommand);
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
		IProjectDescription desc = getProject().getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean found = false;

		for (int i = 0; i < commands.length; ++i)
			if (commands[i].getBuilderName().equals(GwtCore.BUILDER_ID)) {
				found = true;
				break;
			}
		if (found) {
			// remove builder from project
			ICommand[] newCommands = new ICommand[commands.length - 1];
			int j = 0;
			for (ICommand element : commands) {
				if (element.getBuilderName().equals(GwtCore.BUILDER_ID))
					continue;
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
		return this.project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
