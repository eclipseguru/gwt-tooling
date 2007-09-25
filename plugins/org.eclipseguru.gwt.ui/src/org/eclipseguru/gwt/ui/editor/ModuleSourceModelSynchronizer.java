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

import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

/**
 * This adapter reacts to any and all notifications and triggers a Tree refresh
 * on the affected notifier. This is for demonstration purposes only since this
 * can easily tie up the UI Thread performing unnecessary updates. Normally
 * refreshes would be requested more judiciously.
 */
class ModuleSourceModelSynchronizer extends Object implements INodeAdapter {
	private ModuleSourceWC fDesignViewer = null;

	ModuleSourceModelSynchronizer(ModuleSourceWC moduleSource) {
		super();
		fDesignViewer = moduleSource;
	}

	public boolean isAdapterForType(Object type) {
		return type.equals(ModuleSourceModelSynchronizer.class);
	}

	public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
		// Refresh this node
	}
}
