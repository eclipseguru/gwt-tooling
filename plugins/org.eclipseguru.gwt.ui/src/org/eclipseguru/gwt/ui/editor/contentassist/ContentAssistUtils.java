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
package org.eclipseguru.gwt.ui.editor.contentassist;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;

/**
 * Utility class for content assists.
 */
class ContentAssistUtils {

	/**
	 * Returns the underlying resource of the content assist request
	 * 
	 * @param contentAssistRequest
	 * @return
	 */
	public static IResource getResource(final ContentAssistRequest contentAssistRequest) {
		IResource resource = null;
		String baselocation = null;

		if (contentAssistRequest != null) {
			final IStructuredDocumentRegion region = contentAssistRequest.getDocumentRegion();
			if (region != null) {
				final IDocument document = region.getParentDocument();
				IStructuredModel model = null;
				try {
					model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
					if (model != null) {
						baselocation = model.getBaseLocation();
					}
				} finally {
					if (model != null) {
						model.releaseFromRead();
					}
				}
			}
		}

		if (baselocation != null) {
			final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			final IPath filePath = new Path(baselocation);
			if (filePath.segmentCount() > 0) {
				resource = root.getFile(filePath);
			}
		}
		return resource;
	}

	static final ICompilationUnit getWorkingCopy(final IProject project) throws JavaModelException {
		final IPackageFragmentRoot[] roots = JavaCore.create(project).getPackageFragmentRoots();
		if (roots.length > 0) {
			IPackageFragment frag = null;
			for (int i = 0; i < roots.length; i++)
				if ((roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) || project.equals(roots[i].getCorrespondingResource()) || (roots[i].isArchive() && !roots[i].isExternal())) {
					final IJavaElement[] elems = roots[i].getChildren();
					if ((elems.length > 0) && (elems[i] instanceof IPackageFragment)) {
						frag = (IPackageFragment) elems[i];
						break;
					}
				}
			if (frag != null)
				return frag.getCompilationUnit("Dummy2.java").getWorkingCopy(new NullProgressMonitor()); //$NON-NLS-1$
		}
		return null;
	}

	public static final String removeLeadingSpaces(final String value) {
		final char[] valueArray = value.toCharArray();
		int i = 0;
		for (; i < valueArray.length; i++)
			if (!Character.isWhitespace(valueArray[i])) {
				break;
			}
		return (i == valueArray.length) ? "" : new String(valueArray, i, valueArray.length - i); //$NON-NLS-1$
	}

	/**
	 * Hidden.
	 */
	private ContentAssistUtils() {
		// empty
	}
}
