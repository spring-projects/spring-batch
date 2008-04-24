package org.springframework.batch.item.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.xml.stax.DefaultFragmentEventReader;
import org.springframework.batch.item.xml.stax.DefaultTransactionalEventReader;
import org.springframework.batch.item.xml.stax.FragmentEventReader;
import org.springframework.batch.item.xml.stax.TransactionalEventReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Input source for reading XML input based on StAX.
 * 
 * It extracts fragments from the input XML document which correspond to records
 * for processing. The fragments are wrapped with StartDocument and EndDocument
 * events so that the fragments can be further processed like standalone XML
 * documents.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventItemReader extends ExecutionContextUserSupport implements ItemReader, ItemStream,
		InitializingBean {

	private static final String READ_COUNT_STATISTICS_NAME = "read.count";

	private FragmentEventReader fragmentReader;

	private TransactionalEventReader txReader;

	private EventReaderDeserializer eventReaderDeserializer;

	private Resource resource;

	private InputStream inputStream;

	private String fragmentRootElementName;

	private boolean initialized = false;

	private long lastCommitPointRecordCount = 0;

	private long currentRecordCount = 0;

	private boolean saveState = false;

	public StaxEventItemReader() {
		setName(ClassUtils.getShortName(StaxEventItemReader.class));
	}

	/**
	 * Read in the next root element from the file, and return it.
	 * 
	 * @return the next available record, if none exist, return null
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public Object read() {
		if (!initialized) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}
		Object item = null;

		currentRecordCount++;
		if (moveCursorToNextFragment(fragmentReader)) {
			fragmentReader.markStartFragment();
			item = eventReaderDeserializer.deserializeFragment(fragmentReader);
			fragmentReader.markFragmentProcessed();
		}

		if (item == null) {
			currentRecordCount--;
		}
		return item;
	}

	public void close(ExecutionContext executionContext) {
		initialized = false;
		try {
			if (fragmentReader != null) {
				fragmentReader.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
		catch (XMLStreamException e) {
			throw new DataAccessResourceFailureException("Error while closing event reader", e);
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException("Error while closing input stream", e);
		}
		finally {
			fragmentReader = null;
			inputStream = null;
		}
	}

	public void open(ExecutionContext executionContext) {
		Assert.state(resource.exists(), "Input resource does not exist: [" + resource + "]");

		try {
			inputStream = resource.getInputStream();
			txReader = new DefaultTransactionalEventReader(XMLInputFactory.newInstance().createXMLEventReader(
					inputStream));
			fragmentReader = new DefaultFragmentEventReader(txReader);
		}
		catch (XMLStreamException xse) {
			throw new DataAccessResourceFailureException("Unable to create XML reader", xse);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to get input stream", ioe);
		}
		initialized = true;

		if (executionContext.containsKey(getKey(READ_COUNT_STATISTICS_NAME))) {
			long restoredRecordCount = executionContext.getLong(getKey(READ_COUNT_STATISTICS_NAME));
			int REASONABLE_ADHOC_COMMIT_FREQUENCY = 100;
			while (currentRecordCount <= restoredRecordCount) {
				currentRecordCount++;
				if (currentRecordCount % REASONABLE_ADHOC_COMMIT_FREQUENCY == 0) {
					txReader.onCommit(); // reset the history buffer
				}
				if (!fragmentReader.hasNext()) {
					throw new ItemStreamException("Restore point must be before end of input");
				}
				fragmentReader.next();
				moveCursorToNextFragment(fragmentReader);
			}
			mark(); // reset the history buffer
		}
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
		Assert.notNull(resource, "The Resource must not be null.");
		Assert.notNull(eventReaderDeserializer, "The FragmentDeserializer must not be null.");
		Assert.hasLength(fragmentRootElementName, "The FragmentRootElementName must not be null");
	}

	/**
	 * @see ItemStream#update(ExecutionContext)
	 */
	public void update(ExecutionContext executionContext) {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putLong(getKey(READ_COUNT_STATISTICS_NAME), currentRecordCount);
		}
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

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.AbstractItemReader#mark()
	 */
	public void mark() {
		lastCommitPointRecordCount = currentRecordCount;
		txReader.onCommit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		currentRecordCount = lastCommitPointRecordCount;
		txReader.onRollback();
		fragmentReader.reset();
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to
	 * save any state from this stream, and you don't need it to be restartable.
	 * 
	 * @param saveState flag value (default true)
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}
