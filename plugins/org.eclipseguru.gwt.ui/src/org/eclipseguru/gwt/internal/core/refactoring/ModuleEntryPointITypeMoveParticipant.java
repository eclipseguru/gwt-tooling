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

import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

/**
 * Move participant for the entry point.
 */
public class ModuleEntryPointITypeMoveParticipant extends MoveParticipant {

	/** the type */
	private IType type;

	/** the destination */
	private IJavaElement destination;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#checkConditions(org.eclipse.core.runtime.IProgressMonitor,
	 *      org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	@Override
	public RefactoringStatus checkConditions(final IProgressMonitor pm, final CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if ((null == type) || (null == destination))
			return null;

		return GwtRefactoringUtil.createChangesForTypeMove(type, destination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#getName()
	 */
	@Override
	public String getName() {
		return "GWT module entry point participant";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant#initialize(java.lang.Object)
	 */
	@Override
	protected boolean initialize(final Object element) {
		type = (IType) element;
		try {
			if (GwtModule.isEntryPoint(type)) {
				try {
					// check that the type is not a local, and is not declared
					// in a local type
					IType declaringType = type;
					while (declaringType != null) {
						if (declaringType.isLocal()) {
							type = null;
							return false;
						}
						declaringType = declaringType.getDeclaringType();
					}
				} catch (final JavaModelException e) {
					JDIDebugUIPlugin.log(e);
				}
				final Object destination = getArguments().getDestination();
				if ((destination instanceof IPackageFragment) || (destination instanceof IType)) {
					this.destination = (IJavaElement) destination;
					return true;
				}
			}
		} catch (final JavaModelException e) {
			GwtUi.logError("Error while testing for entry point", e);
		}

		type = null;
		return false;
	}

}
