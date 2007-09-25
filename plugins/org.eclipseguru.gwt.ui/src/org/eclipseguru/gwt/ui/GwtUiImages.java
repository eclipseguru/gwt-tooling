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
package org.eclipseguru.gwt.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

/**
 * Bundle of all images used by the PDE plugin.
 */
public class GwtUiImages {

	private static final String NAME_PREFIX = GwtUi.PLUGIN_ID + "."; //$NON-NLS-1$

	private static ImageRegistry PLUGIN_REGISTRY;

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	/**
	 * Set of predefined Image Descriptors.
	 */

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr8/"; //$NON-NLS-1$

	/**
	 * Frequently used images
	 */
	public static final String IMG_INTERFACE = NAME_PREFIX + "IMG_INTERFACE"; //$NON-NLS-1$
	public static final String IMG_CLASS = NAME_PREFIX + "IMG_CLASS"; //$NON-NLS-1$
	public static final String IMG_PACKAGE = NAME_PREFIX + "IMG_PACKAGE"; //$NON-NLS-1$
	public static final String IMG_MODULE = NAME_PREFIX + "IMG_MODULE"; //$NON-NLS-1$

	public static final ImageDescriptor DESC_INTERFACE = create(PATH_OBJ, "obj_interface.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLASS = create(PATH_OBJ, "obj_class.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PACKAGE = create(PATH_OBJ, "obj_package.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_MODULE = create(PATH_OBJ, "obj_module.png"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_OVERLAY_MODULE = create(PATH_OVR, "ovr_module.png"); //$NON-NLS-1$

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeImageURL(prefix, name));
	}

	public static Image get(String key) {
		if (PLUGIN_REGISTRY == null)
			initialize();
		return PLUGIN_REGISTRY.get(key);
	}

	/* package */
	private static final void initialize() {
		PLUGIN_REGISTRY = new ImageRegistry(PlatformUI.getWorkbench().getDisplay());
		manage(IMG_INTERFACE, DESC_INTERFACE);
		manage(IMG_CLASS, DESC_CLASS);
		manage(IMG_PACKAGE, DESC_PACKAGE);
		manage(IMG_MODULE, DESC_MODULE);
	}

	private static URL makeImageURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return FileLocator.find(GwtUi.getPlugin().getBundle(), new Path(path), null);
	}

	public static Image manage(String key, ImageDescriptor desc) {
		Image image = desc.createImage();
		PLUGIN_REGISTRY.put(key, image);
		return image;
	}
}
