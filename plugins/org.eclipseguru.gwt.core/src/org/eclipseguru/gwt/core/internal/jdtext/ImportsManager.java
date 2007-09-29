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
package org.eclipseguru.gwt.core.internal.jdtext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.text.edits.TextEdit;

/**
 * Class used in rewrite routines to add and remove needed imports to a
 * compilation unit.
 */
@SuppressWarnings("restriction")
public class ImportsManager {

	/**
	 * Creates an AST that is suitable for working with import declarations but
	 * nothing more.
	 * 
	 * @param cu
	 * @return the AST
	 */
	public static CompilationUnit createASTForImports(ICompilationUnit cu) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(false);
		parser.setFocalPosition(0);
		return (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Returns all existing imports from the specified AST.
	 * 
	 * @param root
	 * @return
	 */
	public static Set<String> getExistingImports(CompilationUnit root) {
		List imports = root.imports();
		Set<String> res = new HashSet<String>(imports.size());
		for (int i = 0; i < imports.size(); i++)
			res.add(ASTNodes.asString((ImportDeclaration) imports.get(i)));
		return res;
	}

	/**
	 * Removes unused imports from the specified compiliation unit.
	 * <p>
	 * Adapted from
	 * <code>org.eclipse.jdt.ui.wizards.NewTypeWizardPage#removeUnusedImports</code>.
	 * </p>
	 * 
	 * @param cu
	 *            the compilation unit
	 * @param existingImports
	 *            the set of existing imports
	 * @param needsSave
	 *            if the compilation unit needs to be saved
	 * @throws CoreException
	 *             if an error occured
	 */
	public static void removeUnusedImports(ICompilationUnit cu, Set existingImports, boolean needsSave) throws CoreException {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);

		CompilationUnit root = (CompilationUnit) parser.createAST(null);
		if (root.getProblems().length == 0)
			return;

		List importsDecls = root.imports();
		if (importsDecls.isEmpty())
			return;
		ImportsManager imports = new ImportsManager(root);

		int importsEnd = ASTNodes.getExclusiveEnd((ASTNode) importsDecls.get(importsDecls.size() - 1));
		IProblem[] problems = root.getProblems();
		for (IProblem curr : problems) {
			if (curr.getSourceEnd() < importsEnd) {
				int id = curr.getID();
				// not visible problems hide unused -> remove both
				if ((id == IProblem.UnusedImport) || (id == IProblem.NotVisibleType)) {
					int pos = curr.getSourceStart();
					for (int k = 0; k < importsDecls.size(); k++) {
						ImportDeclaration decl = (ImportDeclaration) importsDecls.get(k);
						if ((decl.getStartPosition() <= pos) && (pos < decl.getStartPosition() + decl.getLength())) {
							if (existingImports.isEmpty() || !existingImports.contains(ASTNodes.asString(decl))) {
								String name = decl.getName().getFullyQualifiedName();
								if (decl.isOnDemand())
									name += ".*"; //$NON-NLS-1$
								if (decl.isStatic())
									imports.removeStaticImport(name);
								else
									imports.removeImport(name);
							}
							break;
						}
					}
				}
			}
		}
		imports.create(needsSave, null);
	}

	/** importsRewrite */
	private ImportRewrite importsRewrite;

	public ImportsManager(CompilationUnit astRoot) throws CoreException {
		importsRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
	}

	/**
	 * Adds a new import declaration that is sorted in the existing imports. If
	 * an import already exists or the import would conflict with an import of
	 * an other type with the same simple name, the import is not added.
	 * 
	 * @param typeBinding
	 *            the binding of the type to import
	 * 
	 * @return Returns the simple type name that can be used in the code or the
	 *         fully qualified type name if an import conflict prevented the
	 *         import.
	 */
	public String addImport(ITypeBinding typeBinding) {
		return importsRewrite.addImport(typeBinding);
	}

	/**
	 * Adds a new import declaration that is sorted in the existing imports. If
	 * an import already exists or the import would conflict with an import of
	 * an other type with the same simple name, the import is not added.
	 * 
	 * @param qualifiedTypeName
	 *            The fully qualified name of the type to import (dot
	 *            separated).
	 * @return Returns the simple type name that can be used in the code or the
	 *         fully qualified type name if an import conflict prevented the
	 *         import.
	 */
	public String addImport(String qualifiedTypeName) {
		return importsRewrite.addImport(qualifiedTypeName);
	}

	/**
	 * Adds a new import declaration for a static type that is sorted in the
	 * existing imports. If an import already exists or the import would
	 * conflict with an import of an other static import with the same simple
	 * name, the import is not added.
	 * 
	 * @param declaringTypeName
	 *            The qualified name of the static's member declaring type
	 * @param simpleName
	 *            the simple name of the member; either a field or a method
	 *            name.
	 * @param isField
	 *            <code>true</code> specifies that the member is a field,
	 *            <code>false</code> if it is a method.
	 * @return returns either the simple member name if the import was
	 *         successful or else the qualified name if an import conflict
	 *         prevented the import.
	 * 
	 * @since 3.2
	 */
	public String addStaticImport(String declaringTypeName, String simpleName, boolean isField) {
		return importsRewrite.addStaticImport(declaringTypeName, simpleName, isField);
	}

	/**
	 * Creates and applys the changes on the underlying compilation unit.
	 * 
	 * @param needsSave
	 * @param monitor
	 * @throws CoreException
	 */
	public void create(boolean needsSave, IProgressMonitor monitor) throws CoreException {
		TextEdit edit = importsRewrite.rewriteImports(monitor);
		JavaModelUtil.applyEdit(importsRewrite.getCompilationUnit(), edit, needsSave, null);
	}

	public ICompilationUnit getCompilationUnit() {
		return importsRewrite.getCompilationUnit();
	}

	/**
	 * Removes an import.
	 * 
	 * @param qualifiedName
	 * @see ImportRewrite#removeImport(String)
	 */
	public void removeImport(String qualifiedName) {
		importsRewrite.removeImport(qualifiedName);
	}

	/**
	 * Removes a static import
	 * 
	 * @param qualifiedName
	 * @see ImportRewrite#removeStaticImport(String)
	 */
	public void removeStaticImport(String qualifiedName) {
		importsRewrite.removeStaticImport(qualifiedName);
	}
}