/*******************************************************************************
 * Copyright (c) 2006, 2008 EclipseGuru and others.
 * All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     EclipseGuru - initial API and implementation
 *******************************************************************************/
package org.eclipseguru.gwt.ui.editor;

import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.wst.xml.ui.internal.actions.ActionContributorXML;

/**
 * Module source editor {@link EditorActionBarContributor}
 */
public class ModuleSourceActionContributor extends ActionContributorXML {
	private static final String[] EDITOR_IDS = { "org.eclipseguru.gwt.ui.editor.module", "org.eclipse.wst.sse.ui.StructuredTextEditor" }; //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	protected String[] getExtensionIDs() {
		return EDITOR_IDS;
	}
}
