/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.io.xml.xstream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.xml.ObjectInput;
import org.springframework.batch.io.xml.ObjectInputFactory;
import org.springframework.batch.io.xml.ObjectOutput;
import org.springframework.batch.io.xml.ObjectOutputFactory;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;

/**
 * XStreamFactory class implements both factory interfaces -
 * {@link ObjectInputFactory} and {@link ObjectOutputFactory}. Factory methods
 * {@link #createObjectInput(Resource, String)} and
 * {@link #createObjectOutput(Resource, String)} return implementations of
 * {@link ObjectInput} and {@link ObjectOutput} interfaces. These
 * implementations (ObjectInputWrapper and ObjecOutputWrapper) are defined as
 * factory's inner classes. They are wrapping StAX reader/writer for accessing
 * xml streams, XStream mapper for mapping Xml-to-ValueObjects and
 * {@link FileChannel} for file manipulation This factory implementation uses
 * {@link XStreamConfiguration} as source for XStream configuration settings.
 * 
 * @author peter.zozom
 * @see ObjectInputFactory
 * @see ObjectOutputFactory
 * @see XStreamConfiguration
 */
public class XStreamFactory implements ObjectOutputFactory, ObjectInputFactory {
	private static final Log log = LogFactory.getLog(XStreamFactory.class);

	private XStreamConfiguration config;

	/**
	 * Set the XStream's configuration.
	 * @param config value object holding XStream's configuration settings
	 */
	public void setConfig(XStreamConfiguration config) {
		this.config = config;
	}

	/*
	 * Set up XStream. Proctected visibility modifier is used to allow easier
	 * testing of set...() methods.
	 */
	protected void setUpXStream(XStream stream) {
		setClassAliases(stream);
		setTypeAliases(stream);
		setFieldAliases(stream);
		setAttributeAliases(stream);
		setAttributes(stream);
		registerConverters(stream);
		setMode(stream);
		addImplicitCollections(stream);
		setOmittedFileds(stream);
		addImmutableTypes(stream);
		addDefaultImplementations(stream);
	}

