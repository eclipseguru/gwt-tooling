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
package org.eclipseguru.gwt.ui.editor.contentassist;

import org.eclipseguru.gwt.ui.GwtUiImages;
import org.eclipseguru.gwt.ui.java.GwtJavaUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The content assist processor for the GWT module descriptor.
 */
public class ModuleSourceContentAssistProcessor extends XMLContentAssistProcessor {

	protected SearchEngine searchEngine = new SearchEngine();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor#addAttributeValueProposals(org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest)
	 */
	@Override
	protected void addAttributeValueProposals(final ContentAssistRequest contentAssistRequest) {
		final IDOMNode node = (IDOMNode) contentAssistRequest.getNode();

		// Find the attribute region and name for which this position should
		// have a value proposed
		final IStructuredDocumentRegion open = node.getFirstStructuredDocumentRegion();
		final ITextRegionList openRegions = open.getRegions();
		int i = openRegions.indexOf(contentAssistRequest.getRegion());
		if (i < 0)
			return;
		ITextRegion nameRegion = null;
		while (i >= 0) {
			nameRegion = openRegions.get(i--);
			if (nameRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
				break;
		}

		// get prefix
		String prefix = contentAssistRequest.getMatchString();
		if (prefix == null)
			prefix = "";
		else if ((prefix.length() > 0) && (prefix.startsWith("\"") || prefix.startsWith("'")))
			prefix = prefix.substring(1);

		// the name region is REQUIRED to do anything useful
		if (nameRegion != null) {
			final String attributeName = open.getText(nameRegion);
			generateAttributeValueProposals(contentAssistRequest, node, attributeName, prefix);
			super.addAttributeValueProposals(contentAssistRequest);
		}
	}

	/**
	 * Computes the GWT specific attribute value proposals.
	 * 
	 * @param contentAssistRequest
	 * @param node
	 * @param attributeName
	 * @param prefix
	 */
	private void generateAttributeValueProposals(final ContentAssistRequest contentAssistRequest, final IDOMNode node, final String attributeName, final String prefix) {
		final IResource resource = ContentAssistUtils.getResource(contentAssistRequest);
		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

		// we need to adjust the replacement position
		// (because we do not replace quotes)
		final int startOffset = contentAssistRequest.getReplacementBeginPosition() + 1;
		final int length = contentAssistRequest.getReplacementLength() > 1 ? contentAssistRequest.getReplacementLength() - 2 : 0;

		if ("class".equals(attributeName))
			if (null != resource) {
				final String errorMessage = generateTypeAndPackageProposals(prefix, resource.getProject(), proposals, startOffset, length, IJavaSearchConstants.CLASS);
				if (null != errorMessage) {
					setErrorMessage(errorMessage);
					return;
				}
			}

		if ("inherits".equals(node.getNodeName()))
			if ("name".equals(attributeName))
				if (null != resource)
					generateModuleProposals(contentAssistRequest, prefix, resource.getProject());

		// add proposals
		for (final ICompletionProposal proposal : proposals)
			contentAssistRequest.addProposal(proposal);
	}

	/**
	 * Generates proposals for GWT modules
	 * 
	 * @param contentAssistRequest
	 * @param prefix
	 * @param project
	 */
	protected void generateModuleProposals(final ContentAssistRequest contentAssistRequest, final String prefix, final IProject project) {
		// TODO Auto-generated method stub

	}

	/**
	 * Generates proposals for Java types and packages
	 * 
	 * @param currentContent
	 * @param project
	 * @param typeScope
	 * @param contentAssistRequest
	 * @return an error message (maybe <code>null</code>)
	 */
	protected String generateTypeAndPackageProposals(String currentContent, final IProject project, final Collection<ICompletionProposal> proposals, final int replacementStart, final int replacementLength, final int typeScope) {
		currentContent = ContentAssistUtils.removeLeadingSpaces(currentContent);
		if (currentContent.length() == 0)
			return "The Java content assist needs a prefix of at least one character.";

		try {
			final ICompilationUnit unit = ContentAssistUtils.getWorkingCopy(project);
			if (unit == null)
				return generateTypeProposals(currentContent, project, proposals, replacementStart, replacementLength, typeScope);

			final IBuffer buff = unit.getBuffer();
			buff.setContents("class Dummy2 { " + currentContent); //$NON-NLS-1$

			final CompletionRequestor req = new CompletionRequestor() {

				@Override
				public void accept(CompletionProposal proposal) {
					if (proposal.getKind() == CompletionProposal.PACKAGE_REF) {
						String pkgName = new String(proposal.getCompletion());
						proposals.add(new TypeCompletionProposal(pkgName, GwtUiImages.get(GwtUiImages.IMG_PACKAGE), pkgName, replacementStart, replacementLength));
					} else {
						boolean isInterface = Flags.isInterface(proposal.getFlags());
						String completion = new String(proposal.getCompletion());
						if ((isInterface && (typeScope == IJavaSearchConstants.CLASS)) || completion.equals("Dummy2")) //$NON-NLS-1$
							// don't want Dummy class showing up as option.
							return;
						int period = completion.lastIndexOf('.');
						String cName = null, pName = null;
						if (period == -1)
							cName = completion;
						else {
							cName = completion.substring(period + 1);
							pName = completion.substring(0, period);
						}
						Image image = isInterface ? GwtUiImages.get(GwtUiImages.IMG_INTERFACE) : GwtUiImages.get(GwtUiImages.IMG_CLASS);
						proposals.add(new TypeCompletionProposal(completion, image, cName + " - " + pName, replacementStart, replacementLength)); //$NON-NLS-1$
					}
				}

			};

			// ignore everything but class and package references
			for (int i = 1; i <= 20; i++)
				if ((i != CompletionProposal.PACKAGE_REF) && (i != CompletionProposal.TYPE_REF))
					req.setIgnored(i, true);
			unit.codeComplete(15 + currentContent.length(), req);
			unit.discardWorkingCopy();
		} catch (final JavaModelException e) {
			return e.getMessage();
		}

		return null;
	}

	/**
	 * Generates Java Type proposals.
	 * 
	 * @param currentContent
	 * @param project
	 * @param proposals
	 * @param replacementStart
	 * @param replacementLength
	 * @param typeScope
	 */
	protected String generateTypeProposals(final String currentContent, final IProject project, final Collection<ICompletionProposal> proposals, final int replacementStart, final int replacementLength, final int typeScope) {
		// Dynamically adjust the search scope depending on the current
		// state of the project
		final IJavaSearchScope scope = GwtJavaUtil.getSearchScope(project);
		char[] packageName = null;
		char[] typeName = null;
		final int index = currentContent.lastIndexOf('.');

		if (index == -1)
			// There is no package qualification
			// Perform the search only on the type name
			typeName = currentContent.toCharArray();
		else if ((index + 1) == currentContent.length()) {
			// There is a package qualification and the last character is a
			// dot
			// Perform the search for all types under the given package
			// Pattern for all types
			typeName = "".toCharArray(); //$NON-NLS-1$
			// Package name without the trailing dot
			packageName = currentContent.substring(0, index).toCharArray();
		} else {
			// There is a package qualification, followed by a dot, and
			// a type fragment
			// Type name without the package qualification
			typeName = currentContent.substring(index + 1).toCharArray();
			// Package name without the trailing dot
			packageName = currentContent.substring(0, index).toCharArray();
		}

		try {
			final TypeNameRequestor req = new TypeNameRequestor() {
				@Override
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
					// Accept search results from the JDT SearchEngine
					String cName = new String(simpleTypeName);
					String pName = new String(packageName);
					String label = cName + " - " + pName; //$NON-NLS-1$
					String content = pName + "." + cName; //$NON-NLS-1$
					Image image = (Flags.isInterface(modifiers)) ? GwtUiImages.get(GwtUiImages.IMG_INTERFACE) : GwtUiImages.get(GwtUiImages.IMG_CLASS);
					TypeCompletionProposal proposal = new TypeCompletionProposal(content, image, label, replacementStart, replacementLength);
					proposals.add(proposal);
				}
			};
			// Note: Do not use the search() method, its performance is
			// bad compared to the searchAllTypeNames() method
			searchEngine.searchAllTypeNames(packageName, typeName, SearchPattern.R_PREFIX_MATCH, typeScope, scope, req, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
		} catch (final CoreException e) {
			return e.getMessage();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.xml.ui.internal.contentassist.AbstractContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '=', '\"', '<' };
	}
}
