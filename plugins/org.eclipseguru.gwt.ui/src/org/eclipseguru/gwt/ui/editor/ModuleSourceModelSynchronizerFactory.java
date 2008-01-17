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

import org.eclipse.wst.sse.core.internal.provisional.AbstractAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

/**
 * A factory for creating our refreshing adapter. This factory is registered
 * with the model so that when an adapter with the matching key
 * (RefreshOnNotifyChangedAdapter.class) is requested, it can automatically
 * return one. For simplicity, a singleton instance of the adapter is used.
 */
class ModuleSourceModelSynchronizerFactory extends AbstractAdapterFactory {
	/**
	 * Our single instance
	 */
	ModuleSourceModelSynchronizer refreshAdapter = null;

	public ModuleSourceModelSynchronizerFactory(final ModuleSourceWC moduleSource) {
		super(ModuleSourceModelSynchronizer.class, true);
		refreshAdapter = new ModuleSourceModelSynchronizer(moduleSource);
	}

	@Override
	protected INodeAdapter createAdapter(final INodeNotifier target) {
		// Return a singleton
		return refreshAdapter;
	}

}
