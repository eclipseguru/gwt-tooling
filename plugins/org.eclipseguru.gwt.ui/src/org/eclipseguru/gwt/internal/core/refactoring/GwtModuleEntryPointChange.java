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
package org.eclipseguru.gwt.internal.core.refactoring;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModelException;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.ui.GwtUi;

/**
 * The change for the GWT module entry point type name.
 */
public class GwtModuleEntryPointChange extends Change {

	private final GwtModule gwtModule;
	private final String newEntryPointTypeName;
	private final String oldEntryPointTypeName;

	/**
	 * Creates a new change.
	 * 
	 * @param gwtModule
	 *            the GWT module to modify
	 * @param newEntryPointTypeName
	 *            the name of the new main type, or <code>null</code> if not
	 *            modified.
	 */
	public GwtModuleEntryPointChange(GwtModule gwtModule, String newEntryPointTypeName) {
		this.gwtModule = gwtModule;
		this.newEntryPointTypeName = newEntryPointTypeName;
		String currentTypeName;
		try {
			currentTypeName = gwtModule.getEntryPointTypeName();
		} catch (GwtModelException e) {
			currentTypeName = null;
		}
		this.oldEntryPointTypeName = currentTypeName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
	 */
	@Override
	public Object getModifiedElement() {
		return gwtModule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Change#getName()
	 */
	@Override
	public String getName() {
		return MessageFormat.format("Update entry point type name in module \"{0}\"", gwtModule.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initializeValidationData(IProgressMonitor pm) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (!gwtModule.isBinary()) {
			IFile moduleDescriptor = (IFile) gwtModule.getModuleDescriptor();
			if (moduleDescriptor.exists()) {
				GwtModule module = GwtCore.create(moduleDescriptor);
				if ((null != oldEntryPointTypeName) && oldEntryPointTypeName.equals(module.getEntryPointTypeName()))
					return new RefactoringStatus();
				else
					return RefactoringStatus.createFatalErrorStatus(MessageFormat.format("The entry point name in GWT module \"{0}\" changed since the beginning of the refactoring operation.", gwtModule.getName()));
			}
			return RefactoringStatus.createFatalErrorStatus(MessageFormat.format("The GWT module \"{0}\" no longer exists.", gwtModule.getName()));
		}
		return RefactoringStatus.createFatalErrorStatus(MessageFormat.format("The GWT module \"{0}\" is a binary module.", gwtModule.getName()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		IFile moduleDescriptor = (IFile) gwtModule.getModuleDescriptor();
		try {
			char[] content = Util.getResourceContentsAsCharArray(moduleDescriptor);
			char[] newContent = CharOperation.replace(content, oldEntryPointTypeName.toCharArray(), newEntryPointTypeName.toCharArray());
			if (CharOperation.equals(content, newContent))
				return null;

			moduleDescriptor.setContents(new ByteArrayInputStream(new String(newContent).getBytes(moduleDescriptor.getCharset())), IResource.KEEP_HISTORY, pm);
		} catch (UnsupportedEncodingException e) {
			throw new CoreException(GwtUi.newErrorStatus(e));
		}

		// return the undo change
		return new GwtModuleEntryPointChange(GwtCore.create(moduleDescriptor), oldEntryPointTypeName);
	}

}
