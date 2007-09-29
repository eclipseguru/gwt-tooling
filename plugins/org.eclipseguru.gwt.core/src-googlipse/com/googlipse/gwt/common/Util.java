/*
 * Copyright 2006 TG. (techieguy@gmail.com)
 * Copyright 2006,2007 Eclipse Guru (eclipseguru@gmail.com)
 * Copyright 2007 Hugo Garcia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.googlipse.gwt.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.GwtUtil;
import org.eclipseguru.gwt.core.facet.GwtFacetConstants;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;

/**
 * @author TG. (techieguy@gmail.com)
 * @author Eclipse Guru (eclipseguru@gmail.com)
 * @deprecated
 */
@Deprecated
public class Util {

	/** GWT_USER_JAR */
	private static final String GWT_USER_JAR = "gwt-user.jar";

	/** GWT_DEV_WINDOWS_JAR */
	private static final String GWT_DEV_WINDOWS_JAR = "gwt-dev-windows.jar";

	/** GWT_DEV_LINUX_JAR */
	private static final String GWT_DEV_LINUX_JAR = "gwt-dev-linux.jar";

	/** Constant value indicating if the current platform is Windows */
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public static IJavaProject[] filterGwtProjects(IJavaProject[] javaProjects) {

		List<IJavaProject> gwtProjects = new ArrayList<IJavaProject>(javaProjects.length);
		for (IJavaProject aProject : javaProjects)
			try {
				if (aProject.getProject().hasNature(GwtCore.NATURE_ID))
					gwtProjects.add(aProject);
			} catch (CoreException e) {
				GwtCore.logError("Error filtering projects", e);
			}

		return gwtProjects.toArray(new IJavaProject[gwtProjects.size()]);

	}

	public static List<IFile> findModuleDescriptors(IJavaProject javaProject) throws CoreException {
		List<IFile> moduleFiles = new ArrayList<IFile>();
		for (IPackageFragmentRoot aRoot : javaProject.getPackageFragmentRoots()) {
			// check only in source folders. Skip others
			if (aRoot.getKind() != IPackageFragmentRoot.K_SOURCE)
				continue;
			for (IJavaElement aPackage : aRoot.getChildren()) {
				// look only for packages. Skip others
				if (aPackage.getElementType() != IJavaElement.PACKAGE_FRAGMENT)
					continue;

				for (Object aResource : ((IPackageFragment) aPackage).getNonJavaResources()) {
					// look only files. Skip others
					if (!(aResource instanceof IFile))
						continue;

					IFile aFile = (IFile) aResource;
					if (isGwtModuleFile(aFile))
						moduleFiles.add(aFile);
				}
			}
		}
		return Collections.unmodifiableList(moduleFiles);
	}

	public static IStatus getErrorStatus(String errorMessage) {
		return new Status(IStatus.ERROR, Constants.PLUGIN_ID, IStatus.OK, errorMessage, null);
	}

	public static IPath getGwtDevLibPath() {
		return getGwtHomeClasspathVariable().append(WINDOWS ? GWT_DEV_WINDOWS_JAR : GWT_DEV_LINUX_JAR);
	}

	public static IPath getGwtHomeClasspathVariable() {
		return new Path(getGwtHomeClasspathVariableName());
	}

	/**
	 * @return
	 */
	public static String getGwtHomeClasspathVariableName() {
		return "GWT_HOME";
	}

