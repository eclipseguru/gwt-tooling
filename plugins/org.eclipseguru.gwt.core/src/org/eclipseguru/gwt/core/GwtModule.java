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
package org.eclipseguru.gwt.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import com.googlipse.gwt.common.Constants;

/**
 * A GWT Module
 */
public class GwtModule extends GwtElement {

	/** ENTRY_POINT_TYPE */
	public static final String ENTRY_POINT_TYPE = "com.google.gwt.core.client.EntryPoint";

	/**
	 * Indicates if the specified type is an entry point, i.e. implements the
	 * <code>{@value #ENTRY_POINT_TYPE}</code> interface.
	 * 
	 * @param someType
	 *            a type
	 * @return <code>true</code> if the specified type implements the
	 *         <code>{@value #ENTRY_POINT_TYPE}</code> interface,
	 *         <code>false</code> otherwise
	 * @throws JavaModelException
	 */
	public static boolean isEntryPoint(final IType someType) throws JavaModelException {
		// ignore non-classes
		if (!someType.isClass())
			return false;

		// for every interface implemented by that type
		final IType[] stypes = someType.newSupertypeHierarchy(null).getAllSuperInterfaces(someType);
		for (final IType element : stypes)
			if (element.getFullyQualifiedName().equals(ENTRY_POINT_TYPE))
				return true;

		return false;
	}

	/** moduleId */
	private final String moduleId;

	/** moduleDescriptor */
	private final IStorage moduleDescriptor;

	/** modulePackage */
	private final IPackageFragment modulePackage;

	/** entryPointType */
	private IType entryPointType;

	/** entryPointTypeName */
	private String entryPointTypeName;

	/** inheritedModules */
	private GwtModule[] inheritedModules;

	/** moduleSourceInfo */
	private GwtModuleSourceHandler moduleSourceInfo;

	/**
	 * Creates a new module from a file.
	 * 
	 * @param moduleDescriptor
	 * @param parent
	 */
	GwtModule(final IFile moduleDescriptor, final GwtProject parent) {
		super(parent);

		if (!GwtUtil.isModuleDescriptor(moduleDescriptor)) {
			throw new IllegalArgumentException("Module descriptor is invalid");
		}

		this.moduleDescriptor = moduleDescriptor;

		// module package
		final IJavaElement element = JavaCore.create(moduleDescriptor.getParent());
		if (null != element) {
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					modulePackage = ((IPackageFragmentRoot) element).getPackageFragment("");
					break;
				case IJavaElement.PACKAGE_FRAGMENT:
					modulePackage = (IPackageFragment) element;
					break;
				default:
					modulePackage = null;
					break;
			}
		} else {
			modulePackage = null;
		}

