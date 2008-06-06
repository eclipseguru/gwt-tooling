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

import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

/**
 * A module source completion proposal.
 */
public class TypeCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension5 {

	protected String fReplacementString;
	protected Image fImage;
	protected String fDisplayString;
	protected int fBeginInsertPoint;
	protected int fLength;
	protected String fAdditionalInfo;
	private IInformationControlCreator fCreator;

	public TypeCompletionProposal(final String replacementString, final Image image, final String displayString) {
		this(replacementString, image, displayString, 0, 0);
	}

	public TypeCompletionProposal(final String replacementString, final Image image, final String displayString, final int startOffset, final int length) {
		Assert.isNotNull(replacementString);

		fReplacementString = replacementString;
		fImage = image;
		fDisplayString = displayString;
		fBeginInsertPoint = startOffset;
		fLength = length;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse
	 * .jface.text.IDocument)
	 */
	public void apply(final IDocument document) {
		if (fLength == -1) {
			final String current = document.get();
			fLength = current.length();
		}
		try {
			document.replace(fBeginInsertPoint, fLength, fReplacementString);
		} catch (final BadLocationException e) {
			// DEBUG
			// e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		// No additional proposal information
		return null;
	}

	public Object getAdditionalProposalInfo(final IProgressMonitor monitor) {
		return fAdditionalInfo;
	}

	/*
	 * (non-Javadoc)
	 * @seeorg.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		// No context information
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString
	 * ()
	 */
	public String getDisplayString() {
		return fDisplayString;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	public IInformationControlCreator getInformationControlCreator() {
		final Shell shell = GwtUi.getActiveWorkbenchShell();
		if ((shell == null) || !BrowserInformationControl.isAvailable(shell)) {
			return null;
		}

		if (fCreator == null) {
			//			fCreator = new AbstractReusableInformationControlCreator() {
			//
			//				/*
			//				 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
			//				 */
			//				@Override
			//				public IInformationControl doCreateInformationControl(final Shell parent) {
			//					return new BrowserInformationControl(parent, SWT.NO_TRIM | SWT.TOOL, SWT.NONE, null);
			//				}
			//			};
			// copied from org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal
			// see also: https://bugs.eclipse.org/bugs/show_bug.cgi?id=232024
			final JavadocHover.PresenterControlCreator presenterControlCreator = new JavadocHover.PresenterControlCreator();
			fCreator = new JavadocHover.HoverControlCreator(presenterControlCreator, true);
		}
		return fCreator;
	}

	public int getPrefixCompletionStart(final IDocument document, final int completionOffset) {
		return fBeginInsertPoint;
	}

	public CharSequence getPrefixCompletionText(final IDocument document, final int completionOffset) {
		return fReplacementString;
	}

	/**
	 * @return
	 */
	public String getReplacementString() {
		return fReplacementString;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection
	 * (org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(final IDocument document) {
		if (fReplacementString.equals("\"\"")) {
			return new Point(fBeginInsertPoint + 1, 0);
		}
		return new Point(fBeginInsertPoint + fReplacementString.length(), 0);
	}

	public void setAdditionalProposalInfo(final String info) {
		fAdditionalInfo = info;
	}

}
