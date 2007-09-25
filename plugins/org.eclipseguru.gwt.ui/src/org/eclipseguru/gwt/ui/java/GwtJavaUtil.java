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
package org.eclipseguru.gwt.ui.java;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipseguru.gwt.ui.GwtUi;

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
	public static IPackageFragmentRoot[] getNonJRERoots(IJavaProject project) {
		List<IPackageFragmentRoot> result = new ArrayList<IPackageFragmentRoot>();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++)
				if (!isJRELibrary(roots[i]))
					result.add(roots[i]);
		} catch (JavaModelException e) {
		}
		return result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	/**
	 * Returns the default dearch scope
	 * 
	 * @param project
	 * @return
	 */
	public static IJavaSearchScope getSearchScope(IProject project) {
		return SearchEngine.createJavaSearchScope(getNonJRERoots(JavaCore.create(project)));
	}

	/**
	 * Indicates if this belongs to a JRE library.
	 * 
	 * @param root
	 * @return
	 */
	public static boolean isJRELibrary(IPackageFragmentRoot root) {
		try {
			IPath path = root.getRawClasspathEntry().getPath();
			if (path.equals(new Path(JavaRuntime.JRE_CONTAINER)) || path.equals(new Path(JavaRuntime.JRELIB_VARIABLE)))
				return true;
		} catch (JavaModelException e) {
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
	public static String selectType(IResource resource, int scope) {
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(GwtUi.getActiveWorkbenchShell(), PlatformUI.getWorkbench().getProgressService(), getSearchScope(project), scope, false, ""); //$NON-NLS-1$
			dialog.setTitle("Select Type");
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
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
