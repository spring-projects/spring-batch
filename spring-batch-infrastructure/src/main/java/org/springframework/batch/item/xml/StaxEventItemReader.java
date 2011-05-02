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

package org.springframework.batch.item.xml;

import java.io.InputStream;

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
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Item reader for reading XML input based on StAX.
 * 
 * It extracts fragments from the input XML document which correspond to records for processing. The fragments are
 * wrapped with StartDocument and EndDocument events so that the fragments can be further processed like standalone XML
 * documents.
 * 
 * The implementation is *not* thread-safe.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements
		ResourceAwareItemReaderItemStream<T>, InitializingBean {

	private static final Log logger = LogFactory.getLog(StaxEventItemReader.class);

	private FragmentEventReader fragmentReader;

	private XMLEventReader eventReader;

	private Unmarshaller unmarshaller;

	private Resource resource;

	private InputStream inputStream;

	private String fragmentRootElementName;

	private boolean noInput;

	private boolean strict = true;

	private String fragmentRootElementNameSpace;

	public StaxEventItemReader() {
		setName(ClassUtils.getShortName(StaxEventItemReader.class));
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(org.springframework.batch.item.ExecutionContext)} if the input resource does not exist.
	 * @param strict false by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

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
		this.fragmentRootElementName = fragmentRootElementName;
	}

	/**
	 * Ensure that all required dependencies for the ItemReader to run are provided after all properties have been set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @throws IllegalArgumentException if the Resource, FragmentDeserializer or FragmentRootElementName is null, or if
	 * the root element is empty.
	 * @throws IllegalStateException if the Resource does not exist.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(unmarshaller, "The Unmarshaller must not be null.");
		Assert.hasLength(fragmentRootElementName, "The FragmentRootElementName must not be null");
		if (fragmentRootElementName.contains("{")) {
			fragmentRootElementNameSpace = fragmentRootElementName.replaceAll("\\{(.*)\\}.*", "$1");
			fragmentRootElementName = fragmentRootElementName.replaceAll("\\{.*\\}(.*)", "$1");
		}
	}

	/**
	 * Responsible for moving the cursor before the StartElement of the fragment root.
	 * 
	 * This implementation simply looks for the next corresponding element, it does not care about element nesting. You
	 * will need to override this method to correctly handle composite fragments.
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
				if (startElementName.getLocalPart().equals(fragmentRootElementName)) {
					if (fragmentRootElementNameSpace == null
							|| startElementName.getNamespaceURI().equals(fragmentRootElementNameSpace)) {
						return true;
					}
				}
				reader.nextEvent();

			}
		}
		catch (XMLStreamException e) {
			throw new NonTransientResourceException("Error while reading from event reader", e);
		}
	}

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
		eventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
		fragmentReader = new DefaultFragmentEventReader(eventReader);
		noInput = false;

	}

	/**
	 * Move to next fragment and map it to item.
	 */
	protected T doRead() throws Exception {

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
				T mappedFragment = (T) unmarshaller.unmarshal(StaxUtils.getSource(fragmentReader));
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
			readToStartFragment();
			readToEndFragment();
		}
	}

	/*
	 * Read until the first StartElement tag that matches the provided fragmentRootElementName. Because there may be any
	 * number of tags in between where the reader is now and the fragment start, this is done in a loop until the
	 * element type and name match.
	 */
	private void readToStartFragment() throws XMLStreamException {
		while (true) {
			XMLEvent nextEvent = eventReader.nextEvent();
			if (nextEvent.isStartElement()
					&& ((StartElement) nextEvent).getName().getLocalPart().equals(fragmentRootElementName)) {
				return;
			}
		}
	}

	/*
	 * Read until the first EndElement tag that matches the provided fragmentRootElementName. Because there may be any
	 * number of tags in between where the reader is now and the fragment end tag, this is done in a loop until the
	 * element type and name match
	 */
	private void readToEndFragment() throws XMLStreamException {
		while (true) {
			XMLEvent nextEvent = eventReader.nextEvent();
			if (nextEvent.isEndElement()
					&& ((EndElement) nextEvent).getName().getLocalPart().equals(fragmentRootElementName)) {
				return;
			}
		}
	}
}
