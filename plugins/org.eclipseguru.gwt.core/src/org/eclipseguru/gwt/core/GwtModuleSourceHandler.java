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
 *     dobesv - contributed patch for issue 58
 *******************************************************************************/
package org.eclipseguru.gwt.core;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * An XML event handler for parsing a GWT module source file.
 */
public class GwtModuleSourceHandler extends DefaultHandler {

	/**
	 * An exception indicating that the source is invalid.
	 */
	public static class InvalidModuleSourceException extends SAXException {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an instance of <code>StopParsingException</code> with a
		 * <code>null</code> detail message.
		 */
		public InvalidModuleSourceException(final String message) {
			super(message);
		}
	}

	/**
	 * An exception indicating that the parsing should stop.
	 */
	private static class StopParsingException extends SAXException {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;
	}

	/** default source path */
	private static final String[] DEFAULT_SOURCE_PATHS = new String[] { "client" };

	static final String ELEM_MODULE = "module"; //$NON-NLS-1$
	static final String ELEM_INHERITS = "inherits"; //$NON-NLS-1$
	static final String ELEM_ENTRY_POINT = "entry-point"; //$NON-NLS-1$
	static final String ELEM_SOURCE = "source"; //$NON-NLS-1$
	static final String ELEM_PUBLIC = "public"; //$NON-NLS-1$
	static final String ELEM_SERVLET = "servlet"; //$NON-NLS-1$
	static final String ELEM_SCRIPT = "script"; //$NON-NLS-1$
	static final String ELEM_STYLESHEET = "stylesheet"; //$NON-NLS-1$
	static final String ELEM_EXTEND_PROPERTY = "extend-property"; //$NON-NLS-1$
	static final String ATTR_NAME = "name"; //$NON-NLS-1$
	static final String ATTR_PATH = "path"; //$NON-NLS-1$
	static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	static final String ATTR_SRC = "src"; //$NON-NLS-1$
	static final String ATTR_VALUES = "values"; //$NON-NLS-1$
	static final String ATTR_RENAME_TO = "rename-to"; //$NON-NLS-1$

	private SAXParserFactory factory;
	private int level = -1;

	private String entryPointClass;
	private final List<String> inheritedModules = new ArrayList<String>(4);
	private final List<String> sourcePaths = new ArrayList<String>();

	private String alternateModuleName;

	/**
	 * Creates a new SAX parser for use within this instance.
	 * 
	 * @return The newly created parser.
	 * @throws ParserConfigurationException
	 *             If a parser of the given configuration cannot be created.
	 * @throws SAXException
	 *             If something in general goes wrong when creating the parser.
	 */
	private final SAXParser createParser(final SAXParserFactory parserFactory) throws ParserConfigurationException, SAXException, SAXNotRecognizedException, SAXNotSupportedException {
		// Initialize the parser.
		final SAXParser parser = parserFactory.newSAXParser();
		final XMLReader reader = parser.getXMLReader();
		// disable DTD validation
		// (bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=63625)
		try {
			// be sure validation is "off" or the feature to ignore DTD's will
			// not apply
			reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		} catch (final SAXNotRecognizedException e) {
			// not a big deal if the parser does not recognize the features
		} catch (final SAXNotSupportedException e) {
			// not a big deal if the parser does not support the features
		}
		return parser;
	}

	/*
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		level--;
	}

	/**
	 * @return the alternateModuleName
	 */
	public String getAlternateModuleName() {
		return alternateModuleName;
	}

	/**
	 * @return the entryPointClass
	 */
	public String getEntryPointClass() {
		return entryPointClass;
	}

	private SAXParserFactory getFactory() {
		synchronized (this) {
			if (factory != null) {
				return factory;
			}
			factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
		}
		return factory;
	}

	/**
	 * @return the inheritedModules
	 */
	public String[] getInheritedModules() {
		return inheritedModules.toArray(new String[inheritedModules.size()]);
	}

	/**
	 * @return the sourcePaths
	 */
	public String[] getSourcePaths() {
		if (sourcePaths.isEmpty()) {
			return DEFAULT_SOURCE_PATHS; // default source path
		} else {
			return sourcePaths.toArray(new String[sourcePaths.size()]);
		}
	}

	protected boolean parseContents(final InputSource contents) throws IOException, ParserConfigurationException, SAXException {
		// Parse the file into we have what we need (or an error occurs).
		try {
			factory = getFactory();
			if (factory == null) {
				return false;
			}
			final SAXParser parser = createParser(factory);
			// to support external entities specified as relative URIs
			// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63298)
			contents.setSystemId("/"); //$NON-NLS-1$
			parser.parse(contents, this);
		} catch (final StopParsingException e) {
			// Abort the parsing normally. Fall through...
		}
		return true;
	}

	private void processEntryPoint(final Attributes attributes) throws InvalidModuleSourceException {
		if (null != entryPointClass) {
			throw new InvalidModuleSourceException("entry point defined more than once");
		}
		entryPointClass = attributes.getValue(ATTR_CLASS);
	}

	private void processInherits(final Attributes attributes) {
		final String moduleId = attributes.getValue(ATTR_NAME);
		if (null != moduleId) {
			inheritedModules.add(moduleId);
		}
	}

	private void processModule(final Attributes attributes) throws InvalidModuleSourceException {
		if (null != alternateModuleName) {
			throw new InvalidModuleSourceException("rename-to defined more than once");
		}
		alternateModuleName = attributes.getValue(ATTR_RENAME_TO);
	}

	private void processSource(final Attributes attributes) {
		final String sourcePath = attributes.getValue(ATTR_PATH);
		if (null != sourcePath) {
			sourcePaths.add(sourcePath);
		}
	}

	/*
	 * Resolve external entity definitions to an empty string. This is to speed
	 * up processing of files with external DTDs. Not resolving the contents of
	 * the DTD is ok, as only the System ID of the DTD declaration is used.
	 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

	@Override
	public final void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
		level++;

		switch (level) {
			case 0:
				if (!ELEM_MODULE.equals(elementName)) {
					throw new InvalidModuleSourceException(MessageFormat.format("Root element is not ''{0}''.", ELEM_MODULE));
				}
				processModule(attributes);
				break;

			case 1:
				if (ELEM_ENTRY_POINT.equals(elementName)) {
					processEntryPoint(attributes);
				} else if (ELEM_INHERITS.equals(elementName)) {
					processInherits(attributes);
				} else if (ELEM_SOURCE.equals(elementName)) {
					processSource(attributes);
				}
				break;
		}
	}
}
