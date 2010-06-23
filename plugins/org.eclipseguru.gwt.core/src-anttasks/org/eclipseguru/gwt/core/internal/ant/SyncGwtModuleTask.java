/***************************************************************************************************
 * Copyright (c) 2010 Eclipse Guru and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Eclipse Guru - initial API and implementation
 *               Eclipse.org - ideas, concepts and code from existing Eclipse projects
 **************************************************************************************************/
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.eclipseguru.gwt.core.internal.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Sync;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.NoneSelector;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Modified Sync task for better dealing with SVN working copies.
 * <p>
 * This is basically a re-implementation of the Sync task.
 * </p>
 */
public class SyncGwtModuleTask extends Sync {

	/**
	 * Subclass Copy in order to access it's file/dir maps.
	 */
	public static class MyCopy extends Copy {

		// List of files that must be copied, irrelevant from the
		// fact that they are newer or not than the destination.
		private final Set<String> nonOrphans = new HashSet<String>();

		/** Constructor for MyCopy. */
		public MyCopy() {
		}

		/**
		 * Get the includeEmptyDirs attribute.
		 * 
		 * @return true if emptyDirs are to be included
		 */
		public boolean getIncludeEmptyDirs() {
			return includeEmpty;
		}

		/**
		 * Get the destination directory.
		 * 
		 * @return the destination directory
		 */
		public File getToDir() {
			return destDir;
		}

		/**
		 * @see Copy#scan(File, File, String[], String[])
		 */
		/** {@inheritDoc} */
		@Override
		protected void scan(final File fromDir, final File toDir, final String[] files, final String[] dirs) {
			assertTrue("No mapper", mapperElement == null);

			super.scan(fromDir, toDir, files, dirs);

			for (int i = 0; i < files.length; ++i) {
				nonOrphans.add(files[i]);
			}
			for (int i = 0; i < dirs.length; ++i) {
				nonOrphans.add(dirs[i]);
			}
		}

		/**
		 * @see Copy#scan(Resource[], File)
		 */
		/** {@inheritDoc} */
		@Override
		protected Map scan(final Resource[] resources, final File toDir) {
			assertTrue("No mapper", mapperElement == null);

			final Map m = super.scan(resources, toDir);

			final Iterator iter = m.keySet().iterator();
			while (iter.hasNext()) {
				nonOrphans.add(((Resource) iter.next()).getName());
			}
			return m;
		}

		/**
		 * Yes, we can.
		 * 
		 * @return true always.
		 * @since Ant 1.7
		 */
		@Override
		protected boolean supportsNonFileResources() {
			return true;
		}
	}

	/**
	 * Inner class used to hold exclude patterns and selectors to save stuff
	 * that happens to live in the target directory but should not get removed.
	 * 
	 * @since Ant 1.7
	 */
	public static class SyncTarget extends AbstractFileSet {

		/**
		 * Constructor for SyncTarget. This just changes the default value of
		 * "defaultexcludes" from true to false.
		 */
		public SyncTarget() {
			super();
		}

		/**
		 * Override AbstractFileSet#setDir(File) to disallow setting the
		 * directory.
		 * 
		 * @param dir
		 *            ignored
		 * @throws BuildException
		 *             always
		 */
		@Override
		public void setDir(final File dir) throws BuildException {
			throw new BuildException("preserveintarget doesn't support the dir " + "attribute");
		}

	}

	/**
	 * Pseudo-assert method.
	 */
	private static void assertTrue(final String message, final boolean condition) {
		if (!condition) {
			throw new BuildException("Assertion Error: " + message);
		}
	}

	// Same as regular <copy> task... see at end-of-file!
	private MyCopy myCopy;

	// Similar to a fileset, but doesn't allow dir attribute to be set
	private SyncTarget syncTarget;

	/**
	 * Adds a collection of filesystem resources to copy.
	 * 
	 * @param rc
	 *            a resource collection
	 * @since Ant 1.7
	 */
	@Override
	public void add(final ResourceCollection rc) {
		myCopy.add(rc);
	}

	/**
	 * Adds a set of files to copy.
	 * 
	 * @param set
	 *            a fileset
	 */
	@Override
	public void addFileset(final FileSet set) {
		add(set);
	}

	/**
	 * A container for patterns and selectors that can be used to specify files
	 * that should be kept in the target even if they are not present in any
	 * source directory.
	 * <p>
	 * You must not invoke this method more than once.
	 * </p>
	 * 
	 * @param s
	 *            a preserveintarget nested element
	 * @since Ant 1.7
	 */
	public void addPreserveInTarget(final SyncTarget s) {
		if (syncTarget != null) {
			throw new BuildException("you must not specify multiple " + "preserveintarget elements.");
		}
		syncTarget = s;
	}

	//
	// Various copy attributes/subelements of <copy> passed thru to <mycopy>
	//

	private void configureTask(final Task helper) {
		helper.setProject(getProject());
		helper.setTaskName(getTaskName());
		helper.setOwningTarget(getOwningTarget());
		helper.init();
	}

