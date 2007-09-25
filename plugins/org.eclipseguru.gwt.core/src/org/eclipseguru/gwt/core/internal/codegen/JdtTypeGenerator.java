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
package org.eclipseguru.gwt.core.internal.codegen;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.internal.jdtext.ImportsManager;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

/**
 * Abstract base class for a JDT code generator.
 */
@SuppressWarnings("restriction") 
public abstract class JdtTypeGenerator {
	/**
	 * Constant to signal that the created type is a class.
	 */
	public static final int CLASS_TYPE = 1;

	/**
	 * Constant to signal that the created type is a interface.
	 */
	public static final int INTERFACE_TYPE = 2;

	/**
	 * Constant to signal that the created type is an enum.
	 */
	public static final int ENUM_TYPE = 3;

	/**
	 * Constant to signal that the created type is an annotation.
	 */
	public static final int ANNOTATION_TYPE = 4;

	/** DUMMY_CLASS_NAME */
	public static final String DUMMY_CLASS_NAME = "$$__$$"; //$NON-NLS-1$

	/**
	 * a handle to the type to be created (does usually not exist, can be null)
	 */
	private IType currentType;

	private final String typeName;
	private final int typeKind;
	private final int modifiers;

	private StubTypeContext superClassStubTypeContext;
	private StubTypeContext superInterfaceStubTypeContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param typeName
	 *            the type name
	 * @param typeKind
	 *            the kind of this type
	 * @param modifiers
	 *            see {@link Flags}
	 */
	public JdtTypeGenerator(String typeName, int typeKind, int modifiers) {
		this.typeName = typeName;
		this.typeKind = typeKind;
		this.modifiers = modifiers;
	}

