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

import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

/**
 * This adapter reacts to any and all notifications and triggers a Tree refresh
 * on the affected notifier. This is for demonstration purposes only since this
 * can easily tie up the UI Thread performing unnecessary updates. Normally
 * refreshes would be requested more judiciously.
 */
class ModuleSourceModelSynchronizer extends Object implements INodeAdapter {
	ModuleSourceModelSynchronizer(final ModuleSourceWC moduleSource) {
		super();
	}

	public boolean isAdapterForType(final Object type) {
		return type.equals(ModuleSourceModelSynchronizer.class);
	}

	public void notifyChanged(final INodeNotifier notifier, final int eventType, final Object changedFeature, final Object oldValue, final Object newValue, final int pos) {
		// Refresh this node
	}
}