	private boolean delete(final File f) {
		if (!(f.delete())) {
			if (Os.isFamily(Os.FAMILY_WINDOWS)) {
				System.gc();
			}
			try {
				Thread.sleep(10L);
			} catch (final InterruptedException ex) {
			}
			if (!(f.delete())) {
				return false;
			}
		}
		return true;
	}

	// Override Task#execute
	/**
	 * Execute the sync task.
	 * 
	 * @throws BuildException
	 *             if there is an error.
	 * @see Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		// The destination of the files to copy
		final File toDir = myCopy.getToDir();

		// The complete list of files to copy
		final Set<String> allFiles = myCopy.nonOrphans;

		// If the destination directory didn't already exist,
		// or was empty, then no previous file removal is necessary!
		final boolean noRemovalNecessary = !toDir.exists() || (toDir.list().length < 1);

		// Copy all the necessary out-of-date files
		log("PASS#1: Copying files to " + toDir, Project.MSG_DEBUG);
		myCopy.execute();

		// Do we need to perform further processing?
		if (noRemovalNecessary) {
			log("NO removing necessary in " + toDir, Project.MSG_DEBUG);
			return; // nope ;-)
		}

		// Get rid of all files not listed in the source filesets.
		log("PASS#2: Removing orphan files from " + toDir, Project.MSG_DEBUG);
		final int[] removedFileCount = removeOrphanFiles(allFiles, toDir);
		logRemovedCount(removedFileCount[0], "dangling director", "y", "ies");
		logRemovedCount(removedFileCount[1], "dangling file", "", "s");

		// Get rid of empty directories on the destination side
		if (!myCopy.getIncludeEmptyDirs()) {
			log("PASS#3: Removing empty directories from " + toDir, Project.MSG_DEBUG);
			final int removedDirCount = removeEmptyDirectories(toDir, false);
			logRemovedCount(removedDirCount, "empty director", "y", "ies");
		}
	}

	// Override Task#init
	/**
	 * Initialize the sync task.
	 * 
	 * @throws BuildException
	 *             if there is a problem.
	 * @see Task#init()
	 */
	@Override
	public void init() throws BuildException {
		// Instantiate it
		myCopy = new MyCopy();
		configureTask(myCopy);

		// Default config of <mycopy> for our purposes.
		myCopy.setFiltering(false);
		myCopy.setIncludeEmptyDirs(false);
		myCopy.setPreserveLastModified(true);
	}

	private boolean isSvnOrCvsMetadataDirectory(final File dir) {
		return dir.isDirectory() && (dir.getName().equalsIgnoreCase(".svn") || dir.getName().equalsIgnoreCase("CVS"));
	}

	private void logRemovedCount(final int count, final String prefix, final String singularSuffix, final String pluralSuffix) {
		final File toDir = myCopy.getToDir();

		String what = (prefix == null) ? "" : prefix;
		what += (count < 2) ? singularSuffix : pluralSuffix;

		if (count > 0) {
			log("Removed " + count + " " + what + " from " + toDir, Project.MSG_INFO);
		} else {
			log("NO " + what + " to remove from " + toDir, Project.MSG_VERBOSE);
		}
	}

	private void removeDir(final File d) {
		String[] list = d.list();
		if (list == null) {
			list = new String[0];
		}
		for (int i = 0; i < list.length; ++i) {
			final String s = list[i];
			final File f = new File(d, s);
			if (f.isDirectory()) {
				removeDir(f);
			} else {
				log("Deleting " + f.getAbsolutePath(), Project.MSG_VERBOSE);
				if (!(delete(f))) {
					log("Unable to delete file " + f.getAbsolutePath(), Project.MSG_WARN);
				}
			}
		}
		log("Deleting directory " + d.getAbsolutePath(), Project.MSG_VERBOSE);
		if (!(delete(d))) {
			log("Unable to delete directory " + d.getAbsolutePath(), Project.MSG_WARN);
		}
	}

