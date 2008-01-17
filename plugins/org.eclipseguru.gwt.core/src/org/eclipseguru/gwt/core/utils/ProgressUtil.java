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
package org.eclipseguru.gwt.core.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * A simple util for working with <code>{@link IProgressMonitor}</code>s.
 */
public class ProgressUtil {

	/**
	 * Checks if the monitor was canceled.
	 * 
	 * @param monitor
	 */
	public static void checkCanceled(final IProgressMonitor monitor) throws OperationCanceledException {
		if ((null != monitor) && monitor.isCanceled())
			throw new OperationCanceledException();
	}

	public static IProgressMonitor monitor(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();

		return monitor;
	}

	/**
	 * @param monitor
	 * @param ticks
	 * @return
	 */
	public static IProgressMonitor subProgressMonitor(final IProgressMonitor monitor, final int ticks) {
		if (null == monitor)
			return new NullProgressMonitor();

		if (monitor instanceof NullProgressMonitor)
			return monitor;

		return new SubProgressMonitor(monitor, ticks);
	}

	private ProgressUtil() {
		// no need to instanciate
	}
}