	/*
	 * Iterate over list of DefaultImplementation objects and add default
	 * implementations to the XStream.
	 */
	private void addDefaultImplementations(XStream stream) {

		// get list of DefaultImplementation objects
		List defaultImplementations = config.getDefaultImplementations();
		// if not null iterate over list
		if (defaultImplementations != null) {
			for (Iterator i = defaultImplementations.iterator(); i.hasNext();) {
				DefaultImplementation di = (DefaultImplementation) i.next();

				// try to create Class object for default implementation class
				// name
				Class defaultImplementation;
				try {
					defaultImplementation = Class.forName(di.getDefaultImpl());
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + di.getDefaultImpl(), cnfe);
				}

				// try to create Class object for ofType class name
				Class ofType;
				try {
					ofType = Class.forName(di.getType());
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + di.getType(), cnfe);
				}

				// add default implementation
				stream.addDefaultImplementation(defaultImplementation, ofType);
			}
		}
	}

	/*
	 * Iterate over list of immutable type names and pass them to the XStream.
	 */
	private void addImmutableTypes(XStream stream) {
		// get list of names of immutable types
		List immutableTypes = config.getImmutableTypes();
		// if not null iterate over list
		if (immutableTypes != null) {
			for (Iterator i = immutableTypes.iterator(); i.hasNext();) {
				String it = (String) i.next();

				// try to create Class object for immutableTypeName
				Class immutableType;
				try {
					immutableType = Class.forName(it);
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + it, cnfe);
				}

				// add immutable type
				stream.addImmutableType(immutableType);
			}
		}

	}

	/*
	 * Iterate over list of OmmitField objects and register ommited fields to
	 * the XStream.
	 */
	private void setOmittedFileds(XStream stream) {
		// get list of OmmitedField objects
		List ommitedFields = config.getOmmitedFields();
		// if not null iterate over list
		if (ommitedFields != null) {
			for (Iterator i = ommitedFields.iterator(); i.hasNext();) {
				OmmitedField ommitedField = (OmmitedField) i.next();

				// register field to be ommited
				try {
					stream.omitField(Class.forName(ommitedField.getType()), ommitedField.getFieldName());
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + ommitedField.getType(), cnfe);
				}
			}
		}
	}

	/*
	 * Iterate over list of ImplicitCollection objects and add implicit
	 * collections to the XStream. XStream has 3 methods for adding implicit
	 * collections. Decision which method to call is based on provided settings.
	 */
	private void addImplicitCollections(XStream stream) {
		// get list of ImplicionCollection object
		List implicitCollections = config.getImplicitCollections();
		// if not null iterate over list
		if (implicitCollections != null) {
			for (Iterator i = implicitCollections.iterator(); i.hasNext();) {
				ImplicitCollection impCol = (ImplicitCollection) i.next();
				String typeName = impCol.getOwnerType();

				// create Class object for typeName
				try {
					Class ownerType = Class.forName(typeName);

					// if itemType not provided, add implicit collection for any
					// unmapped xml tag
					if (impCol.getItemType() == null) {
						stream.addImplicitCollection(ownerType, impCol.getFieldName());
					}
					else {
						typeName = impCol.getItemType();

						// create Class object for itemType
						Class itemType = Class.forName(typeName);
						// if itemFieldName not provided, add implicit
						// collection for all items of the given itemType
						if (impCol.getItemFieldName() == null) {
							stream.addImplicitCollection(ownerType, impCol.getFieldName(), itemType);
						}
						else {
							// else add implicit collection for all items of the
							// given element name defined by itemFieldName
							stream.addImplicitCollection(ownerType, impCol.getFieldName(), impCol.getItemFieldName(),
									itemType);
						}
					}
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + typeName, cnfe);
				}
			}
		}
	}

	/*
	 * Set mode for dealing with duplicate references.
	 */
	private void setMode(XStream stream) {
		stream.setMode(config.getMode());
	}

	/*
	 * Iterate over list of ConverterProperties objects and register converters
	 * to XStream.
	 */
	private void registerConverters(XStream stream) {
		// get list of ConverterProperties
		List converters = config.getConverters();
		// if not null iterate over list
		if (converters != null) {
			for (Iterator i = converters.iterator(); i.hasNext();) {
				ConverterProperties cp = (ConverterProperties) i.next();

				// create Class object for converter class name
				try {
					Class converter = Class.forName(cp.getClassName());

					// if converter type is assignable to SingleValueConverter,
					// register it as SingleValueConverter
					if (SingleValueConverter.class.isAssignableFrom(converter)) {
						stream.registerConverter((SingleValueConverter) converter.newInstance(), cp.getPriority());
						// if converter type is assignable to Converter,
						// register it as Converter
					}
					else if (Converter.class.isAssignableFrom(converter)) {
						stream.registerConverter((Converter) converter.newInstance(), cp.getPriority());
					}
					else {
						throw new BatchEnvironmentException("Unable to register converter for class: "
								+ cp.getClassName());
					}
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + cp.getClassName(), cnfe);
				}
				catch (InstantiationException ie) {
					log.debug(ie);
					throw new BatchEnvironmentException("Unable to instantiate class: " + cp.getClassName(),
							ie);
				}
				catch (IllegalAccessException iae) {
					log.debug(iae);
					throw new BatchEnvironmentException("Unable to instantiate class: " + cp.getClassName(),
							iae);
				}
			}
		}
	}

	/*
	 * Iterate over list of AttributeProperties objects and map XML attributes
	 * to fields or types.
	 */
	private void setAttributes(XStream stream) {
		// get list of AttributeProperties objects
		List attributeProperties = config.getAttributes();
		// if not null iterate over list
		if (attributeProperties != null) {
			for (Iterator i = attributeProperties.iterator(); i.hasNext();) {
				AttributeProperties ap = (AttributeProperties) i.next();
				String fieldName = ap.getFieldName();

				// create Class object for type name
				try {
					Class type = Class.forName(ap.getType());

					// if field name is provided, map attribute to the field
					if (fieldName != null) {
						stream.useAttributeFor(fieldName, type);
					}
					else {
						// else map attribute to the type
						stream.useAttributeFor(type);
					}

				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + ap.getType(), cnfe);
				}
			}
		}
	}

	/*
	 * Iterate over list of AttributeAlias objects and configure attribute
	 * aliases
	 */
	private void setAttributeAliases(XStream stream) {
		// get list of AttributeAlias objects
		List attributeAliases = config.getAttributeAliases();
		// if not null iterate over list
		if (attributeAliases != null) {
			for (Iterator i = attributeAliases.iterator(); i.hasNext();) {
				AttributeAlias alias = (AttributeAlias) i.next();
				stream.aliasAttribute(alias.getAlias(), alias.getAttributeName());
			}
		}
	}

	/*
	 * Iterate over list of FieldAlias objects and configure field aliases
	 */
	private void setFieldAliases(XStream stream) {
		// get list of FieldAlias objects
		List fieldAliases = config.getFieldAliases();
		// if not null iterate over list
		if (fieldAliases != null) {
			for (Iterator i = fieldAliases.iterator(); i.hasNext();) {
				FieldAlias alias = (FieldAlias) i.next();

				try {
					stream.aliasField(alias.getAliasName(), Class.forName(alias.getType()), alias.getFieldName());
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + alias.getType(), cnfe);
				}
			}
		}
	}

	/*
	 * Iterate over list of TypeAlias objects and configure type aliases
	 */
	private void setTypeAliases(XStream stream) {
		// get list of TypeAlias objects
		List typeAliases = config.getTypeAliases();
		// if not null iterate over list
		if (typeAliases != null) {
			for (Iterator i = typeAliases.iterator(); i.hasNext();) {
				TypeAlias alias = (TypeAlias) i.next();

				try {
					stream.aliasType(alias.getName(), Class.forName(alias.getType()));
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + alias.getType(), cnfe);
				}
			}
		}
	}

	/*
	 * Iterate over list of ClassAlias objects and configure class aliases
	 */
	private void setClassAliases(XStream stream) {
		// get list of ClassAlias objects
		List classAliases = config.getClassAliases();
		// if not null iterate over list
		if (classAliases != null) {
			for (Iterator i = classAliases.iterator(); i.hasNext();) {
				ClassAlias alias = (ClassAlias) i.next();

				Class type;
				try {
					type = Class.forName(alias.getType());
				}
				catch (ClassNotFoundException cnfe) {
					log.debug(cnfe);
					throw new BatchEnvironmentException("Unable to find class: " + alias.getType(), cnfe);
				}

				if (alias.getDefaultImplementation() != null) {
					Class defaultImplementation;
					try {
						defaultImplementation = Class.forName(alias.getDefaultImplementation());
					}
					catch (ClassNotFoundException cnfe) {
						log.debug(cnfe);
						throw new BatchEnvironmentException(
								"Unable to find class: " + alias.getDefaultImplementation(), cnfe);
					}

					stream.alias(alias.getName(), type, defaultImplementation);
				}
				else {
					stream.alias(alias.getName(), type);
				}
			}
		}
	}

	/*
	 * Create QNameMap from list of Mapping objects.
	 */
	private QNameMap getMapping() {

		List mappings = config.getMappings();

		QNameMap map = new QNameMap();

		if (mappings != null) {
			for (Iterator i = mappings.iterator(); i.hasNext();) {
				Mapping mapping = (Mapping) i.next();
				QName qname = new QName(mapping.getNamespaceURI(), mapping.getLocalPart(), mapping.getPrefix());
				map.registerMapping(qname, mapping.getClassName());
			}
		}

		return map;
	}

	/**
	 * Creates instance of {@link ObjectInput} which is used by
	 * for deserializing object from XML file.
	 * @param resource the input XML file
	 * @param encoding the encoding to use
	 * @return ObjectInput which will read from the provided file
	 * @see org.springframework.batch.io.xml.ObjectInputFactory#createObjectInput(Resource,
	 * java.lang.String)
	 */
	public ObjectInput createObjectInput(Resource resource, String encoding) {

		ObjectInput wrapper;

		XStream stream = new XStream();
		setUpXStream(stream);

		try {
			XMLInputFactory xmlif = XMLInputFactory.newInstance();
			XMLStreamReader xmlReader = xmlif.createXMLStreamReader(resource.getInputStream(), encoding);

			StaxReader reader = new StaxReader(getMapping(), xmlReader);
			java.io.ObjectInput input = stream.createObjectInputStream(reader);
			wrapper = new ObjectInputWrapper(xmlReader, input);
		}
		catch (XMLStreamException xse) {
			log.error(xse);
			throw new DataAccessResourceFailureException("Unable to get XML reader", xse);
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to get ObjectInputStream", ioe);
		}

		return wrapper;
	}

	/**
	 * Creates instance of {@link ObjectOutput} which is used by
	 * for serializing object to XML file.
	 * @param resource the output XML file
	 * @param encoding the encoding to use
	 * @return ObjectOutput which will write to the provided file
	 * @see org.springframework.batch.io.xml.ObjectOutputFactory#createObjectOutput(Resource,
	 * java.lang.String)
	 */
	public ObjectOutput createObjectOutput(Resource resource, String encoding) {

		ObjectOutput wrapper;
		FileChannel channel;

		XStream stream = new XStream();
		setUpXStream(stream);

		try {
			XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

			FileOutputStream os;

			try {
				os = new FileOutputStream(resource.getFile(), true);
				channel = os.getChannel();
			}
			catch (FileNotFoundException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("Unable to write to file resource: [" + resource + "]",
						ioe);
			}

			XMLStreamWriter xmlWriter = xmlof.createXMLStreamWriter(os, encoding);

			StaxWriter writer = new StaxWriter(getMapping(), xmlWriter);
			String rootElementName = config.getRootElementName();
			java.io.ObjectOutput output;
			if (rootElementName != null) {
				output = stream.createObjectOutputStream(writer, rootElementName);
			}
			else {
				output = stream.createObjectOutputStream(writer);
			}

			writeAttributes(xmlWriter, config.getRootElementAttributes());
			wrapper = new ObjectOutputWrapper(xmlWriter, channel, output);

		}
		catch (XMLStreamException xse) {
			log.error(xse);
			throw new DataAccessResourceFailureException("Unable to get XML writer", xse);
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to get ObjectOutputStream", ioe);
		}

		return wrapper;
	}

	/*
	 * Writes attributes to current xml element
	 * 
	 * @param attributes map of attributes (key, value) @throws
	 * XMLStreamException
	 */
	private void writeAttributes(XMLStreamWriter xmlWriter, Map attributes) throws XMLStreamException {
		if ((attributes != null) && !attributes.isEmpty()) {

			for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				xmlWriter.writeAttribute((String) entry.getKey(), (String) entry.getValue());
			}
		}
	}

	/**
	 * Implementation of {@link ObjectInput} which wraps
	 * {@link java.io.ObjectInput} and {@link XMLStreamReader} (which is StAX
	 * parser) objects. Each of these objects handles the same input file on
	 * different level and provides different set of methods:
	 * <UL>
	 * <LI>java.io.ObjectInput object is used for reading mapped objects</LI>
	 * <LI>XMLStreamReader object is used for getting actual position (line
	 * number) within xml file</LI>
	 * </UL>
	 */
	public static class ObjectInputWrapper implements ObjectInput {

		java.io.ObjectInput input;

		XMLStreamReader reader;

		/**
		 * Postprocessing after restart. Current implementation does nothing.
		 * @param data
		 * @see org.springframework.batch.io.xml.ObjectInput#afterRestart(java.lang.Object)
		 */
		public void afterRestart(Object data) {
		}

		/**
		 * Constructor.
		 * 
		 * @param reader the xml stream reader
		 * @param input the object input pointing to same file as reader
		 */
		public ObjectInputWrapper(XMLStreamReader reader, java.io.ObjectInput input) {
			this.input = input;
			this.reader = reader;
		}

		/**
		 * Close the object input. It closes all wraped input streams
		 * @see org.springframework.batch.io.xml.ObjectInput#close()
		 */
		public void close() {
			try {
				input.close();
				reader.close();
			}
			catch (XMLStreamException xse) {
				log.error(xse);
				throw new DataAccessResourceFailureException("Unable to close XML Input Source", xse);
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("Unable to close ObjectInputStream", ioe);
			}
		}

		/**
		 * Return the current line number in the input stream.
		 * @return the current line number
		 * @see org.springframework.batch.io.xml.ObjectInput#position()
		 */
		public long position() {
			Location location = reader.getLocation();
			return location.getLineNumber();
		}

		/**
		 * Read and return an object.
		 * @return the object read from the stream
		 * @throws ClassNotFoundException If the class of a serialized bject
		 * cannot be found.
		 * @throws IOException If any of the usual Input/Output related
		 * exceptions occur.
		 * @see org.springframework.batch.io.xml.ObjectInput#readObject()
		 */
		public Object readObject() throws ClassNotFoundException, IOException {
			return input.readObject();
		}

	}

	/**
	 * Implementation of ObjectOutput which wraps java.io.ObjectOutput,
	 * XMLStreamWriter and FileChannel objects. Each of these objects handles
	 * the same output file on different level and provides different set of
	 * methods:
	 * <UL>
	 * <LI>java.io.ObjectOutput object is used for writing java objects to xml
	 * output file</LI>
	 * <LI>FileChannel object is used for file manipulation (truncate,
	 * position, size)</LI>
	 * <LI>XMLStreamWriter object is used for writing XML elements directly to
	 * XML output stream</LI>
	 * </UL>
	 */
	public static class ObjectOutputWrapper implements ObjectOutput {

		java.io.ObjectOutput output;

		XMLStreamWriter writer;

		FileChannel channel;

		/**
		 * Constructor.
		 * 
		 * @param writer the xml stream writer
		 * @param channel the file channel pointing to same file as writer
		 * @param output the object output pointing to same file as writer
		 */
		public ObjectOutputWrapper(XMLStreamWriter writer, FileChannel channel, java.io.ObjectOutput output) {
			this.writer = writer;
			this.channel = channel;
			this.output = output;
		}

		/**
		 * Postprocessing after restart. It removes redundant xml header.
		 * @param data java.lang.Long restart file position
		 * @see org.springframework.batch.io.xml.ObjectOutput#afterRestart(java.lang.Object)
		 */
		public void afterRestart(Object data) {

			long offset = ((Long) data).longValue();

			// When xmlWriter is initialized, it always writes xml header and
			// opening tag of root element
			// but this is unwanted, because currently we are restarting job.
			// Header and opening
			// tag of root element have been already written at the beginning of
			// job processing.
			// Current output file looks like this:

			// 1. <?xml version='1.0' encoding='utf-8'?>
			// 2. <root>
			// 3-n. .... <someRecords/> ....
			// n+1. </root>
			// n+2. <?xml version='1.0' encoding='utf-8'?>
			// n+3. <root

			// Opening tag is not currently finished. We want to remove lines
			// n,n+1,n+2,n+3,...
			// offset is currently equal to position of beginning of line n+1.

			// NOTE: openning tag of root element is always written when output
			// is being opened.
			// closing tag of root element is always written when output is
			// being closed.
			try {
				// At first we write some element to writer, e.g. empty comment.
				// This will change output like this:

				// n+3. <root>
				// n+4. <!-- -->
				writer.writeComment("");
				// Now we flush output stream. Lines n+2,n+3,n+4 are now written
				// to the file.
				output.flush();
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("Unable to write to ObjectOutputStream", ioe);
			}
			catch (XMLStreamException xse) {
				log.error(xse);
				throw new DataAccessResourceFailureException("Unable to get XML writer", xse);
			}

			// Finally we truncate file size to lastMarkedByteOffsetPosition.
			// This will remove lines n+1 .. n+4.
			truncate(offset);
			position(offset);
		}

		/**
		 * Close the object output, which means to close all wrapped output
		 * streams.
		 * @see org.springframework.batch.io.xml.ObjectOutput#close()
		 */
		public void close() {
			try {
				output.close();
				writer.close();
				channel.close();
			}
			catch (XMLStreamException xse) {
				log.error(xse);
				throw new DataAccessResourceFailureException("Unable to close XML Output Source", xse);
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("Unable to close ObjectOutputStream", ioe);
			}
		}

		/**
		 * Flush the object output. This will write any buffered output bytes.
		 * @see org.springframework.batch.io.xml.ObjectOutput#flush()
		 */
		public void flush() {
			try {
				output.flush();
			}
			catch (IOException ioe) {
				log.debug(ioe);
				throw new DataAccessResourceFailureException("An error occured while writing to XmlOutputSource", ioe);
			}
		}

		/**
		 * Retrieve file position.
		 * @return File position, a non-negative integer counting the number of
		 * bytes from the beginning of the file to the current position
		 * @see org.springframework.batch.io.xml.ObjectOutput#position()
		 */
		public long position() {
			long position = 0;
			
			// flush buffer before getting position
			flush();

			try {
				position = channel.position();
			}
			catch (IOException ioe) {
				log.debug(ioe);
				throw new DataAccessResourceFailureException("An error occured while writing to XmlOutputSource", ioe);
			}
			return position;
		}

		/**
		 * Set the file position.
		 * @param newPosition The new position, a non-negative integer counting
		 * the number of bytes from the beginning of the file
		 * @see org.springframework.batch.io.xml.ObjectOutput#position(long)
		 */
		public void position(long newPosition) {
			try {
				channel.position(newPosition);
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("An error occured while writing to XmlOutputSource", ioe);
			}
		}

		/**
		 * Returns the current size of the file.
		 * @return The current size of the file, measured in bytes
		 * @see org.springframework.batch.io.xml.ObjectOutput#size()
		 */
		public long size() {
			long size;

			try {
				size = channel.size();
			}
			catch (IOException ioe) {
				log.debug(ioe);
				throw new DataAccessResourceFailureException("An error occured while writing to XmlOutputSource", ioe);
			}
			return size;
		}

		/**
		 * Truncates the file to the given size.
		 * @param size The new size, a non-negative byte count
		 * @see org.springframework.batch.io.xml.ObjectOutput#truncate(long)
		 */
		public void truncate(long size) {
			try {
				channel.truncate(size);
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("An error occured while writing to XmlOutputSource", ioe);
			}
		}

		/**
		 * Write object to the underlying stream.
		 * @param obj the object to write
		 * @throws IOException Any of the usual Input/Output related exceptions.
		 * @see org.springframework.batch.io.xml.ObjectOutput#writeObject(java.lang.Object)
		 */
		public void writeObject(Object obj) throws IOException {
			output.writeObject(obj);
		}
	}
}
