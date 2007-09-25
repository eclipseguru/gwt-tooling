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
package org.eclipseguru.gwt.ui.editor;

import org.eclipse.ui.forms.editor.FormPage;

/**
 * The overview page for the module source editor.
 */
public class ModuleSourceOverviewPage extends FormPage {

	/**
	 * Creates a new instance
	 * 
	 * @param editor
	 */
	public ModuleSourceOverviewPage(ModuleSourceEditor editor) {
		super(editor, "overview", "Overview");
	}

	/**
	 * @param moduleSource
	 */
	public void setModel(ModuleSourceWC moduleSource) {
		// TODO Auto-generated method stub

	}

}