	/**
	 * Removes all empty directories from a directory.
	 * <p>
	 * <em>Note that a directory that contains only empty
	 * directories, directly or not, will be removed!</em>
	 * </p>
	 * <p>
	 * Recurses depth-first to find the leaf directories which are empty and
	 * removes them, then unwinds the recursion stack, removing directories
	 * which have become empty themselves, etc...
	 * </p>
	 * 
	 * @param dir
	 *            the root directory to scan for empty directories.
	 * @param removeIfEmpty
	 *            whether to remove the root directory itself if it becomes
	 *            empty.
	 * @return the number of empty directories actually removed.
	 */
	private int removeEmptyDirectories(final File dir, final boolean removeIfEmpty) {
		int removedCount = 0;
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			// the the only children is SVN or CVS metadata we remove it completely
			if ((children.length == 1) && isSvnOrCvsMetadataDirectory(children[0])) {
				removeDir(children[0]);
			} else {
				for (int i = 0; i < children.length; ++i) {
					final File file = children[i];
					// Test here again to avoid method call for non-directories or SVN/CVS directories already handled above!
					if (file.isDirectory() && !isSvnOrCvsMetadataDirectory(dir)) {
						removedCount += removeEmptyDirectories(file, true);
					}
				}
			}
			if (children.length > 0) {
				// This directory may have become empty...
				// We need to re-query its children list!
				children = dir.listFiles();
			}
			if ((children.length < 1) && removeIfEmpty) {
				log("Removing empty directory: " + dir, Project.MSG_DEBUG);
				dir.delete();
				++removedCount;
			}
		}
		return removedCount;
	}

	/**
	 * Removes all files and folders not found as keys of a table (used as a
	 * set!).
	 * <p>
	 * If the provided file is a directory, it is recursively scanned for
	 * orphaned files which will be removed as well.
	 * </p>
	 * <p>
	 * If the directory is an orphan, it will also be removed.
	 * </p>
	 * 
	 * @param nonOrphans
	 *            the table of all non-orphan <code>File</code>s.
	 * @param file
	 *            the initial file or directory to scan or test.
	 * @return the number of orphaned files and directories actually removed.
	 *         Position 0 of the array is the number of orphaned directories.
	 *         Position 1 of the array is the number or orphaned files.
	 */
	private int[] removeOrphanFiles(final Set<String> nonOrphans, final File toDir) {
		final int[] removedCount = new int[] { 0, 0 };
		final String[] excls = nonOrphans.toArray(new String[nonOrphans.size() + 1]);
		// want to keep toDir itself
		excls[nonOrphans.size()] = "";

		DirectoryScanner ds = null;
		if (syncTarget != null) {
			final FileSet fs = new FileSet();
			fs.setDir(toDir);
			fs.setCaseSensitive(syncTarget.isCaseSensitive());
			fs.setFollowSymlinks(syncTarget.isFollowSymlinks());

			// preserveInTarget would find all files we want to keep,
			// but we need to find all that we want to delete - so the
			// meaning of all patterns and selectors must be inverted
			final PatternSet ps = syncTarget.mergePatterns(getProject());
			fs.appendExcludes(ps.getIncludePatterns(getProject()));
			fs.appendIncludes(ps.getExcludePatterns(getProject()));
			fs.setDefaultexcludes(!syncTarget.getDefaultexcludes());

			// selectors are implicitly ANDed in DirectoryScanner.  To
			// revert their logic we wrap them into a <none> selector
			// instead.
			final FileSelector[] s = syncTarget.getSelectors(getProject());
			if (s.length > 0) {
				final NoneSelector ns = new NoneSelector();
				for (final FileSelector element : s) {
					ns.appendSelector(element);
				}
				fs.appendSelector(ns);
			}
			ds = fs.getDirectoryScanner(getProject());
		} else {
			ds = new DirectoryScanner();
			ds.setBasedir(toDir);
		}
		ds.addExcludes(excls);

		ds.scan();
		final String[] files = ds.getIncludedFiles();
		for (final String file : files) {
			final File f = new File(toDir, file);
			log("Removing orphan file: " + f, Project.MSG_DEBUG);
			f.delete();
			++removedCount[1];
		}
		final String[] dirs = ds.getIncludedDirectories();
		// ds returns the directories in lexicographic order.
		// iterating through the array backwards means we are deleting
		// leaves before their parent nodes - thus making sure (well,
		// more likely) that the directories are empty when we try to
		// delete them.
		for (int i = dirs.length - 1; i >= 0; --i) {
			final File f = new File(toDir, dirs[i]);
			if (f.list().length < 1) {
				log("Removing orphan directory: " + f, Project.MSG_DEBUG);
				f.delete();
				++removedCount[0];
			}
		}
		return removedCount;
	}

	/**
	 * If false, note errors to the output but keep going.
	 * 
	 * @param failonerror
	 *            true or false
	 */
	@Override
	public void setFailOnError(final boolean failonerror) {
		myCopy.setFailOnError(failonerror);
	}

	/**
	 * The number of milliseconds leeway to give before deciding a target is out
	 * of date.
	 * <p>
	 * Default is 0 milliseconds, or 2 seconds on DOS systems.
	 * </p>
	 * 
	 * @param granularity
	 *            a <code>long</code> value
	 * @since Ant 1.6.2
	 */
	@Override
	public void setGranularity(final long granularity) {
		myCopy.setGranularity(granularity);
	}

	/**
	 * Used to copy empty directories.
	 * 
	 * @param includeEmpty
	 *            If true copy empty directories.
	 */
	@Override
	public void setIncludeEmptyDirs(final boolean includeEmpty) {
		myCopy.setIncludeEmptyDirs(includeEmpty);
	}

	/**
	 * Overwrite any existing destination file(s).
	 * 
	 * @param overwrite
	 *            if true overwrite any existing destination file(s).
	 */
	@Override
	public void setOverwrite(final boolean overwrite) {
		myCopy.setOverwrite(overwrite);
	}

	/**
	 * Sets the destination directory.
	 * 
	 * @param destDir
	 *            the destination directory
	 */
	@Override
	public void setTodir(final File destDir) {
		myCopy.setTodir(destDir);
	}

	/**
	 * Used to force listing of all names of copied files.
	 * 
	 * @param verbose
	 *            if true force listing of all names of copied files.
	 */
	@Override
	public void setVerbose(final boolean verbose) {
		myCopy.setVerbose(verbose);
	}
}
