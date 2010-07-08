/*******************************************************************************
 * Copyright (c) 2006, 2010 EclipseGuru and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseGuru - initial API and implementation
 *     dobesv - contributed patch for issue 2
 *******************************************************************************/
package org.eclipseguru.gwt.core.internal.codegen;

import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.internal.jdtext.ImportsManager;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

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

	/** GENERATED */
	private static final String GENERATED = "javax.annotation.Generated";
	/** GENERATOR */
	private static final String GENERATOR = GwtCore.PLUGIN_ID.concat(".AsynServiceCodeGenerator");

	/** DATE_FORMAT_ISO8601 */
	private static final DateFormat DATE_FORMAT_ISO8601;
	static {
		DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		DATE_FORMAT_ISO8601.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Add all methods from the given interface and its super interfaces to the
	 * given list.
	 * 
	 * @param type
	 *            Interface to get the methods of
	 * @param output
	 *            List to add the methods to
	 * @throws CoreException
	 *             If an error occurs
	 */
	private static void collectAllInterfaceAndSuperMethods(final IType type, final List<IMethod> output, final Set<String> methodSigs) throws CoreException {
		for (final IMethod method : type.getMethods()) {
			// Don't repeat the same method
			final String methodSig = method.getElementName() + method.getSignature();
			if (!methodSigs.add(methodSig)) {
				continue;
			}

			output.add(method);
		}
		for (final String superInterfaceName : type.getSuperInterfaceNames()) {
			final String[][] resolvedType = type.resolveType(superInterfaceName);
			if (resolvedType != null) {
				for (final String[] typeSegments : resolvedType) {
					final IType superInterface = type.getJavaProject().findType(Signature.toQualifiedName(typeSegments));
					if (superInterface != null) {
						collectAllInterfaceAndSuperMethods(superInterface, output, methodSigs);
					}
				}
			}
		}
	}

	/**
	 * Collects imports including imports from super-interfaces of the specified
	 * type.
	 * 
	 * @param type
	 * @param imports
	 * @throws JavaModelException
	 */
	private static void collectImports(final IType type, final ImportsManager imports) throws JavaModelException {
		final ICompilationUnit compilationUnit = type.getCompilationUnit();
		if (compilationUnit != null) {
			final IImportDeclaration[] existingImports = compilationUnit.getImports();
			for (final IImportDeclaration declaration : existingImports) {
				if (Flags.isStatic(declaration.getFlags())) {
					String name = Signature.getSimpleName(declaration.getElementName());
					final boolean isField = !name.endsWith("()"); //$NON-NLS-1$
					if (!isField) {
						name = name.substring(0, name.length() - 2);
					}
					final String qualifier = Signature.getQualifier(declaration.getElementName());
					imports.addStaticImport(qualifier, name, isField);
				} else {
					imports.addImport(declaration.getElementName());
				}
			}
		}

		for (final String superInterfaceName : type.getSuperInterfaceNames()) {
			final String[][] resolvedType = type.resolveType(superInterfaceName);
			if (resolvedType != null) {
				for (final String[] typeSegments : resolvedType) {
					final IType superInterface = type.getJavaProject().findType(Signature.toQualifiedName(typeSegments));
					if (superInterface != null) {
						collectImports(superInterface, imports);
					}
				}
			}
		}
	}

	/**
	 * Returns the compilation unit name for the async service interface for the
	 * specified remote service.
	 * 
	 * @param remoteServiceType
	 * @return the async service CU name
	 */
	public static String getAsyncCUName(final IType remoteServiceType) {
		if (remoteServiceType.isBinary()) {
			return getAsyncTypeNameWithoutParameters(remoteServiceType).concat(".class");
		}
		return getAsyncTypeNameWithoutParameters(remoteServiceType).concat(".java");
	}

	/**
	 * Returns the type name for the async service interface for the specified
	 * remote service.
	 * 
	 * @param remoteServiceType
	 * @return the async service type name
	 */
	public static String getAsyncTypeNameWithoutParameters(final IType remoteServiceType) {
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
	 * @param parentCU
	 * @param asyncServiceTypeName
	 * @return <code>true</code> if the async generation is allowed,
	 *         <code>false</code> otherwise
	 * @throws CoreException
	 */
	public static boolean isAllowedToGenerateAsyncServiceType(final ICompilationUnit parentCU, final String asyncServiceTypeName) throws CoreException {
		if (!parentCU.exists()) {
			return true;
		}

		final IType asyncServiceType = parentCU.getType(asyncServiceTypeName);
		if (!asyncServiceType.exists()) {
			return true;
		}

		final ISourceRange javadocRange = asyncServiceType.getJavadocRange();
		if (null == javadocRange) {
			return false;
		}

		final String text = parentCU.getBuffer().getText(javadocRange.getOffset(), javadocRange.getLength());
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
	public AsyncServiceCodeGenerator(final IType remoteServiceType) throws CoreException {
		super(getAsyncTypeNameWithoutParameters(remoteServiceType), INTERFACE_TYPE, remoteServiceType.getFlags());
		this.remoteServiceType = remoteServiceType;
	}

	/**
	 * Appends the async callback parameter to the buffer
	 * 
	 * @param method
	 * @param buffer
	 * @param is50OrHigher
	 * @throws CoreException
	 */
	private void appendAsyncCallbackParameter(final IMethod method, final ImportsManager imports, final StringBuffer buffer, final boolean is50OrHigher) throws CoreException {
		buffer.append(imports.addImport(ASYNC_CALLBACK));
		if (is50OrHigher) {
			final String returnType = method.getReturnType();
			if (Signature.SIG_BOOLEAN.equals(returnType)) {
				buffer.append("<Boolean>");
			} else if (Signature.SIG_BYTE.equals(returnType)) {
				buffer.append("<Byte>");
			} else if (Signature.SIG_CHAR.equals(returnType)) {
				buffer.append("<Character>");
			} else if (Signature.SIG_DOUBLE.equals(returnType)) {
				buffer.append("<Double>");
			} else if (Signature.SIG_FLOAT.equals(returnType)) {
				buffer.append("<Float>");
			} else if (Signature.SIG_INT.equals(returnType)) {
				buffer.append("<Integer>");
			} else if (Signature.SIG_LONG.equals(returnType)) {
				buffer.append("<Long>");
			} else if (Signature.SIG_SHORT.equals(returnType)) {
				buffer.append("<Short>");
			} else if (Signature.SIG_VOID.equals(returnType)) {
				buffer.append("<?>");
			} else {
				buffer.append('<');
				buffer.append(Signature.toString(returnType));
				buffer.append('>');
			}
		}
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
	private void appendMethodParameters(final IMethod method, final StringBuffer buffer) throws CoreException {
		final String[] parameterTypes = method.getParameterTypes();
		final String[] parameterNames = method.getRawParameterNames();
		final int flags = method.getFlags();
		final boolean varargs = Flags.isVarargs(flags);
		final int parameterLength = parameterTypes.length;
		for (int j = 0; j < parameterLength; j++) {
			if (j > 0) {
				buffer.append(","); //$NON-NLS-1$
			}
			buffer.append(Signature.toString(parameterTypes[j]));
			if (varargs && (j == parameterLength - 1)) {
				final int length = buffer.length();
				if ((length >= 2) && (buffer.indexOf("[]", length - 2) >= 0)) {
					buffer.setLength(length - 2);
				}
				buffer.append("..."); //$NON-NLS-1$
			}
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(parameterNames[j]);
		}
	}

	private void appendMethodTypeParameters(final IMethod method, final StringBuffer methodContent) throws JavaModelException {
		final ITypeParameter[] parameters = method.getTypeParameters();
		for (final ITypeParameter typeParameter : parameters) {
			final String source = typeParameter.getSource();
			final String elementName = typeParameter.getElementName();
			methodContent.append(source);
			methodContent.append(elementName);
		}
	}

	@SuppressWarnings("unchecked")
	private TagElement createGeneratedTagForMethod(final ASTRewrite cuRewrite) {
		final TagElement generated = cuRewrite.getAST().newTagElement();
		generated.setTagName("@generated");
		final TextElement element = cuRewrite.getAST().newTextElement();
		element.setText("generated method with asynchronous callback parameter to be used on the client side");
		generated.fragments().add(element);
		return generated;
	}

	@SuppressWarnings("unchecked")
	private TagElement createGeneratedTagForType(final ASTRewrite cuRewrite) {
		final TagElement generated = cuRewrite.getAST().newTagElement();
		generated.setTagName("@generated");
		final TextElement element = cuRewrite.getAST().newTextElement();
		element.setText("generated asynchronous callback interface to be used on the client side");
		generated.fragments().add(element);
		return generated;
	}

	private Javadoc createJavadocIfNecessary(final ASTRewrite cuRewrite, final BodyDeclaration bd, final TextEditGroup textEditGroup) {
		Javadoc javadoc = bd.getJavadoc();
		if (null == javadoc) {
			javadoc = cuRewrite.getAST().newJavadoc();
			cuRewrite.set(bd, bd.getJavadocProperty(), javadoc, textEditGroup);
		}
		return javadoc;
	}

	@Override
	protected void createTypeMembers(final IType createdType, final ImportsManager imports, final IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NLS.bind("Generating methods in ''{0}''...", createdType.getElementName()), 10);
		try {

			// add all existing imports
			writeImports(imports);

			// add all public methods
			final IMethod[] methods = getMethods();
			for (final IMethod method : methods) {
				// skip constructors and binary, static, private or protected methods
				if (method.isConstructor() || method.isBinary() || Flags.isStatic(method.getFlags()) || Flags.isPrivate(method.getFlags()) || Flags.isProtected(method.getFlags())) {
					continue;
				}

				final StringBuffer methodContent = new StringBuffer();

				// javadoc
				final ISourceRange javadocRange = method.getJavadocRange();
				if (null != javadocRange) {
					final IBuffer buffer = method.getOpenable().getBuffer();
					if (buffer != null) {
						methodContent.append(buffer.getText(javadocRange.getOffset(), javadocRange.getLength()));
					}
				}

				// Java 1.5 features
				final boolean is50OrHigher = JavaModelUtil.is50OrHigher(createdType.getJavaProject());

				// @Generated annotation
				if (is50OrHigher) {
					methodContent.append('@');
					methodContent.append(imports.addImport(GENERATED));
					methodContent.append('(');
					methodContent.append("value={").append('"').append(GENERATOR).append('"').append('}');
					methodContent.append(',');
					methodContent.append("date=").append('"').append(DATE_FORMAT_ISO8601.format(new Date())).append('"');
					methodContent.append(',');
					methodContent.append("comments=").append('"').append("from ").append(remoteServiceType.getFullyQualifiedName('.')).append('[').append(Signature.toString(method.getSignature(), method.getElementName(), method.getParameterNames(), true, true)).append(']').append('"');
					methodContent.append(')');
					methodContent.append(' ');
				}

				// generics declaration
				if (is50OrHigher && (method.getTypeParameters().length > 0)) {
					appendMethodTypeParameters(method, methodContent);
					methodContent.append(' ');
				}

				// methos declaration
				methodContent.append(Signature.toString(Signature.SIG_VOID));
				methodContent.append(' ');
				methodContent.append(method.getElementName());

				// parameters
				methodContent.append('(');
				if (method.getParameterTypes().length > 0) {
					appendMethodParameters(method, methodContent);
					methodContent.append(", ");
				}
				appendAsyncCallbackParameter(method, imports, methodContent, is50OrHigher);
				methodContent.append(')');

				// method is abstract and without exceptions
				methodContent.append(';');

				// create the method
				createdType.createMethod(methodContent.toString(), null, false, null);
			}

			// update Javadoc
			updateJavadoc(createdType, ProgressUtil.subProgressMonitor(monitor, 1));

		} finally {
			monitor.done();
		}
	}

	@Override
	protected String getFileComment(final ICompilationUnit parentCU, final String lineDelimiter) throws CoreException {
		// take remote service comment if possible
		try {
			final String source = remoteServiceType.getCompilationUnit().getSource();
			final IScanner scanner = ToolFactory.createScanner(true, false, false, false);
			final StringBuffer buffer = new StringBuffer();
			scanner.setSource(source.toCharArray());
			int next = scanner.getNextToken();
			while (TokenScanner.isComment(next)) {
				buffer.append(scanner.getCurrentTokenSource());
				next = scanner.getNextToken();
			}
			if (buffer.length() > 0) {
				return buffer.toString();
			}
		} catch (final Exception e) {
			// ignore
		}
		return super.getFileComment(parentCU, lineDelimiter);
	}

	/**
	 * Return all methods of the remote service type and its super interfaces.
	 */
	IMethod[] getMethods() throws CoreException {
		final List<IMethod> result = new ArrayList<IMethod>();
		collectAllInterfaceAndSuperMethods(remoteServiceType, result, new HashSet<String>());
		return result.toArray(new IMethod[result.size()]);
	}

	@Override
	protected String getTypeComment(final ICompilationUnit parentCU, final String lineDelimiter) {
		// take remote service comment if possible
		try {
			final ISourceRange javadocRange = remoteServiceType.getJavadocRange();
			if (null != javadocRange) {
				final IBuffer buffer = remoteServiceType.getOpenable().getBuffer();
				if (buffer != null) {
					final String javadoc = buffer.getText(javadocRange.getOffset(), javadocRange.getLength());
					if ((null != javadoc) && (javadoc.trim().length() > 0)) {
						final String[] lines = Strings.convertIntoLines(javadoc);
						final StringBuffer result = new StringBuffer();
						for (int i = 0; i < lines.length; i++) {
							result.append(lines[i]);
							if (i < lines.length - 1) {
								result.append(lineDelimiter);
							}
						}
						return result.toString();
					}
				}
			}
		} catch (final JavaModelException e) {
			// ignore
		}
		return super.getTypeComment(parentCU, lineDelimiter);
	}

	private void updateJavadoc(final IType createdType, final IProgressMonitor monitor) throws JavaModelException, CoreException, ValidateEditException {
		final CompilationUnit cu = createdType.getCompilationUnit().reconcile(AST.JLS3, true, null, null);
		final ASTRewrite cuRewrite = ASTRewrite.create(cu.getAST());
		final TextEditGroup textEditGroup = new TextEditGroup("Updating JavaDoc of interface " + createdType.getElementName());

		// find remote service type and rewrite it
		final ListRewrite typeRewrite = cuRewrite.getListRewrite(cu, CompilationUnit.TYPES_PROPERTY);
		for (final Iterator stream = typeRewrite.getOriginalList().iterator(); stream.hasNext();) {
			final TypeDeclaration td = (TypeDeclaration) stream.next();
			if (td.getName().getIdentifier().equals(getTypeNameWithoutParameters())) {
				updateTypeJavaDoc(cuRewrite, td, textEditGroup);
			}
		}

		// apply edit
		final TextEdit edit = cuRewrite.rewriteAST();
		JavaModelUtil.applyEdit(createdType.getCompilationUnit(), edit, false, ProgressUtil.subProgressMonitor(monitor, 1));
	}

	private void updateMethodJavadoc(final ASTRewrite cuRewrite, final MethodDeclaration md, final TextEditGroup textEditGroup) {
		// create javadoc
		final Javadoc javadoc = createJavadocIfNecessary(cuRewrite, md, textEditGroup);
		final ListRewrite javadocRewrite = cuRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

		// find last @param tag and remove @throws tags and @gwt.typeArgs tags
		ASTNode lastParamPos = null;
		boolean hasReturnTag = false;
		for (final Iterator stream = javadocRewrite.getOriginalList().iterator(); stream.hasNext();) {
			final TagElement element = (TagElement) stream.next();
			if (null != element.getTagName()) {
				final String tagName = element.getTagName();
				if (tagName.equals(TAG_PARAM)) {
					lastParamPos = element;
				} else if (tagName.equals(TAG_RETURN) && !element.fragments().isEmpty()) {
					hasReturnTag = true;
					cuRewrite.set(element, TagElement.TAG_NAME_PROPERTY, TAG_GWT_CALLBACK_RETURN, textEditGroup);
				} else if (tagName.equals(TAG_THROWS) || tagName.equals(TAG_GWT_TYPE_ARGS)) {
					javadocRewrite.remove(element, textEditGroup);
				}
			}
		}

		// create @param callback
		final TagElement callbackParamTag = cuRewrite.getAST().newTagElement();
		if (null != lastParamPos) {
			javadocRewrite.insertAfter(callbackParamTag, lastParamPos, textEditGroup);
		} else {
			javadocRewrite.insertLast(callbackParamTag, textEditGroup);
		}
		cuRewrite.set(callbackParamTag, TagElement.TAG_NAME_PROPERTY, TAG_PARAM, textEditGroup);
		final ListRewrite tagRewrite = cuRewrite.getListRewrite(callbackParamTag, TagElement.FRAGMENTS_PROPERTY);
		final TextElement space = cuRewrite.getAST().newTextElement();
		tagRewrite.insertFirst(space, textEditGroup);
		final SimpleName callbackName = cuRewrite.getAST().newSimpleName(CALLBACK);
		tagRewrite.insertAfter(callbackName, space, textEditGroup);
		final TextElement callbackDescription = cuRewrite.getAST().newTextElement();
		callbackDescription.setText("the callback that will be called to receive the return value");
		tagRewrite.insertAfter(callbackDescription, callbackName, textEditGroup);
		if (hasReturnTag) {
			final TextElement text = cuRewrite.getAST().newTextElement();
			text.setText(NLS.bind("(see <code>{0}</code> tag)", TAG_GWT_CALLBACK_RETURN));
			tagRewrite.insertAfter(text, callbackDescription, textEditGroup);
		}

		// add @generated tag
		javadocRewrite.insertLast(createGeneratedTagForMethod(cuRewrite), textEditGroup);
	}

	private void updateTypeJavaDoc(final ASTRewrite cuRewrite, final TypeDeclaration td, final TextEditGroup textEditGroup) {
		// add @generated tag to JavaDoc
		final Javadoc javadoc = createJavadocIfNecessary(cuRewrite, td, textEditGroup);
		final ListRewrite javadocRewrite = cuRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
		javadocRewrite.insertLast(createGeneratedTagForType(cuRewrite), textEditGroup);

		// rewrite methods
		final ListRewrite bodyRewrite = cuRewrite.getListRewrite(td, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		for (final Iterator stream = bodyRewrite.getOriginalList().iterator(); stream.hasNext();) {
			final ASTNode node = (ASTNode) stream.next();
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
	private void writeImports(final ImportsManager imports) throws JavaModelException {
		collectImports(remoteServiceType, imports);
	}
}
