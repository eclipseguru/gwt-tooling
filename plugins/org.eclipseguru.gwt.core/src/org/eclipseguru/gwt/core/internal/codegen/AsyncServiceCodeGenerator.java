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

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.ValidateEditException;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.internal.jdtext.ImportsManager;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

/**
 * A utility class for generating code
 */
@SuppressWarnings("restriction") 
public class AsyncServiceCodeGenerator extends JdtTypeGenerator {

	/** TAG_GWT_CALLBACK_VALUE */
	private static final String TAG_GWT_CALLBACK_RETURN = "@gwt.callbackReturn";
	/** TAG_GWT_TYPEARGS */
	private static final String TAG_GWT_TYPE_ARGS = "@gwt.typeArgs";
	/** TAG_THROWS */
	private static final String TAG_THROWS = TagElement.TAG_THROWS;
	/** TAG_PARAM */
	private static final String TAG_PARAM = TagElement.TAG_PARAM;
	/** TAG_RETURN */
	private static final String TAG_RETURN = TagElement.TAG_RETURN;
	/** CALLBACK */
	private static final String CALLBACK = "callback";

	/** ASYNC_CALLBACK */
	private static final String ASYNC_CALLBACK = "com.google.gwt.user.client.rpc.AsyncCallback";

	/**
	 * Returns the compilation unit name for the async service interface for the
	 * specified remote service.
	 * 
	 * @param remoteServiceType
	 * @return the async service CU name
	 */
	public static String getAsyncCUName(IType remoteServiceType) {
		if (remoteServiceType.isBinary())
			return getAsyncTypeNameWithoutParameters(remoteServiceType).concat(".class");
		return getAsyncTypeNameWithoutParameters(remoteServiceType).concat(".java");
	}

	/**
	 * Returns the type name for the async service interface for the specified
	 * remote service.
	 * 
	 * @param remoteServiceType
	 * @return the async service type name
	 */
	public static String getAsyncTypeNameWithoutParameters(IType remoteServiceType) {
		return GwtUtil.getTypeNameWithoutParameters(remoteServiceType.getElementName()).concat("Async");
	}

	/**
	 * Indicates if the async service stup with the specified type name is
	 * allowed to be generated in the specified compilation unit.
	 * <p>
	 * The generation of the async service interface is only allowed if the
	 * parent compilation unit or the target type do not exist yet. If the
	 * target type already exists it has to contain the
	 * <code>&#64;generated</code> tag in its JavaDoc comment.
	 * </p>
	 * 
	 * 
	 * @param parentCU
	 * @param asyncServiceTypeName
	 * @return <code>true</code> if the async generation is allowed,
	 *         <code>false</code> otherwise
	 * @throws CoreException
	 */
	public static boolean isAllowedToGenerateAsyncServiceType(ICompilationUnit parentCU, String asyncServiceTypeName) throws CoreException {
		if (!parentCU.exists())
			return true;

		IType asyncServiceType = parentCU.getType(asyncServiceTypeName);
		if (!asyncServiceType.exists())
			return true;

		ISourceRange javadocRange = asyncServiceType.getJavadocRange();
		if (null == javadocRange)
			return false;

		String text = parentCU.getBuffer().getText(javadocRange.getOffset(), javadocRange.getLength());
		return text.contains("@generated");
	}

	private final IType remoteServiceType;

	/**
	 * Creates a new instance using the specified remote service.
	 * 
	 * @param parentCU
	 *            the CU where the async remote service will be created in
	 * @param remoteServiceType
	 *            the remote service to create the asynch service interface for
	 * @throws CoreException
	 *             if the remote service cannot be accessed
	 */
	public AsyncServiceCodeGenerator(IType remoteServiceType) throws CoreException {
		super(getAsyncTypeNameWithoutParameters(remoteServiceType), INTERFACE_TYPE, remoteServiceType.getFlags());
		this.remoteServiceType = remoteServiceType;
	}

