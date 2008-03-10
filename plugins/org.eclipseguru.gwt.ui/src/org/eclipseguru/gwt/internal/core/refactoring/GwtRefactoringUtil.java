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
package org.eclipseguru.gwt.internal.core.refactoring;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.debug.core.refactoring.JDTDebugRefactoringUtil;
import org.eclipse.ltk.core.refactoring.Change;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating refactoring changes.
 */
public class GwtRefactoringUtil {

	/**
	 * Returns a change for the given launch configuration if the launch
	 * configuration needs to be updated for this IType change. It specifically
	 * looks to see if the main type of the launch configuration is an inner
	 * type of the given IType.
	 * 
	 * @param gwtModule
	 *            the launch configuration
	 * @param type
	 *            the type to check for
	 * @param newfqname
	 *            the new fully qualified name
	 * @return the <code>Change</code> for this outer type
	 * @throws CoreException
	 */
	protected static Change createChangesForOuterTypeChange(final GwtModule gwtModule, final IType type, final String newfqname) throws CoreException {
		final IType[] innerTypes = type.getTypes();
		final String mtname = gwtModule.getEntryPointTypeName();
		for (final IType element : innerTypes) {
			final String newTypeName = newfqname + '$' + element.getElementName();
			// if it matches, check the type
			if (element.getFullyQualifiedName().equals(mtname))
				return new GwtModuleEntryPointChange(gwtModule, newTypeName);
			// if it's not the type, check the inner types
			return createChangesForOuterTypeChange(gwtModule, element, newTypeName);
		}
		return null;
	}

	/**
	 * Creates a <code>Change</code> for a type change
	 * 
	 * @param type
	 *            the type that is changing
	 * @param newfqname
	 *            the new fully qualified name
	 * @return the <code>Change</code> for changing the specified type
	 * @throws CoreException
	 */
	protected static Change createChangesForTypeChange(final IType type, final String newfqname) throws CoreException {
		final List<Change> changes = new ArrayList<Change>();
		final String typename = type.getFullyQualifiedName();
		final GwtProject gwtProject = GwtCore.create(type.getJavaProject().getProject());
		if ((gwtProject != null) && gwtProject.exists()) {
			final GwtModule[] modules = gwtProject.getModules();
			String mtname;
			for (final GwtModule gwtModule : modules) {
				mtname = gwtModule.getEntryPointTypeName();
				if (typename.equals(mtname)) {
					changes.add(new GwtModuleEntryPointChange(gwtModule, newfqname));
				} else {
					final Change change = createChangesForOuterTypeChange(gwtModule, type, newfqname);
					if (change != null) {
						changes.add(change);
					}
				}
			}
		}
		return JDTDebugRefactoringUtil.createChangeFromList(changes, "GWT module updates");
	}

	/**
	 * Provides a public mechanism for creating the <code>Change</code> for
	 * moving a type
	 * 
	 * @param type
	 *            the type being moved
	 * @param destination
	 *            the destination to move the type to
	 * @return the <code>Change</code> for the type move
	 * @throws CoreException
	 */
	public static Change createChangesForTypeMove(final IType type, final IJavaElement destination) throws CoreException {
		String newfqname = type.getElementName();
		if (destination instanceof IType) {
			newfqname = ((IType) destination).getFullyQualifiedName() + '$' + type.getElementName();
		} else if (destination instanceof IPackageFragment)
			if (!((IPackageFragment) destination).isDefaultPackage()) {
				newfqname = destination.getElementName() + '.' + type.getElementName();
			}
		return createChangesForTypeChange(type, newfqname);
	}

	/**
	 * Provides a public mechanism for creating the <code>Change</code> for
	 * renaming a type
	 * 
	 * @param type
	 *            the type to rename
	 * @param newname
	 *            the new name for the type
	 * @return the <code>Change</code> for the type rename
	 * @throws CoreException
	 */
	public static Change createChangesForTypeRename(final IType type, final String newname) throws CoreException {
		final IType dtype = type.getDeclaringType();
		String newfqname = newname;
		if (dtype == null) {
			final IPackageFragment packageFragment = type.getPackageFragment();
			if (!packageFragment.isDefaultPackage()) {
				newfqname = packageFragment.getElementName() + '.' + newname;
			}
		} else {
			newfqname = dtype.getFullyQualifiedName() + '$' + newname;
		}
		return createChangesForTypeChange(type, newfqname);
	}

	/**
	 * No need to instanciate
	 */
	private GwtRefactoringUtil() {
		// empty
	}

}
