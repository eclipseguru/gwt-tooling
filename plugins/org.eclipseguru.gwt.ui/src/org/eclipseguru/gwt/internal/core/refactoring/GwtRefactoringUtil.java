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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.debug.core.refactoring.JDTDebugRefactoringUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;

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
	protected static Change createChangesForOuterTypeChange(GwtModule gwtModule, IType type, String newfqname) throws CoreException {
		IType[] innerTypes = type.getTypes();
		String mtname = gwtModule.getEntryPointTypeName();
		for (IType element : innerTypes) {
			String newTypeName = newfqname + '$' + element.getElementName();
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
	protected static Change createChangesForTypeChange(IType type, String newfqname) throws CoreException {
		List<Change> changes = new ArrayList<Change>();
		String typename = type.getFullyQualifiedName();
		GwtProject gwtProject = GwtCore.create(type.getJavaProject().getProject());
		if ((gwtProject != null) && gwtProject.exists()) {
			GwtModule[] modules = gwtProject.getModules();
			String mtname;
			for (GwtModule gwtModule : modules) {
				mtname = gwtModule.getEntryPointTypeName();
				if (typename.equals(mtname))
					changes.add(new GwtModuleEntryPointChange(gwtModule, newfqname));
				else {
					Change change = createChangesForOuterTypeChange(gwtModule, type, newfqname);
					if (change != null)
						changes.add(change);
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
	public static Change createChangesForTypeMove(IType type, IJavaElement destination) throws CoreException {
		String newfqname = type.getElementName();
		if (destination instanceof IType)
			newfqname = ((IType) destination).getFullyQualifiedName() + '$' + type.getElementName();
		else if (destination instanceof IPackageFragment)
			if (!((IPackageFragment) destination).isDefaultPackage())
				newfqname = destination.getElementName() + '.' + type.getElementName();
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
	public static Change createChangesForTypeRename(IType type, String newname) throws CoreException {
		IType dtype = type.getDeclaringType();
		String newfqname = newname;
		if (dtype == null) {
			IPackageFragment packageFragment = type.getPackageFragment();
			if (!packageFragment.isDefaultPackage())
				newfqname = packageFragment.getElementName() + '.' + newname;
		} else
			newfqname = dtype.getFullyQualifiedName() + '$' + newname;
		return createChangesForTypeChange(type, newfqname);
	}

	/**
	 * No need to instanciate
	 */
	private GwtRefactoringUtil() {
		// empty
	}

}
