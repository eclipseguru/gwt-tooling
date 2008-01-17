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
package org.eclipseguru.gwt.core.j2ee;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.Servlet;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ArtifactEdit;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.internal.util.ArtifactEditRegistryReader;
import org.eclipse.wst.common.componentcore.internal.util.IArtifactEditFactory;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipseguru.gwt.core.GwtModule;
import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

/**
 * Job for configuring a WTP web project to GWT specific needs.
 */
@SuppressWarnings("restriction")
public class ConfigureWebProjectJob extends WorkspaceJob {

	/** deploymentPath */
	private final IPath deploymentPath;

	/** outputPath */
	private final IPath outputPath;

	/** isHostedDeploymentMode */
	private final boolean isHostedDeploymentMode;

	/** project */
	private final GwtProject project;

	/**
	 * Creates a new instance using the configured project values.
	 * 
	 * @param project2
	 */
	public ConfigureWebProjectJob(GwtProject project) {
		this(project, GwtUtil.getOutputLocation(project), GwtUtil.getDeploymentPath(project), GwtUtil.isHostedDeploymentMode(project));
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param project
	 * @param buildOutputPath
	 * @param deploymentPath
	 * @param isHostedDeploymentMode
	 */
	public ConfigureWebProjectJob(GwtProject project, IPath buildOutputPath, IPath deploymentPath, boolean isHostedDeploymentMode) {
		super("Configuring Web Project " + project.getName());
		this.deploymentPath = deploymentPath;
		this.outputPath = buildOutputPath;
		this.isHostedDeploymentMode = isHostedDeploymentMode;
		this.project = project;

		// configure job
		setRule(ResourcesPlugin.getWorkspace().getRoot());
		setPriority(SHORT);
	}

	/**
	 * Adds gwt-hosted.html servlet
	 * 
	 * @param webAppEdit
	 */
	private void addGwtHostedServlet(WebArtifactEdit webAppEdit) {
		// remove existing definition
		Servlet gwtHostedServlet = webAppEdit.getWebApp().getServletNamed("gwt-hosted");
		if (null != gwtHostedServlet) {
			// remove mappings
			Object[] mappings = gwtHostedServlet.getMappings().toArray();
			for (int i = 0; i < mappings.length; i++)
				webAppEdit.getWebApp().getServletMappings().remove(mappings[i]);
			// remove servlet
			webAppEdit.getWebApp().getServlets().remove(gwtHostedServlet);
		}

		// create servlet edit
		ServletEditUtil servletEdit = new ServletEditUtil(webAppEdit);

		// register "gwt-hosted.jsp" as servlet
		gwtHostedServlet = servletEdit.createServlet(deploymentPath.append("gwt-hosted.jsp").toString(), false, "gwt-hosted", "gwt-hosted", "Necessary for GWT hosted mode.");

		// create servlet mapping
		servletEdit.createURLMappings(gwtHostedServlet, deploymentPath.append("gwt-hosted.html").toString());
	}

	/**
	 * @param flexProject
	 * @param monitor
	 */
	private void addModuleJars(final IVirtualComponent flexProject, IProgressMonitor monitor) {
		try {
			monitor.beginTask("Updatings Jars...", 4);

			// the jar location
			final IPath gwtUserJarPath = new Path("GWT_HOME").append("gwt-user.jar");

			// get current references
			boolean foundGwtUserJar = false;
			boolean changedReferences = false;
			List<IVirtualReference> references = new ArrayList<IVirtualReference>();
			IVirtualReference[] currentReferences = flexProject.getReferences();
			for (IVirtualReference reference : currentReferences) {
				ProgressUtil.checkCanceled(monitor);
				references.add(reference);
				IVirtualComponent referencedComponent = reference.getReferencedComponent();
				String deployedName = referencedComponent.getDeployedName();
				if ((null != deployedName) && !foundGwtUserJar && deployedName.endsWith(gwtUserJarPath.toString()))
					foundGwtUserJar = true;
			}

			// gwt-user.jar
			if (!foundGwtUserJar) {
				String type = VirtualArchiveComponent.VARARCHIVETYPE + IPath.SEPARATOR;
				IVirtualComponent gwtUserJar = ComponentCore.createArchiveComponent(project.getProjectResource(), type + gwtUserJarPath.toString());
				IVirtualReference gwtUserJarReference = ComponentCore.createReference(flexProject, gwtUserJar, new Path("/WEB-INF/lib"));
				if (!references.contains(gwtUserJarReference)) {
					references.add(gwtUserJarReference);
					changedReferences = true;
				}
			}

			ProgressUtil.checkCanceled(monitor);

			// apply references
			if (changedReferences)
				flexProject.setReferences(references.toArray(new IVirtualReference[references.size()]));
		} finally {
			monitor.done();
		}
	}

	/**
	 * @param flexProject
	 * @param monitor
	 * @throws CoreException
	 */
	private void createDeploymentFolderForOutputFolder(final IVirtualComponent flexProject, IProgressMonitor monitor) throws CoreException {
		try {
			GwtModule[] modules = project.getModules();
			monitor.beginTask("Create Deployment Folder...", modules.length*2);
			for (int i = 0; i < modules.length; i++) {
				GwtModule gwtModule = modules[i];
				final IVirtualFolder gwtDeployFolder = flexProject.getRootFolder().getFolder(deploymentPath.append(gwtModule.getName()));
				// be aggressive: remove any old resource first
				if (gwtDeployFolder.exists())
					gwtDeployFolder.delete(IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
				else
					monitor.worked(1);
				if(gwtModule.getEntryPointTypeName() != null)
					gwtDeployFolder.createLink(outputPath.append(gwtModule.getName()), IResource.FORCE, ProgressUtil.subProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(NLS.bind("Configuring Web Project {0}", project.getName()), 10);

			// get the flex project component
			final IVirtualComponent flexProject = ComponentCore.createComponent(project.getProjectResource());
			if (!flexProject.exists())
				flexProject.create(IResource.FORCE, null);

			ProgressUtil.checkCanceled(monitor);

			// link the deployment folder to the build output folder
			createDeploymentFolderForOutputFolder(flexProject, ProgressUtil.subProgressMonitor(monitor, 1));

			ProgressUtil.checkCanceled(monitor);

			// add necessary jar files
			// TODO: I think this is not necessary
			if (false)
				addModuleJars(flexProject, ProgressUtil.subProgressMonitor(monitor, 1));

			// update the deployment desciptor
			// TODO: not necessary with GWT 1.1
			if (false)
				updateDeploymentDescriptor(flexProject, ProgressUtil.subProgressMonitor(monitor, 1));

		} finally {
			monitor.done();
		}

		return Status.OK_STATUS;
	}

	/**
	 * Updates the deployment desciptor.
	 * 
	 * @param flexProject
	 * @param monitor
	 */
	private void updateDeploymentDescriptor(final IVirtualComponent flexProject, IProgressMonitor monitor) {
		ArtifactEdit edit = null;
		try {
			monitor.beginTask("Updating web.xml...", 2);

			if (!isHostedDeploymentMode)
				return;

			// get factory
			IArtifactEditFactory artifactEditFactory = ArtifactEditRegistryReader.instance().getArtifactEdit(project.getProjectResource());
			if (null == artifactEditFactory)
				return;

			// create edit
			edit = artifactEditFactory.createArtifactEditForWrite(flexProject);
			if (null == edit)
				return;

			// adapt to web edit
			WebArtifactEdit webAppEdit = (WebArtifactEdit) edit.getAdapter(WebArtifactEdit.class);
			if (null == webAppEdit)
				return;

			// in add gwt-hosted.html servlet in hosted mode
			if (false)// (isHostedDeploymentMode)
				addGwtHostedServlet(webAppEdit);
			monitor.worked(1);

			// save descriptor
			webAppEdit.saveIfNecessary(ProgressUtil.subProgressMonitor(monitor, 1));
		} finally {
			if (null != edit)
				edit.dispose();
			monitor.done();
		}
	}
}