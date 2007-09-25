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
package org.eclipseguru.gwt.ui.adapters;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipseguru.gwt.core.GwtElement;
import org.eclipseguru.gwt.core.GwtModel;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;

/**
 * A workbench adapter for GWT element.
 */
public class GwtElementWorkbenchAdapter implements IWorkbenchAdapter {

	/** NO_ELEMENT */
	private static final Object[] NO_CHILDREN = new Object[0];
	/** SHARED_INSTANCE */
	public static final GwtElementWorkbenchAdapter SHARED_INSTANCE = new GwtElementWorkbenchAdapter();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		try {
			if (o instanceof GwtModel)
				return ((GwtModel) o).getProjects();
			else if (o instanceof GwtProject)
				return ((GwtProject) o).getModules();
		} catch (GwtModelException e) {
			// ignore
		}
		return NO_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		if (o instanceof GwtModel)
			return "";
		else if (o instanceof GwtProject)
			return ((GwtProject) o).getName();
		else if (o instanceof GwtModule)
			return ((GwtModule) o).getSimpleName();
		return "[unknown]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		if (o instanceof GwtElement)
			return ((GwtElement) o).getParent();
		return null;
	}
}
