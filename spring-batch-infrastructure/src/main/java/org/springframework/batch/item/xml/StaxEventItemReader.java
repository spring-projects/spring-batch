/*
 * Copyright 2006-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.item.xml.stax.DefaultFragmentEventReader;
import org.springframework.batch.item.xml.stax.FragmentEventReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * Item reader for reading XML input based on StAX.
 * 
 * It extracts fragments from the input XML document which correspond to records for processing. The fragments are
 * wrapped with StartDocument and EndDocument events so that the fragments can be further processed like standalone XML
 * documents.
 * 
 * The implementation is <b>not</b> thread-safe.
 * 
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class StaxEventItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements
ResourceAwareItemReaderItemStream<T>, InitializingBean {

	private static final Log logger = LogFactory.getLog(StaxEventItemReader.class);

	public static final String DEFAULT_ENCODING = Charset.defaultCharset().name();

	private FragmentEventReader fragmentReader;

	private XMLEventReader eventReader;

	private Unmarshaller unmarshaller;

	private Resource resource;

	private InputStream inputStream;

	private List<QName> fragmentRootElementNames;

	private boolean noInput;

	private boolean strict = true;

	private XMLInputFactory xmlInputFactory = StaxUtils.createDefensiveInputFactory();

	private String encoding = DEFAULT_ENCODING;

	public StaxEventItemReader() {
		setName(ClassUtils.getShortName(StaxEventItemReader.class));
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(org.springframework.batch.item.ExecutionContext)} if the input resource does not exist.
	 * @param strict true by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	@Override
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @param unmarshaller maps xml fragments corresponding to records to objects
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * @param fragmentRootElementName name of the root element of the fragment
	 */
	public void setFragmentRootElementName(String fragmentRootElementName) {
		setFragmentRootElementNames(new String[] {fragmentRootElementName});
	}

	/**
	 * @param fragmentRootElementNames list of the names of the root element of the fragment
	 */
	public void setFragmentRootElementNames(String[] fragmentRootElementNames) {
		this.fragmentRootElementNames = new ArrayList<>();
		for (String fragmentRootElementName : fragmentRootElementNames) {
			this.fragmentRootElementNames.add(parseFragmentRootElementName(fragmentRootElementName));
		}
	}

	/**
	 * Set the {@link XMLInputFactory}.
	 * @param xmlInputFactory to use
	 */
	public void setXmlInputFactory(XMLInputFactory xmlInputFactory) {
		Assert.notNull(xmlInputFactory, "XMLInputFactory must not be null");
		this.xmlInputFactory = xmlInputFactory;
	}

	/**
	 * Set encoding to be used for the input file. Defaults to {@link #DEFAULT_ENCODING}.
	 *
	 * @param encoding the encoding to be used
	 */
	public void setEncoding(String encoding) {
		Assert.notNull(encoding, "The encoding must not be null");
		this.encoding = encoding;
	}

	/**
	 * Ensure that all required dependencies for the ItemReader to run are provided after all properties have been set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @throws IllegalArgumentException if the Resource, FragmentDeserializer or FragmentRootElementName is null, or if
	 * the root element is empty.
	 * @throws IllegalStateException if the Resource does not exist.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(unmarshaller, "The Unmarshaller must not be null.");
		Assert.notEmpty(fragmentRootElementNames, "The FragmentRootElementNames must not be empty");
		for (QName fragmentRootElementName : fragmentRootElementNames) {
			Assert.hasText(fragmentRootElementName.getLocalPart(), "The FragmentRootElementNames must not contain empty elements");
		}		
	}

	/**
	 * Responsible for moving the cursor before the StartElement of the fragment root.
	 * 
	 * This implementation simply looks for the next corresponding element, it does not care about element nesting. You
	 * will need to override this method to correctly handle composite fragments.
	 *
	 * @param reader the {@link XMLEventReader} to be used to find next fragment.
	 * 
	 * @return <code>true</code> if next fragment was found, <code>false</code> otherwise.
	 * 
	 * @throws NonTransientResourceException if the cursor could not be moved. This will be treated as fatal and
	 * subsequent calls to read will return null.
	 */
	protected boolean moveCursorToNextFragment(XMLEventReader reader) throws NonTransientResourceException {
		try {
			while (true) {
				while (reader.peek() != null && !reader.peek().isStartElement()) {
					reader.nextEvent();
				}
				if (reader.peek() == null) {
					return false;
				}
				QName startElementName = ((StartElement) reader.peek()).getName();
				if (isFragmentRootElementName(startElementName)) {
					return true;
				}
				reader.nextEvent();

			}
		}
		catch (XMLStreamException e) {
			throw new NonTransientResourceException("Error while reading from event reader", e);
		}
	}

	@Override
	protected void doClose() throws Exception {
		try {
			if (fragmentReader != null) {
				fragmentReader.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
		finally {
			fragmentReader = null;
			inputStream = null;
		}

	}

	@Override
	protected void doOpen() throws Exception {
		Assert.notNull(resource, "The Resource must not be null.");

		noInput = true;
		if (!resource.exists()) {
			if (strict) {
				throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode)");
			}
			logger.warn("Input resource does not exist " + resource.getDescription());
			return;
		}
		if (!resource.isReadable()) {
			if (strict) {
				throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode)");
			}
			logger.warn("Input resource is not readable " + resource.getDescription());
			return;
		}

		inputStream = resource.getInputStream();
		eventReader = xmlInputFactory.createXMLEventReader(inputStream, this.encoding);
		fragmentReader = new DefaultFragmentEventReader(eventReader);
		noInput = false;

	}

	/**
	 * Move to next fragment and map it to item.
	 */
	@Nullable
	@Override
	protected T doRead() throws IOException, XMLStreamException {

		if (noInput) {
			return null;
		}

		T item = null;

		boolean success = false;
		try {
			success = moveCursorToNextFragment(fragmentReader);
		}
		catch (NonTransientResourceException e) {
			// Prevent caller from retrying indefinitely since this is fatal
			noInput = true;
			throw e;
		}
		if (success) {
			fragmentReader.markStartFragment();

			try {
				@SuppressWarnings("unchecked")
				T mappedFragment = (T) unmarshaller.unmarshal(StaxUtils.createStaxSource(fragmentReader));
				item = mappedFragment;
			}
			finally {
				fragmentReader.markFragmentProcessed();
			}
		}

		return item;
	}

	/*
	 * jumpToItem is overridden because reading in and attempting to bind an entire fragment is unacceptable in a
	 * restart scenario, and may cause exceptions to be thrown that were already skipped in previous runs.
	 */
	@Override
	protected void jumpToItem(int itemIndex) throws Exception {
		for (int i = 0; i < itemIndex; i++) {
			try {
				QName fragmentName = readToStartFragment();
				readToEndFragment(fragmentName);
			} catch (NoSuchElementException e) {
				if (itemIndex == (i + 1)) {
					// we can presume a NoSuchElementException on the last item means the EOF was reached on the last run
					return;
				} else {
					// if NoSuchElementException occurs on an item other than the last one, this indicates a problem
					throw e;
				}
			}
		}
	}

	/*
	 * Read until the first StartElement tag that matches any of the provided fragmentRootElementNames. Because there may be any
	 * number of tags in between where the reader is now and the fragment start, this is done in a loop until the
	 * element type and name match.
	 */
	private QName readToStartFragment() throws XMLStreamException {
		while (true) {
			XMLEvent nextEvent = eventReader.nextEvent();
			if (nextEvent.isStartElement()
					&& isFragmentRootElementName(((StartElement) nextEvent).getName())) {
				return ((StartElement) nextEvent).getName();
			}
		}
	}

	/*
	 * Read until the first EndElement tag that matches the provided fragmentRootElementName. Because there may be any
	 * number of tags in between where the reader is now and the fragment end tag, this is done in a loop until the
	 * element type and name match
	 */
	private void readToEndFragment(QName fragmentRootElementName) throws XMLStreamException {
		while (true) {
			XMLEvent nextEvent = eventReader.nextEvent();
			if (nextEvent.isEndElement()
					&& fragmentRootElementName.equals(((EndElement) nextEvent).getName())) {
				return;
			}
		}
	}
	
	protected boolean isFragmentRootElementName(QName name) {
		for (QName fragmentRootElementName : fragmentRootElementNames) {
			if (fragmentRootElementName.getLocalPart().equals(name.getLocalPart())) {
				if (!StringUtils.hasText(fragmentRootElementName.getNamespaceURI())
						|| fragmentRootElementName.getNamespaceURI().equals(name.getNamespaceURI())) {					
					return true;
				}
			}
		}
		return false;
	}	
	
	private QName parseFragmentRootElementName(String fragmentRootElementName) {
		String name = fragmentRootElementName;
		String nameSpace = null;
		if (fragmentRootElementName.contains("{")) {
			nameSpace = fragmentRootElementName.replaceAll("\\{(.*)\\}.*", "$1");
			name = fragmentRootElementName.replaceAll("\\{.*\\}(.*)", "$1");
		}
		return new QName(nameSpace, name, "");
	}
	
}