	/**
	 * Uses the New Java file template from the code template page to generate a
	 * compilation unit with the given type content.
	 * 
	 * @param cu
	 *            The new created compilation unit
	 * @param typeContent
	 *            The content of the type, including signature and type body.
	 * @param lineDelimiter
	 *            The line delimiter to be used.
	 * @return String Returns the result of evaluating the new file template
	 *         with the given type content.
	 * @throws CoreException
	 */
	protected String constructCUContent(ICompilationUnit cu, String typeContent, String lineDelimiter) throws CoreException {
		String fileComment = getFileComment(cu, lineDelimiter);
		String typeComment = getTypeComment(cu, lineDelimiter);
		IPackageFragment pack = (IPackageFragment) cu.getParent();
		String content = CodeGeneration.getCompilationUnitContent(cu, fileComment, typeComment, typeContent, lineDelimiter);
		if (content != null) {
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setProject(cu.getJavaProject());
			parser.setSource(content.toCharArray());
			CompilationUnit unit = (CompilationUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || (unit.getPackage() != null)) && !unit.types().isEmpty())
				return content;
		}
		StringBuffer buf = new StringBuffer();
		if (!pack.isDefaultPackage())
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		buf.append(lineDelimiter).append(lineDelimiter);
		if (typeComment != null)
			buf.append(typeComment).append(lineDelimiter);
		buf.append(typeContent);
		return buf.toString();
	}

	/**
	 * Constructs a really simple type stub.
	 * 
	 * @return the type stub
	 */
	private String constructSimpleTypeStub() {
		StringBuffer buf = new StringBuffer("public class "); //$NON-NLS-1$
		buf.append(getTypeName());
		buf.append("{ }"); //$NON-NLS-1$
		return buf.toString();
	}

	/*
	 * Called from createType to construct the source for this type
	 */
	private String constructTypeStub(ICompilationUnit parentCU, ImportsManager imports, String lineDelimiter) throws CoreException {
		StringBuffer buf = new StringBuffer();

		buf.append(Flags.toString(modifiers));
		if (modifiers != 0)
			buf.append(' ');
		String type = ""; //$NON-NLS-1$
		String templateID = ""; //$NON-NLS-1$
		switch (typeKind) {
		case CLASS_TYPE:
			type = "class "; //$NON-NLS-1$
			templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
			break;
		case INTERFACE_TYPE:
			type = "interface "; //$NON-NLS-1$
			templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
			break;
		case ENUM_TYPE:
			type = "enum "; //$NON-NLS-1$
			templateID = CodeGeneration.ENUM_BODY_TEMPLATE_ID;
			break;
		case ANNOTATION_TYPE:
			type = "@interface "; //$NON-NLS-1$
			templateID = CodeGeneration.ANNOTATION_BODY_TEMPLATE_ID;
			break;
		}
		buf.append(type);
		buf.append(getTypeName());
		writeSuperClass(buf, imports);
		writeSuperInterfaces(buf, imports);

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody = CodeGeneration.getTypeBody(templateID, parentCU, getTypeName(), lineDelimiter);
		if (typeBody != null)
			buf.append(typeBody);
		else
			buf.append(lineDelimiter);
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	/**
	 * Creates the type into the specified compilation unit.
	 * <p>
	 * Note, the content in the compilation unit will be completely replaced.
	 * </p>
	 * 
	 * @param parentCU
	 *            if the
	 * @param needsSave
	 *            indicates if the compilation unit should be saved
	 * @param monitor
	 *            a progress monitor to report progress (must not be
	 *            <code>null</code> and needs 10 progress steps)
	 * @throws CoreException
	 *             Thrown when the creation failed.
	 */
	@SuppressWarnings("unchecked")
	public void createType(ICompilationUnit parentCU, boolean needsSave, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			String lineDelimiter = GwtUtil.getLineSeparator(parentCU.getJavaProject().getProject());

			monitor.beginTask(NLS.bind("''{0}''...", getTypeName()), 10);

			currentType = parentCU.getType(getTypeNameWithoutParameters());

			IBuffer buffer = parentCU.getBuffer();

			String cuContent = constructCUContent(parentCU, constructSimpleTypeStub(), lineDelimiter);
			buffer.setContents(cuContent);

			CompilationUnit astRoot = ImportsManager.createASTForImports(parentCU);
			Set<String> existingImports = ImportsManager.getExistingImports(astRoot);

			ProgressUtil.checkCanceled(monitor);

			ImportsManager imports = new ImportsManager(astRoot);

			// add an import that will be removed again. Having this import
			// solves 14661
			imports.addImport(JavaModelUtil.concatenateName(parentCU.getParent().getElementName(), getTypeNameWithoutParameters()));

			String typeContent = constructTypeStub(parentCU, imports, lineDelimiter);

			AbstractTypeDeclaration typeNode = (AbstractTypeDeclaration) astRoot.types().get(0);
			int start = ((ASTNode) typeNode.modifiers().get(0)).getStartPosition();
			int end = typeNode.getStartPosition() + typeNode.getLength();

			buffer.replace(start, end - start, typeContent);

			IType createdType = parentCU.getType(getTypeName());

			ProgressUtil.checkCanceled(monitor);

			// add imports for superclass/interfaces, so types can be resolved
			// correctly
			imports.create(needsSave, new SubProgressMonitor(monitor, 1));

			JavaModelUtil.reconcile(parentCU);

			ProgressUtil.checkCanceled(monitor);

			// set up again
			astRoot = ImportsManager.createASTForImports(parentCU);
			imports = new ImportsManager(astRoot);

			createTypeMembers(createdType, imports, needsSave, new SubProgressMonitor(monitor, 1));

			// add imports
			imports.create(needsSave, ProgressUtil.subProgressMonitor(monitor, 1));

			ImportsManager.removeUnusedImports(parentCU, existingImports, needsSave);

			JavaModelUtil.reconcile(parentCU);

			// format the compilation unit
			final TextEdit edit = CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, parentCU.getBuffer().getContents(), 0, lineDelimiter, new HashMap(parentCU.getJavaProject().getOptions(true)));
			if (edit != null)
				JavaModelUtil.applyEdit(parentCU, edit, needsSave, ProgressUtil.subProgressMonitor(monitor, 1));

			if (needsSave)
				parentCU.commitWorkingCopy(true, ProgressUtil.subProgressMonitor(monitor, 1));
			else
				monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Hook method that gets called from <code>createType</code> to support
	 * adding of unanticipated methods, fields, and inner types to the created
	 * type.
	 * <p>
	 * Implementers can use any methods defined on <code>IType</code> to
	 * manipulate the new type.
	 * </p>
	 * <p>
	 * The source code of the new type will be formatted using the platform's
	 * formatter. Needed imports are added by the generator at the end of the
	 * type creation process using the given import manager.
	 * </p>
	 * 
	 * @param newType
	 *            the new type created via <code>createType</code>
	 * @param imports
	 *            an import manager which can be used to add new imports
	 * @param needsSave
	 *            indicates if the underlying CU should be saved
	 * @param monitor
	 *            a progress monitor to report progress. Must not be
	 *            <code>null</code>
	 * @throws CoreException
	 *             Thrown when the creation failed.
	 * 
	 * @see #createType(ICompilationUnit, boolean, IProgressMonitor)
	 */
	protected abstract void createTypeMembers(IType createdType, ImportsManager imports, boolean needsSave, IProgressMonitor monitor) throws CoreException;

	/**
	 * Hook method that gets called from <code>createType</code> to retrieve a
	 * file comment. This default implementation returns the content of the
	 * 'file comment' template.
	 * 
	 * @param parentCU
	 *            the parent compilation unit
	 * @param lineDelimiter
	 *            the line delimiter to use
	 * @return the file comment or <code>null</code> if a file comment is not
	 *         desired
	 * @throws CoreException
	 */
	protected String getFileComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		return CodeGeneration.getFileComment(parentCU, lineDelimiter);
	}

	private IPackageFragment getPackageFragment() {
		if (null != currentType)
			return currentType.getPackageFragment();

		return null;
	}

	/**
	 * Returns the full qualified name of the super class, default is
	 * <code>null</code> which means no super class.
	 * 
	 * @return the full qualified super class (maybe <code>null</code>)
	 */
	public String getSuperClass() {
		return null;
	}

	private StubTypeContext getSuperClassStubTypeContext() {
		if (superClassStubTypeContext == null) {
			String typeName;
			if (currentType != null)
				typeName = getTypeName();
			else
				typeName = DUMMY_CLASS_NAME;
			superClassStubTypeContext = TypeContextChecker.createSuperClassStubTypeContext(typeName, null, getPackageFragment());
		}
		return superClassStubTypeContext;
	}

	/**
	 * Returns the list of super interface, default is an empty list.
	 * 
	 * @return the list of super interfaces (full qualified)
	 */
	public List<String> getSuperInterfaces() {
		return Collections.emptyList();
	}

	private StubTypeContext getSuperInterfacesStubTypeContext() {
		if (superInterfaceStubTypeContext == null) {
			String typeName;
			if (currentType != null)
				typeName = getTypeName();
			else
				typeName = DUMMY_CLASS_NAME;
			superInterfaceStubTypeContext = TypeContextChecker.createSuperInterfaceStubTypeContext(typeName, null, getPackageFragment());
		}
		return superInterfaceStubTypeContext;
	}

	/**
	 * Hook method that gets called from <code>createType</code> to retrieve a
	 * type comment. This default implementation returns the content of the
	 * 'type comment' template.
	 * 
	 * @param parentCU
	 *            the parent compilation unit
	 * @param typeName
	 *            the type name to use
	 * @param lineDelimiter
	 *            the line delimiter to use
	 * @return the type comment or <code>null</code> if a type comment is not
	 *         desired
	 */
	protected String getTypeComment(ICompilationUnit parentCU, String lineDelimiter) {
		final String typeName = getTypeNameWithoutParameters();
		final String[] typeParamNames = new String[0];
		try {
			String comment = CodeGeneration.getTypeComment(parentCU, typeName, typeParamNames, lineDelimiter);
			if ((comment != null) && isValidComment(comment))
				return comment;
		} catch (CoreException e) {
			GwtCore.logError(NLS.bind("Error while generating type comment for ''{0}''", typeName), e);
		}
		return null;
	}

	/**
	 * Returns the type name of the type to create
	 * 
	 * @return the type name.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Returns the type name wihout parameters.
	 * 
	 * @return the type name wihout parameters
	 */
	protected String getTypeNameWithoutParameters() {
		return GwtUtil.getTypeNameWithoutParameters(getTypeName());
	}

	private boolean isValidComment(String template) {
		IScanner scanner = ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next = scanner.getNextToken();
			while (TokenScanner.isComment(next))
				next = scanner.getNextToken();
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
		}
		return false;
	}

	private void writeSuperClass(StringBuffer buf, ImportsManager imports) {
		String superclass = getSuperClass();
		if ((typeKind == CLASS_TYPE) && (null != superclass) && (superclass.length() > 0) && !"java.lang.Object".equals(superclass)) { //$NON-NLS-1$
			buf.append(" extends "); //$NON-NLS-1$

			ITypeBinding binding = null;
			if (currentType != null)
				binding = TypeContextChecker.resolveSuperClass(superclass, currentType, getSuperClassStubTypeContext());
			if (binding != null)
				buf.append(imports.addImport(binding));
			else
				buf.append(imports.addImport(superclass));
		}
	}

	private void writeSuperInterfaces(StringBuffer buf, ImportsManager imports) {
		List<String> interfaces = getSuperInterfaces();
		int last = interfaces.size() - 1;
		if (last >= 0) {
			if (typeKind != INTERFACE_TYPE)
				buf.append(" implements "); //$NON-NLS-1$
			else
				buf.append(" extends "); //$NON-NLS-1$
			String[] intfs = interfaces.toArray(new String[interfaces.size()]);
			ITypeBinding[] bindings;
			if (currentType != null)
				bindings = TypeContextChecker.resolveSuperInterfaces(intfs, currentType, getSuperInterfacesStubTypeContext());
			else
				bindings = new ITypeBinding[intfs.length];
			for (int i = 0; i <= last; i++) {
				ITypeBinding binding = bindings[i];
				if (binding != null)
					buf.append(imports.addImport(binding));
				else
					buf.append(imports.addImport(intfs[i]));
				if (i < last)
					buf.append(',');
			}
		}
	}
}
