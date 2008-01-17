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

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.ui.GwtUi;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.model.FactoryRegistry;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

/**
 * The module source editor.
 */
public class ModuleSourceEditor extends FormEditor {

	/**
	 * The current model being edited. The reference is used only to ensure that
	 * the listeners and factories are managed properly. Care must be taken not
	 * to leak models.
	 */
	private IStructuredModel structuredModel = null;

	/** IDX_SOURCE_PAGE */
	private int IDX_SOURCE_PAGE = -1;

	/** the source editor */
	private StructuredTextEditor sourceEditor = null;

	/** the editing model */
	private ModuleSourceWC moduleSource = null;

	/** overviewPage */
	private ModuleSourceOverviewPage overviewPage;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	@Override
	protected void addPages() {
		try {
			overviewPage = new ModuleSourceOverviewPage(this);
			// IDX_OVERVIEW_PAGE = addPage(overviewPage);

			sourceEditor = new StructuredTextEditor();
			IDX_SOURCE_PAGE = addPage(sourceEditor, getEditorInput());
			setPageText(IDX_SOURCE_PAGE, "Source");

			createModelFromInput(getEditorInput());

			setActivePage(IDX_SOURCE_PAGE);
		} catch (final PartInitException e) {
			GwtUi.logError("Problem creating sourc editor", e);
		}
	}

	/**
	 * Creates the model from the specified input.
	 * <p>
	 * Make sure to {@link #releaseModel() release} the old model before!
	 * </p>
	 * 
	 * @param newInput
	 */
	private void createModelFromInput(final IEditorInput newInput) {
		// reset model
		if ((null != structuredModel) || (null != moduleSource))
			releaseModel();

		if ((newInput != null) && (sourceEditor != null)) {
			final IDocument textDocument = sourceEditor.getDocumentProvider().getDocument(newInput);
			structuredModel = StructuredModelManager.getModelManager().getModelForRead((IStructuredDocument) textDocument);

			if (structuredModel != null) {

				// initialize model
				moduleSource = new ModuleSourceWC();

				/* Register the synchonizer factory */
				final FactoryRegistry factoryRegistry = structuredModel.getFactoryRegistry();
				if (factoryRegistry.getFactoryFor(ModuleSourceModelSynchronizer.class) == null)
					factoryRegistry.addFactory(new ModuleSourceModelSynchronizerFactory(moduleSource));
			}

			// set input to pages
			overviewPage.setModel(moduleSource);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createSite(org.eclipse.ui.IEditorPart)
	 */
	@Override
	protected IEditorSite createSite(final IEditorPart editor) {
		IEditorSite site = null;
		if (editor == sourceEditor)
			site = new MultiPageEditorSite(this, sourceEditor) {
				/**
				 * Set this id so nested editor is configured as an GWT Module
				 * source page.
				 * 
				 * @see org.eclipse.ui.part.MultiPageEditorSite#getId()
				 */
				@Override
				public String getId() {
					return GwtCore.MODULE_SOURCE_CONTENT_TYPE_ID;
				}
			};
		else
			site = super.createSite(editor);
		return site;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(final IProgressMonitor monitor) {
		sourceEditor.doSave(monitor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		sourceEditor.doSaveAs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.MultiPageEditorPart#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		final Object result = super.getAdapter(adapter);
		if (null != result)
			return result;

		if (((Class<?>) adapter).isAssignableFrom(ITextEditor.class))
			return sourceEditor;

		return result;
	}

	/**
	 * Initializes the editor's title based on the given editor input.
	 * 
	 * @param input
	 *            the editor input to be used
	 */
	private void initializeTitle(final IEditorInput input) {

		String title = ""; //$NON-NLS-1$
		String tooltip = ""; //$NON-NLS-1$

		if (input != null) {
			title = input.getName();
			tooltip = input.getToolTipText();
		}

		setPartName(title);
		setTitleToolTip(tooltip);

		firePropertyChange(PROP_DIRTY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return sourceEditor.isSaveAsAllowed();
	}

	/**
	 * Releases the current model.
	 */
	private void releaseModel() {
		if (structuredModel != null) {
			structuredModel.releaseFromRead();
			structuredModel = null;
		}

		if (null != moduleSource)
			moduleSource = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(final IEditorInput input) {
		super.setInput(input);
		if (sourceEditor != null) {
			sourceEditor.setInput(input);
			createModelFromInput(input);
		}

		initializeTitle(input);
	}
}
