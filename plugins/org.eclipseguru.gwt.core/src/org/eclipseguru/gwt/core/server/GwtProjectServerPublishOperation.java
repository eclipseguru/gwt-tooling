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
package org.eclipseguru.gwt.core.server;

import org.eclipseguru.gwt.core.GwtProject;
import org.eclipseguru.gwt.core.builder.GwtProjectPublisher;
import org.eclipseguru.gwt.core.j2ee.ConfigureWebProjectJob;
import org.eclipseguru.gwt.core.utils.ProgressUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.PublishOperation;

import java.text.MessageFormat;

/**
 * Publishes a GWT project during server publishing.
 */
@SuppressWarnings("restriction")
public class GwtProjectServerPublishOperation extends PublishOperation {

	/** webModule */
	private final IWebModule webModule;

	/** project */
	private final GwtProject project;

	/** kind */
	private final int kind;

	/** server */
	private final IServer server;

	/**
	 * @param webModule
	 * @param project
	 * @param server
	 * @param required
	 */
	public GwtProjectServerPublishOperation(final IWebModule webModule, final GwtProject project, final IServer server, final int kind) {
		super(MessageFormat.format("Publishing GWT modules of project {0}", project.getName()), "This operation compiles and publishes all GWT modules included in the project.");
		this.webModule = webModule;
		this.project = project;
		this.kind = kind;
		this.server = server;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.PublishOperation#execute(org.eclipse.core.runtime.IProgressMonitor,
	 *      org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public void execute(IProgressMonitor monitor, final IAdaptable info) throws CoreException {
		monitor = ProgressUtil.monitor(monitor);
		try {
			monitor.beginTask(MessageFormat.format("Publishing GWT modules of project {0}...", project.getName()), 2);

			// 1. configure the web project
			final ConfigureWebProjectJob configureWebProjectJob = new ConfigureWebProjectJob(project);
			configureWebProjectJob.runInWorkspace(ProgressUtil.subProgressMonitor(monitor, 1));

			// 2. publish project
			final GwtProjectPublisher publisher = new GwtProjectPublisher(project);
			publisher.runInWorkspace(ProgressUtil.subProgressMonitor(monitor, 1));

			// 3. clear WebTools cache
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=154451)
			((Server) server).getServerPublishInfo().clearCache();
			((Server) server).getServerPublishInfo().startCaching();

		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.PublishOperation#getKind()
	 */
	@Override
	public int getKind() {
		return kind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.PublishOperation#getOrder()
	 */
	@Override
	public int getOrder() {
		// make sure to run before the other publishing operations
		return -1;
	}

	/**
	 * @return the webModule
	 */
	public IWebModule getWebModule() {
		return webModule;
	}

}
