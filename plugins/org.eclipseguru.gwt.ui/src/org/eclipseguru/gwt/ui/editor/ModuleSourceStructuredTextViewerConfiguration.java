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

import org.eclipseguru.gwt.ui.editor.contentassist.ModuleSourceContentAssistProcessor;

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link StructuredTextViewerConfiguration} for the module source content
 * type.
 */
public class ModuleSourceStructuredTextViewerConfiguration extends StructuredTextViewerConfigurationXML {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML#getContentAssistProcessors(org.eclipse.jface.text.source.ISourceViewer,
	 *      java.lang.String)
	 */
	@Override
	protected IContentAssistProcessor[] getContentAssistProcessors(final ISourceViewer sourceViewer, final String partitionType) {
		if ((partitionType == IStructuredPartitions.DEFAULT_PARTITION) || (partitionType == IXMLPartitions.XML_DEFAULT))
			return new IContentAssistProcessor[] { new ModuleSourceContentAssistProcessor() };

		return super.getContentAssistProcessors(sourceViewer, partitionType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML#getHyperlinkDetectors(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(final ISourceViewer sourceViewer) {
		if ((sourceViewer == null) || !fPreferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_HYPERLINKS_ENABLED))
			return null;

		final List<IHyperlinkDetector> allDetectors = new ArrayList<IHyperlinkDetector>(5);
		allDetectors.add(new ModuleSourceHyperlinkDetector()); // add own
		// hyperlink
		// detector

		final IHyperlinkDetector[] superDetectors = super.getHyperlinkDetectors(sourceViewer);
		for (final IHyperlinkDetector detector : superDetectors)
			if (!allDetectors.contains(detector))
				allDetectors.add(detector);
		return allDetectors.toArray(new IHyperlinkDetector[0]);
	}
}