	public static IJavaProject[] getGwtProjects() {

		IJavaProject[] gwtProjects = new IJavaProject[0];
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IJavaModel javaModel = JavaCore.create(root);
			gwtProjects = filterGwtProjects(javaModel.getJavaProjects());
		} catch (JavaModelException e) {
			GwtCore.logError("getGwtProjects", e);
		}
		return gwtProjects;
	}

	public static IPath getGwtUserLibPath() {
		return getGwtHomeClasspathVariable().append(GWT_USER_JAR);

	}

	public static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	public static String getQualifiedModuleName(IFile moduleFile) {
		if (!isGwtModuleFile(moduleFile))
			return null;

		IJavaElement element = JavaCore.create((IFolder) moduleFile.getParent());
		if ((null == element) || !((element.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) || (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)))
			return null;

		String qualifiedName = element.getElementName() + "." + moduleFile.getName();
		return qualifiedName.substring(0, qualifiedName.length() - Constants.GWT_XML_EXT.length() - 1);
	}

	public static IPath getResolvedGwtDevLibPath() {
		return getResolvedGwtHomePath().append(WINDOWS ? GWT_DEV_WINDOWS_JAR : GWT_DEV_LINUX_JAR);
	}

	public static IPath getResolvedGwtHomePath() {
		return Path.fromPortableString(GwtCore.getGwtCore().getPluginPreferences().getString(GwtCorePreferenceConstants.PREF_GWT_HOME));
	}

	public static IPath getResolvedGwtUserLibPath() {
		return getResolvedGwtHomePath().append(GWT_USER_JAR);

	}

	public static String getSimpleName(IFile file) {
		String simpleName = "";
		if (file != null) {
			simpleName = file.getName();
			int index = simpleName.indexOf(Constants.GWT_XML_EXT);
			simpleName = simpleName.substring(0, index - 1);
		}
		return simpleName;
	}

	public static String getTemplateContents(String templateName) throws IOException {
		URL template;
		String xhtmlTemplatePath = GwtCore.getGwtCore().getPluginPreferences().getString(GwtCorePreferenceConstants.PREF_CUSTOM_MODULE_TEMPLATE_PATH);
		if (templateName.equals("Module.html.template") & (xhtmlTemplatePath != ""))
			template = new URL("file://" + xhtmlTemplatePath);
		else
			template = GwtCore.getGwtCore().getBundle().getEntry("/templates/" + templateName);
		InputStream stream = null;
		try {
			stream = template.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

			String lineSeparator = GwtUtil.getLineSeparator(null);
			StringBuilder contents = new StringBuilder(5000);
			while (reader.ready())
				contents.append(reader.readLine()).append(lineSeparator);
			return contents.toString();
		} finally {
			if (null != stream)
				stream.close();
		}
	}

	/**
	 * Indicates if a project has the
	 * {@value GwtFacetConstants#FACET_ID_GWT_MODULE} facet assigned.
	 * 
	 * @param project
	 * @return <code>true</code> if a project has the
	 *         {@value GwtFacetConstants#FACET_ID_GWT_MODULE} facet assigned,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasGwtModuleFacet(IProject project) {
		try {
			return ProjectFacetsManager.create(project).hasProjectFacet(ProjectFacetsManager.getProjectFacet(GwtFacetConstants.FACET_ID_GWT_MODULE));
		} catch (CoreException e) {
			// fail gracefully
			return false;
		}
	}

	public static boolean hasGwtNature(IJavaProject javaProject) {

		boolean hasGwtNature = false;
		try {
			if (javaProject != null)
				hasGwtNature = hasGwtNature(javaProject.getProject());
		} catch (Exception e) {
			GwtCore.logError(e.getMessage(), e);
		}
		return hasGwtNature;
	}

	/**
	 * Indicates if the project is a valid Googlipse project, i.e. has the
	 * Googlipse nature attached and enabled.
	 * 
	 * @param project
	 * @return <code>true</code> if the project nature is attached an enabled,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasGwtNature(IProject project) {
		if (!project.isAccessible())
			return false;
		try {
			return project.isNatureEnabled(GwtCore.NATURE_ID);
		} catch (CoreException e) {
			// project is closed or does not exists
			return false;
		}
	}

	/**
	 * Indicates if the file is a GWT module definition file.
	 * 
	 * @param resource
	 * @return <code>true</code> if the file is a GWT module definition file,
	 *         <code>false</code> otherwise
	 */
	public static boolean isGwtModuleFile(IResource resource) {
		if ((resource != null) && (resource.getType() == IResource.FILE) && resource.getName().toLowerCase().endsWith("." + Constants.GWT_XML_EXT))
			return true;
		return false;
	}

	public static void writeFileFromTemplate(String templateResource, IFile output, Map<String, String> templateVars) throws IOException, CoreException {

		String contents = Util.getTemplateContents(templateResource);

		for (String aKey : templateVars.keySet()) {
			String value = templateVars.get(aKey).replaceAll("\\\\", "\\\\\\\\");
			contents = contents.replaceAll(aKey, value);
		}

		if (output.exists())
			output.setContents(new ByteArrayInputStream(contents.getBytes()), true, false, null);
		else
			output.create(new ByteArrayInputStream(contents.getBytes()), true, null);
	}
}