	/**
	 * Appens the async callback parameter to the buffer
	 * 
	 * @param method
	 * @param buffer
	 * @throws CoreException
	 */
	private void appendAsyncCallbackParameter(IMethod method, ImportsManager imports, StringBuffer buffer) throws CoreException {
		buffer.append(imports.addImport(ASYNC_CALLBACK));
		buffer.append(' ');
		buffer.append(CALLBACK);
	}

	/**
	 * Appends all existing parameters from the specified method to the
	 * specified buffer
	 * 
	 * @param method
	 * @param buffer
	 * @throws JavaModelException
	 */
	private void appendMethodParameters(IMethod method, StringBuffer buffer) throws CoreException {
		final String[] parameterTypes = method.getParameterTypes();
		final String[] parameterNames = method.getRawParameterNames();
		final int flags = method.getFlags();
		final boolean varargs = Flags.isVarargs(flags);
		final int parameterLength = parameterTypes.length;
		for (int j = 0; j < parameterLength; j++) {
			if (j > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(Signature.toString(parameterTypes[j]));
			if (varargs && (j == parameterLength - 1)) {
				final int length = buffer.length();
				if ((length >= 2) && (buffer.indexOf("[]", length - 2) >= 0)) //$NON-NLS-1$
					buffer.setLength(length - 2);
				buffer.append("..."); //$NON-NLS-1$
			}
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(parameterNames[j]);
		}
	}

	@SuppressWarnings("unchecked")
	private TagElement createGeneratedTagForMethod(ASTRewrite cuRewrite) {
		TagElement generated = cuRewrite.getAST().newTagElement();
		generated.setTagName("@generated");
		TextElement element = cuRewrite.getAST().newTextElement();
		element.setText("generated method with asynchronous callback parameter to be used on the client side");
		generated.fragments().add(element);
		return generated;
	}

	@SuppressWarnings("unchecked")
	private TagElement createGeneratedTagForType(ASTRewrite cuRewrite) {
		TagElement generated = cuRewrite.getAST().newTagElement();
		generated.setTagName("@generated");
		TextElement element = cuRewrite.getAST().newTextElement();
		element.setText("generated asynchronous callback interface to be used on the client side");
		generated.fragments().add(element);
		return generated;
	}

	/**
	 * @param cuRewrite
	 * @param bd
	 * @param textEditGroup
	 */
	private Javadoc createJavadocIfNecessary(ASTRewrite cuRewrite, BodyDeclaration bd, TextEditGroup textEditGroup) {
		Javadoc javadoc = bd.getJavadoc();
		if (null == javadoc) {
			javadoc = cuRewrite.getAST().newJavadoc();
			cuRewrite.set(bd, bd.getJavadocProperty(), javadoc, textEditGroup);
		}
		return javadoc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.internal.codegen.JdtTypeGenerator#createTypeMembers(org.eclipse.jdt.core.IType,
	 *      org.eclipseguru.gwt.core.internal.jdtext.ImportsManager,
	 *      org.eclipse.core.runtime.SubProgressMonitor)
	 */
	@Override
	protected void createTypeMembers(IType createdType, ImportsManager imports, boolean needsSave, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NLS.bind("Generating methods in ''{0}''...", createdType.getElementName()), 10);
		try {

			// add all existing imports
			writeImports(imports);

			// add all public methods
			IMethod[] methods = remoteServiceType.getMethods();
			for (IMethod method : methods) {
				// skip contructors and binary, static, private or protected
				// methods
				if (method.isConstructor() || method.isBinary() || Flags.isStatic(method.getFlags()) || Flags.isPrivate(method.getFlags()) || Flags.isProtected(method.getFlags()))
					continue;

				StringBuffer methodContent = new StringBuffer();

				// javadoc
				ISourceRange javadocRange = method.getJavadocRange();
				if (null != javadocRange) {
					IBuffer buffer = remoteServiceType.getOpenable().getBuffer();
					if (buffer != null)
						methodContent.append(buffer.getText(javadocRange.getOffset(), javadocRange.getLength()));
				}

				// declaration
				methodContent.append("void ");
				methodContent.append(method.getElementName());

				// parameters
				methodContent.append('(');
				if (method.getParameterTypes().length > 0) {
					appendMethodParameters(method, methodContent);
					methodContent.append(',');
				}
				appendAsyncCallbackParameter(method, imports, methodContent);
				methodContent.append(')');

				// method is abstract and without exceptions
				methodContent.append(';');

				// create the method
				createdType.createMethod(methodContent.toString(), null, false, null);
			}

			// update Javadoc
			updateJavadoc(createdType, needsSave, ProgressUtil.subProgressMonitor(monitor, 1));

		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.internal.codegen.JdtTypeGenerator#getFileComment(org.eclipse.jdt.core.ICompilationUnit,
	 *      java.lang.String)
	 */
	@Override
	protected String getFileComment(ICompilationUnit parentCU, String lineDelimiter) throws CoreException {
		// take remote service comment if possible
		try {
			String source = remoteServiceType.getCompilationUnit().getSource();
			IScanner scanner = ToolFactory.createScanner(true, false, false, false);
			StringBuffer buffer = new StringBuffer();
			scanner.setSource(source.toCharArray());
			int next = scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				buffer.append(scanner.getCurrentTokenSource());
				next = scanner.getNextToken();
			}
			if (buffer.length() > 0)
				return buffer.toString();
		} catch (Exception e) {
			// ignore
		}
		return super.getFileComment(parentCU, lineDelimiter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.internal.codegen.JdtTypeGenerator#getTypeComment(org.eclipse.jdt.core.ICompilationUnit,
	 *      java.lang.String)
	 */
	@Override
	protected String getTypeComment(ICompilationUnit parentCU, String lineDelimiter) {
		// take remote service comment if possible
		try {
			ISourceRange javadocRange = remoteServiceType.getJavadocRange();
			if (null != javadocRange) {
				IBuffer buffer = remoteServiceType.getOpenable().getBuffer();
				if (buffer != null) {
					String javadoc = buffer.getText(javadocRange.getOffset(), javadocRange.getLength() - 1);
					if ((null != javadoc) && (javadoc.trim().length() > 0)) {
						String[] lines = Strings.convertIntoLines(javadoc);
						StringBuffer result = new StringBuffer();
						for (int i = 0; i < lines.length; i++) {
							result.append(lines[i]);
							if (i < lines.length - 1)
								result.append(lineDelimiter);
						}
						return result.toString();
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return super.getTypeComment(parentCU, lineDelimiter);
	}

	/**
	 * @param createdType
	 * @param needsSave
	 * @param monitor
	 * @throws JavaModelException
	 * @throws CoreException
	 * @throws ValidateEditException
	 */
	private void updateJavadoc(IType createdType, boolean needsSave, IProgressMonitor monitor) throws JavaModelException, CoreException, ValidateEditException {
		CompilationUnit cu = createdType.getCompilationUnit().reconcile(AST.JLS3, true, null, null);
		ASTRewrite cuRewrite = ASTRewrite.create(cu.getAST());
		TextEditGroup textEditGroup = new TextEditGroup("Updating JavaDoc of interface " + createdType.getElementName());

		// find remote service type and rewrite it
		ListRewrite typeRewrite = cuRewrite.getListRewrite(cu, CompilationUnit.TYPES_PROPERTY);
		for (Iterator stream = typeRewrite.getOriginalList().iterator(); stream.hasNext();) {
			TypeDeclaration td = (TypeDeclaration) stream.next();
			if (td.getName().getIdentifier().equals(getTypeNameWithoutParameters()))
				updateTypeJavaDoc(cuRewrite, td, textEditGroup);
		}

		// apply edit
		TextEdit edit = cuRewrite.rewriteAST();
		JavaModelUtil.applyEdit(createdType.getCompilationUnit(), edit, needsSave, ProgressUtil.subProgressMonitor(monitor, 1));
	}

	private void updateMethodJavadoc(ASTRewrite cuRewrite, MethodDeclaration md, TextEditGroup textEditGroup) {
		// create javadoc
		Javadoc javadoc = createJavadocIfNecessary(cuRewrite, md, textEditGroup);
		ListRewrite javadocRewrite = cuRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

		// find last @param tag and remove @throws tags and @gwt.typeArgs tags
		ASTNode lastParamPos = null;
		boolean hasReturnTag = false;
		for (Iterator stream = javadocRewrite.getOriginalList().iterator(); stream.hasNext();) {
			TagElement element = (TagElement) stream.next();
			if (null != element.getTagName()) {
				String tagName = element.getTagName();
				if (tagName.equals(TAG_PARAM))
					lastParamPos = element;
				else if (tagName.equals(TAG_RETURN) && !element.fragments().isEmpty()) {
					hasReturnTag = true;
					cuRewrite.set(element, TagElement.TAG_NAME_PROPERTY, TAG_GWT_CALLBACK_RETURN, textEditGroup);
				} else if (tagName.equals(TAG_THROWS) || tagName.equals(TAG_GWT_TYPE_ARGS))
					javadocRewrite.remove(element, textEditGroup);
			}
		}

		// create @param callback
		TagElement callbackParamTag = cuRewrite.getAST().newTagElement();
		if (null != lastParamPos)
			javadocRewrite.insertAfter(callbackParamTag, lastParamPos, textEditGroup);
		else
			javadocRewrite.insertLast(callbackParamTag, textEditGroup);
		cuRewrite.set(callbackParamTag, TagElement.TAG_NAME_PROPERTY, TAG_PARAM, textEditGroup);
		ListRewrite tagRewrite = cuRewrite.getListRewrite(callbackParamTag, TagElement.FRAGMENTS_PROPERTY);
		TextElement space = cuRewrite.getAST().newTextElement();
		tagRewrite.insertFirst(space, textEditGroup);
		SimpleName callbackName = cuRewrite.getAST().newSimpleName(CALLBACK);
		tagRewrite.insertAfter(callbackName, space, textEditGroup);
		TextElement callbackDescription = cuRewrite.getAST().newTextElement();
		callbackDescription.setText("the callback that will be called to receive the return value");
		tagRewrite.insertAfter(callbackDescription, callbackName, textEditGroup);
		if (hasReturnTag) {
			TextElement text = cuRewrite.getAST().newTextElement();
			text.setText(NLS.bind("(see <code>{0}</code> tag)", TAG_GWT_CALLBACK_RETURN));
			tagRewrite.insertAfter(text, callbackDescription, textEditGroup);
		}

		// add @generated tag
		javadocRewrite.insertLast(createGeneratedTagForMethod(cuRewrite), textEditGroup);
	}

	private void updateTypeJavaDoc(ASTRewrite cuRewrite, TypeDeclaration td, TextEditGroup textEditGroup) {
		// add @generated tag to JavaDoc
		Javadoc javadoc = createJavadocIfNecessary(cuRewrite, td, textEditGroup);
		ListRewrite javadocRewrite = cuRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		javadocRewrite.insertLast(createGeneratedTagForType(cuRewrite), textEditGroup);

		// rewrite methods
		ListRewrite bodyRewrite = cuRewrite.getListRewrite(td, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		for (Iterator stream = bodyRewrite.getOriginalList().iterator(); stream.hasNext();) {
			ASTNode node = (ASTNode) stream.next();
			switch (node.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				// rewrite method
				updateMethodJavadoc(cuRewrite, (MethodDeclaration) node, textEditGroup);
				break;
			}
		}
	}

	/**
	 * Writes the existing imports from the remote service.
	 * 
	 * @param imports
	 * @throws JavaModelException
	 */
	private void writeImports(ImportsManager imports) throws JavaModelException {
		IImportDeclaration[] existingImports = remoteServiceType.getCompilationUnit().getImports();
		for (IImportDeclaration declaration : existingImports) {
			if (Flags.isStatic(declaration.getFlags())) {
				String name = Signature.getSimpleName(declaration.getElementName());
				boolean isField = !name.endsWith("()"); //$NON-NLS-1$
				if (!isField)
					name = name.substring(0, name.length() - 2);
				String qualifier = Signature.getQualifier(declaration.getElementName());
				imports.addStaticImport(qualifier, name, isField);
			} else
				imports.addImport(declaration.getElementName());
		}
	}
}
