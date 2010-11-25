/**
 *
 */
package org.eclipseguru.gwt.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipseguru.gwt.core.GwtCore;
import org.eclipseguru.gwt.core.preferences.GwtCorePreferenceConstants;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Hugo A. Garcia
 *
 */
public class GwtPreferencesPageTest {

	private static PreferenceDialog dialog;
	private static String gwtPath;
	private static File gwtJarFile;
	private static File gwtDirectory;
	private static GwtPreferencesPage page;
	private static StringButtonDialogField field;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		String preferencePageId = GwtPreferencesPage.ID;
		String[] displayedIds = null;
		Object data = null;
		dialog = PreferencesUtil.createPreferenceDialogOn(shell,
				preferencePageId, displayedIds, data);
		page = (GwtPreferencesPage) dialog.getSelectedPage();
		field = page.getGwtHomeDirectoryDialogField();

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String rootPath = root.getLocation().toOSString();

		gwtPath = rootPath + File.separator + "gwtDir";
		gwtDirectory = new File(gwtPath);

		String gwtJarPath = gwtPath + File.separator + "gwt-user.jar";
		gwtJarFile = new File(gwtJarPath);

	}

	@Test
	public void returnNullGWTHomeDirectoryIsNotSet() {
		IPath path = page.getGwtHomeDirectoryPathFromTextField();
		assertNull(path);
	}

	@Test
	public void checkErrorMessageWhenInitialOpenOfDialog() {
		String actual = page.getErrorMessage();
		String expected = "The GWT home directoy must be entered.";
		assertEquals(expected, actual);
	}

	@Test
	public void verifyNotValidStateChangeWhenSettingDirectory() {
		gwtDirectory.delete();
		assertFalse(page.isValid());
		field.setText(gwtPath);
		assertFalse(page.isValid());
		field.setText("");
		gwtDirectory.delete();
	}

	@Test
	public void verifyErrorMessageWhenDirectoryDoesNotExist() {
		gwtDirectory.delete();
		field.setText("/foobar");
		String actual = page.getErrorMessage();
		String expected = "The GWT home directoy does not exists.";
		assertEquals(expected, actual);
		field.setText("");
	}

	@Test
	public void verifyNotValidStateWhenDirectoryIsFile() throws IOException {
		gwtDirectory.mkdir();
		gwtJarFile.createNewFile();
		assertFalse(page.isValid());
		field.setText(gwtJarFile.getAbsolutePath());
		assertFalse(page.isValid());
		gwtJarFile.delete();
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void verifyErrorWhenDirectoryIsFile() throws IOException {
		gwtDirectory.mkdir();
		gwtJarFile.createNewFile();
		field.setText(gwtJarFile.getAbsolutePath());
		String actual = page.getErrorMessage();
		String expected = "The GWT home directoy must be a directory.";
		assertEquals(expected, actual);
		gwtJarFile.delete();
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void findGwtJarFindMethodWorks() throws IOException {
		gwtDirectory.mkdir();
		gwtJarFile.createNewFile();
		field.setText(gwtPath);
		assertTrue(page.isGwtJarFound());
		gwtJarFile.delete();
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void showErrorMessageWhenGwtJarNotFound() throws IOException {
		gwtDirectory.mkdir();
		field.setText(gwtPath);
		String actual = page.getErrorMessage();
		String expected = "The GWT jars are not found. Please change directory.";
		assertEquals(expected, actual);
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void statusShouldBeNotValidWhenGwtJarNotFound() throws IOException {
		gwtDirectory.mkdir();
		field.setText(gwtPath);
		assertFalse(page.isValid());
		field.setText(gwtPath);
		assertFalse(page.isValid() & page.isGwtJarFound());
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void statusIsValidWhenGwtJarFound() throws IOException {
		gwtDirectory.mkdir();
		gwtJarFile.createNewFile();
		assertFalse(page.isValid());
		field.setText(gwtPath);
		assertTrue(page.isValid() & page.isGwtJarFound());
		gwtJarFile.delete();
		gwtDirectory.delete();
		field.setText("");
	}

	@Test
	public void performOKwithOnlyGwtHomeSet() throws IOException {
		gwtDirectory.mkdir();
		gwtJarFile.createNewFile();
		field.setText(gwtPath);
		page.performOk();
		Preferences pluginPreferences = GwtCore.getGwtCore()
				.getPluginPreferences();
		gwtJarFile.delete();
		gwtDirectory.delete();
		field.setText("");
	}

	// These test do test loading saved preferences. In order to accomplish that
	// we need no run the test without clearing the configuration of the
	// pde-junit runtime.

}
