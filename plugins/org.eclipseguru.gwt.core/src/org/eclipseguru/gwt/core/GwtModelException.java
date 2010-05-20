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
package org.eclipseguru.gwt.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * An exception in the GWT plug-in.
 */
public class GwtModelException extends CoreException {

	/** serialVersionUID */
	private static final long serialVersionUID = -396288056574984459L;

	/**
	 * @param status
	 */
	public GwtModelException(final IStatus status) {
		super(status);
	}

}
