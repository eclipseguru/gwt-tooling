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
package org.eclipseguru.gwt.core.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipseguru.gwt.core.GwtCore;
import org.osgi.framework.Bundle;

/**
 * Some utility functions for dealing with <code>{@link IResource}</code>s.
 * 
 * @author Eclipse Guru (eclipseguru@gmail.com)
 */
public class ResourceUtil {

	/**
	 * Copies the content of the specified folder to the target path. Deletes
	 * any existing resource before copying the content.
	 * 
	 * @param folder
	 * @param target
	 * @param monitor
	 * @throws CoreException
	 */
	public static void copyFolderContent(IFolder folder, IPath target, IProgressMonitor monitor) throws CoreException {
		if (!folder.isAccessible())
			return;
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource[] resources = folder.members();
			monitor.beginTask(NLS.bind("Copying folder {0}", folder.getFullPath()), resources.length);
			IPath sourcePath = folder.getFullPath();
			for (IResource toCopy : resources) {
				IPath relativePath = toCopy.getFullPath().removeFirstSegments(sourcePath.segmentCount());
				IResource existing = root.findMember(target.append(relativePath));
				if (null != existing)
					existing.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
				else
					monitor.worked(1);
				toCopy.copy(target.append(relativePath), IResource.FORCE | IResource.DERIVED, ProgressUtil.subProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Copies a single resource. Deletes a target resource first.
	 * 
	 * @param toCopy
	 * @param target
	 * @param monitor
	 * @throws CoreException
	 */
	public static void copyResource(IResource toCopy, IPath target, IProgressMonitor monitor) throws CoreException {
		if (!toCopy.isAccessible())
			return;
		try {
			monitor.beginTask(NLS.bind("Copying {0}", toCopy.getName()), 2);
			IResource existing = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
			if (null != existing)
				existing.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			toCopy.copy(target, IResource.FORCE | IResource.DERIVED, ProgressUtil.subProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Copies the support file with the specified name from the bundle to the
	 * target folder.
	 * 
	 * @param source
	 * @param fileName
	 * @param targetFolder
	 * @param monitor
	 * @throws IOException
	 * @throws CoreException
	 */
	public static void copySupportFile(Bundle source, String fileName, IFolder targetFolder, IProgressMonitor monitor) throws IOException, CoreException {
		IFile file = targetFolder.getFile(fileName);
		if (!file.exists()) {
			URL fileUrl = source.getEntry("/supportfiles/" + fileName);
			BufferedInputStream stream = new BufferedInputStream(fileUrl.openStream());
			file.create(stream, IResource.FORCE | IResource.DERIVED, monitor);
		}
	}

	/**
	 * Recursively creates a folder hierarchy.
	 * 
	 * @param folder
	 * @param monitor
	 * @throws CoreException
	 */
	public static void createFolderHierarchy(IFolder folder, IProgressMonitor monitor) throws CoreException {
		while (!folder.exists())
			try {
				monitor.beginTask(NLS.bind("Creating folder {0}", folder.getFullPath().toString()), 2);
				IContainer parent = folder.getParent();
				if (!parent.exists())
					if (parent.getType() == IResource.FOLDER)
						createFolderHierarchy((IFolder) parent, ProgressUtil.subProgressMonitor(monitor, 1));
				folder.create(IResource.FORCE, true, ProgressUtil.subProgressMonitor(monitor, 1));
			} finally {
				monitor.done();
			}
	}

	/**
	 * Creates a problem marker.
	 * 
	 * @param resource
	 * @param message
	 * @throws CoreException
	 */
	public static IMarker createProblem(IResource resource, String message) throws CoreException {
		if (null == resource)
			resource = ResourcesPlugin.getWorkspace().getRoot();
		IMarker marker = resource.createMarker(GwtCore.PROBLEM_MARKER);
		Map<String, Object> attributes = new HashMap<String, Object>(2);
		attributes.put(IMarker.MESSAGE, message);
		attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttributes(attributes);
		return marker;
	}

	/**
	 * Indicates if a resource is read only.
	 * 
	 * @param resource
	 * @return
	 */
	public static boolean isReadOnly(IResource resource) {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes == null) // not supported on this platform for
			// this resource
			return false;
		return resourceAttributes.isReadOnly();
	}

	/**
	 * Removes the content of the specified folder.
	 * 
	 * @param folder
	 * @param monitor
	 * @throws CoreException
	 */
	public static void removeFolderContent(IFolder folder, IProgressMonitor monitor) throws CoreException {
		if (!folder.isAccessible())
			return;
		try {
			IResource[] resources = folder.members();
			monitor.beginTask(NLS.bind("Cleaning folder {0}", folder.getFullPath()), resources.length);
			for (IResource toDelete : resources)
				toDelete.delete(true, ProgressUtil.subProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
	}

}
