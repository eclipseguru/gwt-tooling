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
package org.eclipseguru.gwt.ui.editor;

import org.eclipseguru.gwt.ui.editor.utils.HyperlinkDetectorUtils;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link IHyperlinkDetector} for GWT module source files.
 */
public class ModuleSourceHyperlinkDetector implements IHyperlinkDetector {
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region, final boolean canShowMultipleHyperlinks) {
		// for now, only capable of creating 1 hyperlink
		final List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>(0);

		if ((region != null) && (textViewer != null)) {
			final IDocument document = textViewer.getDocument();
			// get current node from given region & document
			final Node currentNode = HyperlinkDetectorUtils.getCurrentNode(document, region.getOffset());
			// check for "class" attribute
			if (currentNode instanceof IDOMElement) {
				final IDOMElement elemNode = (IDOMElement) currentNode;
				final Attr attributeNode = elemNode.getAttributeNode("class");
				if (attributeNode instanceof IDOMAttr)
					HyperlinkDetectorUtils.detectHyperlinkInAttributeValue((IDOMAttr) attributeNode, document, hyperlinks);

			} else if (currentNode instanceof IDOMAttr) {

				final IDOMAttr attributeNode = (IDOMAttr) currentNode;
				if ("class".equals(attributeNode.getName()))
					HyperlinkDetectorUtils.detectHyperlinkInAttributeValue(attributeNode, document, hyperlinks);

			}
		}
		if (hyperlinks.size() == 0)
			return null;
		return hyperlinks.toArray(new IHyperlink[0]);
	}
}
