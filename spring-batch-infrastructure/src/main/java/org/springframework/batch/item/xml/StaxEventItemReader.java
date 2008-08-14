package org.springframework.batch.item.xml;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractBufferedItemReaderItemStream;
import org.springframework.batch.item.xml.stax.DefaultFragmentEventReader;
import org.springframework.batch.item.xml.stax.FragmentEventReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Item reader for reading XML input based on StAX.
 * 
 * It extracts fragments from the input XML document which correspond to records
 * for processing. The fragments are wrapped with StartDocument and EndDocument
 * events so that the fragments can be further processed like standalone XML
 * documents.
 * 
 * The implementation is *not* thread-safe.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventItemReader extends AbstractBufferedItemReaderItemStream implements
		ResourceAwareItemReaderItemStream, InitializingBean {

	private FragmentEventReader fragmentReader;

	private XMLEventReader eventReader;

	private EventReaderDeserializer eventReaderDeserializer;

	private Resource resource;

	private InputStream inputStream;

	private String fragmentRootElementName;

	public StaxEventItemReader() {
		setName(ClassUtils.getShortName(StaxEventItemReader.class));
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @param eventReaderDeserializer maps xml fragments corresponding to
	 * records to objects
	 */
	public void setFragmentDeserializer(EventReaderDeserializer eventReaderDeserializer) {
		this.eventReaderDeserializer = eventReaderDeserializer;
	}

	/**
	 * @param fragmentRootElementName name of the root element of the fragment
	 * TODO String can be ambiguous due to namespaces, use QName?
	 */
	public void setFragmentRootElementName(String fragmentRootElementName) {
		this.fragmentRootElementName = fragmentRootElementName;
	}

	/**
	 * Ensure that all required dependencies for the ItemReader to run are
	 * provided after all properties have been set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 * @throws IllegalArgumentException if the Resource, FragmentDeserializer or
	 * FragmentRootElementName is null, or if the root element is empty.
	 * @throws IllegalStateException if the Resource does not exist.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(eventReaderDeserializer, "The FragmentDeserializer must not be null.");
		Assert.hasLength(fragmentRootElementName, "The FragmentRootElementName must not be null");
	}

	/**
	 * Responsible for moving the cursor before the StartElement of the fragment
	 * root.
	 * 
	 * This implementation simply looks for the next corresponding element, it
	 * does not care about element nesting. You will need to override this
	 * method to correctly handle composite fragments.
	 * 
	 * @return <code>true</code> if next fragment was found,
	 * <code>false</code> otherwise.
	 */
	protected boolean moveCursorToNextFragment(XMLEventReader reader) {
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
					return true;
				}
				else {
					reader.nextEvent();
				}
			}
		}
		catch (XMLStreamException e) {
			throw new DataAccessResourceFailureException("Error while reading from event reader", e);
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
		Assert.state(resource.exists(), "Input resource does not exist: [" + resource + "]");

		inputStream = resource.getInputStream();
		eventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
		fragmentReader = new DefaultFragmentEventReader(eventReader);

	}

	/**
	 * Move to next fragment and map it to item.
	 */
	protected Object doRead() throws Exception {
		Object item = null;

		if (moveCursorToNextFragment(fragmentReader)) {
			fragmentReader.markStartFragment();
			item = eventReaderDeserializer.deserializeFragment(fragmentReader);
			fragmentReader.markFragmentProcessed();
		}

		return item;
	}

}
