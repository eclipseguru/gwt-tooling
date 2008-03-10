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
package org.eclipseguru.gwt.ui.java;

import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class GwtJavaUtil {

	/**
	 * Returns all package fragement roots excluding JRE contributed roots.
	 * 
	 * @param project
	 * @return
	 */
	public static IPackageFragmentRoot[] getNonJRERoots(final IJavaProject project) {
		final List<IPackageFragmentRoot> result = new ArrayList<IPackageFragmentRoot>();
		try {
			final IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++)
				if (!isJRELibrary(roots[i])) {
					result.add(roots[i]);
				}
		} catch (final JavaModelException e) {
		}
		return result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	/**
	 * Returns the default dearch scope
	 * 
	 * @param project
	 * @return
	 */
	public static IJavaSearchScope getSearchScope(final IProject project) {
		return SearchEngine.createJavaSearchScope(getNonJRERoots(JavaCore.create(project)));
	}

	/**
	 * Indicates if this belongs to a JRE library.
	 * 
	 * @param root
	 * @return
	 */
	public static boolean isJRELibrary(final IPackageFragmentRoot root) {
		try {
			final IPath path = root.getRawClasspathEntry().getPath();
			if (path.equals(new Path(JavaRuntime.JRE_CONTAINER)) || path.equals(new Path(JavaRuntime.JRELIB_VARIABLE)))
				return true;
		} catch (final JavaModelException e) {
		}
		return false;
	}

	/**
	 * Selects a type using the specified resource context
	 * 
	 * @param resource
	 * @param scope
	 * @return
	 */
	public static String selectType(final IResource resource, final int scope) {
		if (resource == null)
			return null;
		final IProject project = resource.getProject();
		try {
			final SelectionDialog dialog = JavaUI.createTypeDialog(GwtUi.getActiveWorkbenchShell(), PlatformUI.getWorkbench().getProgressService(), getSearchScope(project), scope, false, ""); //$NON-NLS-1$
			dialog.setTitle("Select Type");
			if (dialog.open() == Window.OK) {
				final IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (final JavaModelException e) {
		}
		return null;
	}

	/**
	 * Hidden.
	 */
	private GwtJavaUtil() {
		// empty
	}

}
