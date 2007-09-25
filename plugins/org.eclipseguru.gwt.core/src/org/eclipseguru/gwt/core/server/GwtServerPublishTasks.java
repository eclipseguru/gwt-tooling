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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtProject;

/**
 * GWT publishing needs.
 */
public class GwtServerPublishTasks extends PublishTaskDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.PublishTaskDelegate#getTasks(org.eclipse.wst.server.core.IServer,
	 *      java.util.List)
	 */
	@Override
	public PublishOperation[] getTasks(IServer server, List modules) {
		if (modules == null)
			return null;

		List<PublishOperation> tasks = new ArrayList<PublishOperation>();
		int size = modules.size();
		for (int i = 0; i < size; i++) {
			IModule[] module = (IModule[]) modules.get(i);
			IModule m = module[module.length - 1];
			IWebModule webModule = (IWebModule) m.loadAdapter(IWebModule.class, null);
			if ((webModule != null) && GwtProject.hasGwtNature(m.getProject())) {
				GwtProject project = GwtCore.create(m.getProject());
				if (null != project) {
					GwtProjectServerPublishOperation task = new GwtProjectServerPublishOperation(webModule, project, server, PublishOperation.PREFERRED);
					tasks.add(task);
				}
			}
		}
		return tasks.toArray(new PublishOperation[tasks.size()]);
	}

}
