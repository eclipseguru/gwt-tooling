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
package org.eclipseguru.gwt.ui.editor.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import java.util.List;

public class HyperlinkDetectorUtils {
	/**
	 * Detecteds if the value of the specified attribute links to an
	 * {@link IJavaElement}.
	 * 
	 * @param attributeNode
	 * @param document
	 * @param hyperlinks
	 */
	public static void detectHyperlinkInAttributeValue(final IDOMAttr attributeNode, final IDocument document, final List<IHyperlink> hyperlinks) {
		final IJavaElement javaElement = getJavaElement(document, attributeNode.getValue());
		if (javaElement != null) {
			int start = attributeNode.getValueRegionStartOffset();
			int length = attributeNode.getValueRegionText().length();

			// remove quotes from link region
			final String text = attributeNode.getValueRegionText();
			if (text.startsWith("\"") || text.startsWith("'")) {
				start += 1;
				length -= 2;
			}
			final IHyperlink link = new JavaHyperlink(new Region(start, length), javaElement);
			hyperlinks.add(link);
		}
	}

	/**
	 * Returns the node the cursor is currently on in the document.
	 * <code>null</code> if no node is selected
	 * 
	 * @param offset
	 * @return IDOMNode
	 */
	public static IDOMNode getCurrentNode(final IDocument document, final int offset) {
		// get the current node at the offset (returns either: element,
		// doctype, text)
		IndexedRegion inode = null;
		IStructuredModel sModel = null;
		try {
			sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
			if (sModel != null) {
				inode = sModel.getIndexedRegion(offset);
				if (inode == null) {
					inode = sModel.getIndexedRegion(offset - 1);
				}
			}
		} finally {
			if (sModel != null) {
				sModel.releaseFromRead();
			}
		}

		if (inode instanceof IDOMNode)
			return (IDOMNode) inode;
		return null;
	}

	/**
	 * Determine java element from given document and fully qualified java name.
	 * Returns IJavaElement or null if could not determine.
	 * 
	 * @param document
	 * @param name
	 * @return IJavaElement or null
	 */
	public static IJavaElement getJavaElement(final IDocument document, final String name) {
		if ((null == name) || (name.trim().length() == 0))
			return null;

		IJavaElement element = null;
		final IJavaProject project = getJavaProject(document);
		if (project != null) {
			try {
				element = project.findType(name);
			} catch (final JavaModelException e) {
				e.printStackTrace();
			}
		}
		return element;
	}

	/**
	 * Returns java project for the given document. null if cannot determine
	 * java project.
	 * 
	 * @param document
	 * @return IJavaProject
	 */
	private static IJavaProject getJavaProject(final IDocument document) {
		IJavaProject project = null;
		String baselocation = null;

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

		if (baselocation != null) {
			final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			final IPath filePath = new Path(baselocation);
			if (filePath.segmentCount() > 0) {
				final IProject proj = root.getProject(filePath.segment(0));
				if (proj != null) {
					project = JavaCore.create(proj);
				}
			}
		}
		return project;
	}
}
