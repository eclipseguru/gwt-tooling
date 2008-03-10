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
package org.eclipseguru.gwt.core.utils;

import org.eclipseguru.gwt.core.GwtCore;

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
import org.osgi.framework.Bundle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
	public static void copyFolderContent(final IFolder folder, final IPath target, final IProgressMonitor monitor) throws CoreException {
		if (!folder.isAccessible())
			return;
		try {
			final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			final IResource[] resources = folder.members();
			monitor.beginTask(NLS.bind("Copying folder {0}", folder.getFullPath()), resources.length);
			final IPath sourcePath = folder.getFullPath();
			for (final IResource toCopy : resources) {
				final IPath relativePath = toCopy.getFullPath().removeFirstSegments(sourcePath.segmentCount());
				final IResource existing = root.findMember(target.append(relativePath));
				if (null != existing) {
					existing.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
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
	public static void copyResource(final IResource toCopy, final IPath target, final IProgressMonitor monitor) throws CoreException {
		if (!toCopy.isAccessible())
			return;
		try {
			monitor.beginTask(NLS.bind("Copying {0}", toCopy.getName()), 2);
			final IResource existing = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
			if (null != existing) {
				existing.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			}
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
	public static void copySupportFile(final Bundle source, final String fileName, final IFolder targetFolder, final IProgressMonitor monitor) throws IOException, CoreException {
		final IFile file = targetFolder.getFile(fileName);
		if (!file.exists()) {
			final URL fileUrl = source.getEntry("/supportfiles/" + fileName);
			final BufferedInputStream stream = new BufferedInputStream(fileUrl.openStream());
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
	public static void createFolderHierarchy(final IFolder folder, final IProgressMonitor monitor) throws CoreException {
		while (!folder.exists()) {
			try {
				monitor.beginTask(NLS.bind("Creating folder {0}", folder.getFullPath().toString()), 2);
				final IContainer parent = folder.getParent();
				if (!parent.exists())
					if (parent.getType() == IResource.FOLDER) {
						createFolderHierarchy((IFolder) parent, ProgressUtil.subProgressMonitor(monitor, 1));
					}
				folder.create(IResource.FORCE, true, ProgressUtil.subProgressMonitor(monitor, 1));
			} finally {
				monitor.done();
			}
		}
	}

	/**
	 * Creates a problem marker.
	 * 
	 * @param resource
	 * @param message
	 * @throws CoreException
	 */
	public static IMarker createProblem(IResource resource, final String message) throws CoreException {
		if (null == resource) {
			resource = ResourcesPlugin.getWorkspace().getRoot();
		}
		final IMarker marker = resource.createMarker(GwtCore.PROBLEM_MARKER);
		final Map<String, Object> attributes = new HashMap<String, Object>(2);
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
	public static boolean isReadOnly(final IResource resource) {
		final ResourceAttributes resourceAttributes = resource.getResourceAttributes();
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
	public static void removeFolderContent(final IFolder folder, final IProgressMonitor monitor) throws CoreException {
		if (!folder.isAccessible())
			return;
		try {
			final IResource[] resources = folder.members();
			monitor.beginTask(NLS.bind("Cleaning folder {0}", folder.getFullPath()), resources.length);
			for (final IResource toDelete : resources) {
				toDelete.delete(true, ProgressUtil.subProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

}
