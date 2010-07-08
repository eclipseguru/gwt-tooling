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
 *     dobesv - contributed patch for issue 58
 *******************************************************************************/
package org.eclipseguru.gwt.core;

import org.eclipseguru.gwt.core.internal.codegen.AsyncServiceCodeGenerator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * A GWT remote services
 */
public class GwtRemoteService extends GwtElement {

	/** REMOTE_SERVICE_CLASS_SIMPLE_NAME */
	static final String[] REMOTE_SERVICE_CLASS_SIMPLE_NAMES = new String[] { "RemoteService", "RpcService" };

	/**
	 * Finds all interfaces in the specified modules which implements the GWT
	 * <code>RemoteService</code> or <code>RpcService</code> interface.
	 * 
	 * @param projectModules
	 * @return a list of all found interfaces
	 * @throws CoreException
	 */
	public static List<IType> findRemoteServices(final GwtModule[] projectModules) throws CoreException {
		final List<IType> remoteServiceFiles = new ArrayList<IType>();
		for (final GwtModule module : projectModules) {
			// skip modules without a java package
			if ((null == module.getModulePackage()) || !module.getModulePackage().exists()) {
				continue;
			}

			// get the "client" folder
			for (final String sourcePath : module.getSourcePaths()) {
				final IFolder folder = ((IFolder) module.getModulePackage().getResource()).getFolder(sourcePath);
				if (!folder.exists()) {
					continue;
				}

				// check for service definitions
				folder.accept(new IResourceProxyVisitor() {
					public boolean visit(final IResourceProxy proxy) throws CoreException {
						switch (proxy.getType()) {
							case IResource.FOLDER:
								return true;

							case IResource.FILE:
								if (!JavaCore.isJavaLikeFileName(proxy.getName())) {
									return false;
								}

								final IFile file = (IFile) proxy.requestResource();
								if (!module.isModuleResource(file)) {
									return false;
								}

								final ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
								if ((null != cu) && module.getModulePackage().getJavaProject().isOnClasspath(cu)) {
									GwtRemoteService.findRemoteServices(cu, remoteServiceFiles);
								}
						}
						return false;
					}
				}, IResource.DEPTH_INFINITE);
			}
		}
		return remoteServiceFiles;
	}

	/**
	 * Finds remote services in the specified compilation unit which implement
	 * the GWT <code>RemoteService</code> or <code>RpcService</code> interface.
	 * 
	 * @param cu
	 * @param remoteServiceFiles
	 * @throws JavaModelException
	 */
	public static void findRemoteServices(final ICompilationUnit cu, final List<IType> remoteServiceFiles) throws JavaModelException {
		// for every type declared in the java file
		for (final IType someType : cu.getTypes()) {
			// ignore binary types and non-interfaces
			if (!someType.isInterface() || someType.isBinary()) {
				continue;
			}
			// for every interface implemented by that type
			for (final String aSuperInterfaceSignature : someType.getSuperInterfaceTypeSignatures()) {
				final String simpleName = GwtUtil.getTypeNameWithoutParameters(Signature.getSignatureSimpleName(aSuperInterfaceSignature));
				for (final String remoteServiceInterfaceName : REMOTE_SERVICE_CLASS_SIMPLE_NAMES) {
					if (simpleName.equals(remoteServiceInterfaceName) && !remoteServiceFiles.contains(someType)) {
						remoteServiceFiles.add(someType);
					}
				}
			}
		}
	}

	/**
	 * Indicates if the specified type is a remote service, i.e. implements the
	 * the GWT <code>RemoteService</code> or <code>RpcService</code> interface.
	 * 
	 * @param someType
	 *            a type
	 * @return <code>true</code> if the specified type implements the GWT
	 *         <code>RemoteService</code> or <code>RpcService</code> interface,
	 *         <code>false</code> otherwise
	 * @throws JavaModelException
	 */
	public static boolean isRemoteService(final IType someType) throws JavaModelException {
		// ignore non-interfaces
		if (!someType.isInterface()) {
			return false;
		}

		// for every interface implemented by that type
		for (final String aSuperInterfaceSignature : someType.getSuperInterfaceTypeSignatures()) {
			final String simpleName = GwtUtil.getTypeNameWithoutParameters(Signature.getSignatureSimpleName(aSuperInterfaceSignature));
			for (final String remoteServiceInterfaceName : REMOTE_SERVICE_CLASS_SIMPLE_NAMES) {
				if (simpleName.equals(remoteServiceInterfaceName)) {
					return true;
				}
			}
		}

		return false;
	}

	/** type */
	private final IType type;

	/** asyncType */
	private IType asyncType;

	/**
	 * Creates a new instance
	 * 
	 * @param type
	 * @param parent
	 */
	GwtRemoteService(final IType type, final GwtModule parent) {
		super(parent);
		this.type = type;
	}

	/**
	 * Returns the {@link IType type} of the async stub.
	 * 
	 * @return the {@link IType type} of the async stub
	 */
	public IType getAsyncStubType() {
		if (null == asyncType) {
			final String asyncTypeName = getAsyncStubTypeName();
			final String asyncCUName = AsyncServiceCodeGenerator.getAsyncCUName(type);
			asyncType = type.getPackageFragment().getCompilationUnit(asyncCUName).getType(asyncTypeName);
		}
		return asyncType;
	}

	/**
	 * Returns the simple, unqualified name of the async service stub.
	 * 
	 * @return the simple, unqualified name of the async service stub
	 */
	public String getAsyncStubTypeName() {
		return AsyncServiceCodeGenerator.getAsyncTypeNameWithoutParameters(type);
	}

	/**
	 * Returns the underlying {@link IType type}.
	 * 
	 * @return the underlying {@link IType type}
	 */
	public IType getJavaType() {
		return type;
	}

	/**
	 * Returns the simple name of the remote service unqualified by package or
	 * enclosing type name.
	 * 
	 * @return the simple type name.
	 */
	@Override
	public String getName() {
		return type.getElementName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipseguru.gwt.core.GwtElement#getType()
	 */
	@Override
	public int getType() {
		return GWT_REMOTE_SERVICE;
	}
}
