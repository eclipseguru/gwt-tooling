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
package org.eclipseguru.gwt.core.internal.classpath;

import org.eclipseguru.gwt.core.GwtCore;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A util for generating access rules.
 */
public class AccessRulesUtil {

	/** ACCESSIBLE_JRE_TYPES_RESOURCE */
	private static final String ACCESSIBLE_JRE_TYPES_RESOURCE = "accessible-jre-types.properties";

	/** EXCLUDE_ALL_RULE */
	private static final IAccessRule EXCLUDE_ALL_RULE = JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER); //$NON-NLS-1$

	/** ACCESSIBLE_RULES */
	private static final Map<IPath, IAccessRule> ACCESSIBLE_RULES_CACHE = new HashMap<IPath, IAccessRule>();

	/** accessibleJRETypes */
	private static IPath[] accessibleJRETypes;

	/**
	 * Gets the defined accessible JRE types.
	 * 
	 * @return the defined accessible JRE types.
	 */
	private static synchronized IPath[] getAccessibleJRETypes() {
		if (null != accessibleJRETypes)
			return accessibleJRETypes;

		final Properties properties = new Properties();
		InputStream stream = null;
		try {
			stream = FileLocator.openStream(GwtCore.getGwtCore().getBundle(), new Path(ACCESSIBLE_JRE_TYPES_RESOURCE), false);
			if (null != stream) {
				properties.load(stream);
			}
		} catch (final IOException e) {
			GwtCore.logError("Error while reading JRE access rules", e);
		} finally {
			if (null != stream) {
				try {
					stream.close();
				} catch (final IOException e) {
					// ignore
				}
			}
		}

		final List<IPath> types = new ArrayList<IPath>();
		final Enumeration<?> names = properties.propertyNames();
		while (names.hasMoreElements()) {
			final String name = (String) names.nextElement();
			types.add(new Path(name.replace('.', '/')));
		}

		accessibleJRETypes = types.toArray(new IPath[types.size()]);
		return accessibleJRETypes;
	}

	private static synchronized IAccessRule getAccessibleRule(final IPath path) {
		IAccessRule rule = ACCESSIBLE_RULES_CACHE.get(path);
		if (rule == null) {
			rule = JavaCore.newAccessRule(path, IAccessRule.K_ACCESSIBLE);
			ACCESSIBLE_RULES_CACHE.put(path, rule);
		}
		return rule;
	}

	public static IAccessRule[] getJREAccessRules() {
		final IPath[] types = getAccessibleJRETypes();
		final IAccessRule[] accessRules = new IAccessRule[types.length + 1];
		for (int i = 0; i < types.length; i++) {
			final IPath typePath = types[i];
			accessRules[i] = getAccessibleRule(typePath);
		}
		accessRules[types.length] = EXCLUDE_ALL_RULE;
		return accessRules;

	}

	/**
	 * No need to instanciate.
	 */
	private AccessRulesUtil() {
		// empty
	}
}