		// the module id
		final StringBuilder moduleIdBuilder = new StringBuilder();
		if (null != modulePackage) {
			moduleIdBuilder.append(modulePackage.getElementName());
			if (moduleIdBuilder.length() > 0) {
				moduleIdBuilder.append('.');
			}
			moduleIdBuilder.append(moduleDescriptor.getName().substring(0, moduleDescriptor.getName().length() - Constants.GWT_XML_EXT.length() - 1));
		} else {
			final String path = moduleDescriptor.getFullPath().makeRelative().toString();
			moduleIdBuilder.append(path.substring(0, path.length() - GwtUtil.GWT_MODULE_SOURCE_EXTENSION.length()).replace('/', '.'));
		}
		moduleId = moduleIdBuilder.toString();
	}

	/**
	 * Creates a binary module.
	 * 
	 * @param moduleDescriptor
	 * @param packageFragment
	 * @param parent
	 */
	GwtModule(final IStorage moduleDescriptor, final IPackageFragment packageFragment, final GwtProject parent) {
		super(parent);

		if (null == moduleDescriptor) {
			throw new IllegalArgumentException("Module descriptor cannot be null");
		}

		if (null == packageFragment) {
			throw new IllegalArgumentException("Package fragment cannot be null");
		}

		final String simpleName = GwtUtil.getSimpleName(moduleDescriptor);
		if (null == simpleName) {
			throw new IllegalArgumentException("Invalid storage name");
		}

		if (packageFragment.isDefaultPackage()) {
			moduleId = GwtUtil.getSimpleName(moduleDescriptor);
		} else {
			moduleId = packageFragment.getElementName().concat(".").concat(simpleName);
		}
		modulePackage = packageFragment;
		this.moduleDescriptor = moduleDescriptor;
	}

	/**
	 * Creates a GWT remote service for the specified type.
	 * 
	 * @param type
	 * @return
	 */
	/* package */GwtRemoteService createRemoteService(final IType type) {
		// TODO Auto-generated method stub
		return new GwtRemoteService(type, this);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!GwtModule.class.isAssignableFrom(obj.getClass()))
			return false;
		final GwtModule other = (GwtModule) obj;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		return true;
	}

	/**
	 * Finds the entry point type.
	 * 
	 * @return the entry point type (maybe <code>null</code>)
	 * @throws GwtModelException
	 */
	private synchronized IType findEntryPointType() throws GwtModelException {
		// check if already set
		if (null != entryPointType)
			return entryPointType;

		// get entry point type name
		final String entryPointClass = getEntryPointTypeName();
		if (null == entryPointClass)
			return null;

		try {
			return getProject().getJavaProject().findType(entryPointClass);
		} catch (final JavaModelException e) {
			throw new GwtModelException(e.getStatus());
		}
	}

	/**
	 * Finds the entry point type name.
	 * 
	 * @return the entry point type name (maybe <code>null</code>)
	 * @throws GwtModelException
	 */
	private synchronized String findEntryPointTypeName() throws GwtModelException {
		// check if already set
		if (null != entryPointTypeName)
			return entryPointTypeName;

		// get type name from module source
		return getModuleSourceInfo().getEntryPointClass();
	}

	/**
	 * Finds the modules inherited by this module.
	 * 
	 * @return
	 * @throws CoreException
	 */
	private synchronized GwtModule[] findInheritedModules() throws GwtModelException {
		if (null != inheritedModules)
			return inheritedModules;

		final GwtModuleSourceHandler info = getModuleSourceInfo();

		// read all inherited module ids
		final String[] inheritedModuleIds = info.getInheritedModules();

		// resolve modules
		final GwtModule[] resolvedModules = GwtModelManager.findModules(inheritedModuleIds, getProject());
		return null != resolvedModules ? resolvedModules : GwtModelManager.NO_MODULES;
	}

	/**
	 * Returns the entry point type.
	 * 
	 * @return the entry point type (maybe <code>null</code>)
	 * @throws GwtModelException
	 */
	public IType getEntryPointType() throws GwtModelException {
		if (null == entryPointType) {
			entryPointType = findEntryPointType();
		}

		return entryPointType;
	}

	/**
	 * Returns the entry point type name.
	 * 
	 * @return the entry point type name (maybe <code>null</code>)
	 * @throws GwtModelException
	 */
	public String getEntryPointTypeName() throws GwtModelException {
		if (null == entryPointTypeName) {
			entryPointTypeName = findEntryPointTypeName();
		}

		return entryPointTypeName;
	}

	/**
	 * Returns the list of modules directly inherited my this module.
	 * 
	 * @return the inheritedModules
	 * @throws CoreException
	 */
	public GwtModule[] getInheritedModules() throws CoreException {
		if (null == inheritedModules) {
			inheritedModules = findInheritedModules();
		}

		return inheritedModules;
	}

	/**
	 * Returns the module descriptor.
	 * 
	 * @return the moduleDescriptor
	 */
	public IStorage getModuleDescriptor() {
		return moduleDescriptor;
	}

	/**
	 * Returns the module id.
	 * 
	 * @return the module id
	 */
	public String getModuleId() {
		return moduleId;
	}

	/**
	 * Returns the module package. <code>null</code> is returned if the module
	 * is not on the classpath.
	 * 
	 * @return the modulePackage (maybe <code>null</code>)
	 */
	public IPackageFragment getModulePackage() {
		return modulePackage;
	}

	/**
	 * Returns (creates if necessary) the module source info.
	 * 
	 * @return the module source info
	 * @throws GwtModelException
	 */
	private synchronized GwtModuleSourceHandler getModuleSourceInfo() throws GwtModelException {
		if (null == moduleSourceInfo) {
			moduleSourceInfo = new GwtModuleSourceHandler();
			InputStream contents = null;
			try {
				contents = getModuleDescriptor().getContents();
				moduleSourceInfo.parseContents(new InputSource(contents));
			} catch (final IOException e) {
				throw new GwtModelException(GwtCore.newErrorStatus("Error while parsing module source", e));
			} catch (final ParserConfigurationException e) {
				final String message = "Internal Error: XML parser configuration error during pasing module source file."; //$NON-NLS-1$
				GwtCore.logError(message, e);
				throw new RuntimeException(message, e);
			} catch (final SAXException e) {
				throw new GwtModelException(GwtCore.newErrorStatus("Error while parsing module source", e));
			} catch (final CoreException e) {
				throw new GwtModelException(e.getStatus());
			} finally {
				if (null != contents) {
					try {
						contents.close();
					} catch (final IOException e) {
						// ignore
					}
				}
			}
		}
		return moduleSourceInfo;
	}

	/**
	 * Returns the module id.
	 * 
	 * @return the module id
	 */
	@Override
	public String getName() {
		return moduleId;
	}

	/**
	 * Returns the project.
	 * 
	 * @return the project
	 */
	public GwtProject getProject() {
		return (GwtProject) getParent();
	}

	/**
	 * Returns the project name.
	 * 
	 * @return the project name
	 */
	public String getProjectName() {
		return getProject().getName();
	}

	/**
	 * Returns the project resource.
	 * 
	 * @return the project resource
	 */
	public IProject getProjectResource() {
		return getProject().getProjectResource();
	}

	/**
	 * Returns the simple module name, i.e. the last part of the module id.
	 * 
	 * @return the simple module name
	 */
	public String getSimpleName() {
		return GwtUtil.getSimpleName(moduleId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipseguru.gwt.core.GwtElement#getType()
	 */
	@Override
	public int getType() {
		return GWT_MODULE;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		return result;
	}

	/**
	 * Indicates if this module is a binary module.
	 * 
	 * @return <code>true</code> if this module is a binary module
	 */
	public boolean isBinary() {
		return !(moduleDescriptor instanceof IFile);
	}

	/**
	 * Indicates if the specified path points to a resource within this module.
	 * <p>
	 * A path is considered to be with a module if it points to any resouce
	 * somewhere inside the client, server or public directory or one of their
	 * subdirectories. Note that the resource specified by the path does not
	 * need to exist.
	 * </p>
	 * 
	 * @param fullPath
	 *            the full, absolute path rooted at the workspace root
	 * @return <code>true</code> if the path points to a resource within the
	 *         module, <code>false</code> otherwise
	 */
	public boolean isModulePath(final IPath fullPath) {
		final IPath moduleRoot = isBinary() ? modulePackage.getPath() : ((IFile) moduleDescriptor).getParent().getFullPath();

		// client folder
		if (moduleRoot.append(Constants.CLIENT_PACKAGE).isPrefixOf(fullPath))
			return true;

		// public folder
		else if (moduleRoot.append(Constants.PUBLIC_FOLDER).isPrefixOf(fullPath))
			return true;

		// server folder
		else if (moduleRoot.append(Constants.SERVER_PACKAGE).isPrefixOf(fullPath))
			return true;

		// TODO: support customizable folders specified in module descriptor

		return false;
	}

	/**
	 * Indicates if the specified resource is part of this module.
	 * <p>
	 * This method just calls <code>{@link #isModulePath(IPath)}</code>.
	 * </p>
	 * 
	 * @param resource
	 * @return <code>true</code> if the resource is a module resource,
	 *         <code>false</code> otherwise
	 * @see #isModulePath(IPath)
	 */
	public boolean isModuleResource(final IResource resource) {
		return isModulePath(resource.getFullPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GwtModule: " + moduleId;
	}
}
