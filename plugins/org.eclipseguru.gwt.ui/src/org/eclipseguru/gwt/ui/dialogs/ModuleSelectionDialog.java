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
package org.eclipseguru.gwt.ui.dialogs;

import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A dialog for selecting a module from a project.
 * 
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class ModuleSelectionDialog extends ElementListSelectionDialog {

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param project
	 * @throws CoreException
	 */
	public ModuleSelectionDialog(final Shell parent, final GwtProject project) throws CoreException {
		this(parent, new GwtProject[] { project });
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param projects
	 * @throws CoreException
	 */
	public ModuleSelectionDialog(final Shell parent, final GwtProject[] projects) throws CoreException {
		super(parent, new WorkbenchLabelProvider());
		setTitle("GWT Module");
		setMessage("Select a GWT module:");
		final List<GwtModule> modules = new ArrayList<GwtModule>();
		for (final GwtProject project : projects)
			modules.addAll(Arrays.asList(project.getModules()));
		setElements(modules.toArray(new GwtModule[modules.size()]));
	}

	public GwtModule getSelectedModule() {
		return (GwtModule) getFirstResult();
	}

	public String getSelectedModuleName() {
		final GwtModule selectedModule = getSelectedModule();
		return selectedModule.getModuleId();
	}

}
