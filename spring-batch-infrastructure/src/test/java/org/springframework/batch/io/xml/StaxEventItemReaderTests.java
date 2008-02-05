package org.springframework.batch.io.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Tests for {@link StaxEventItemReader}.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventItemReaderTests extends TestCase {

	// object under test
	private StaxEventItemReader source;

	// test xml input
	private String xml = "<root> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	private EventReaderDeserializer deserializer = new MockFragmentDeserializer();

	private static final String FRAGMENT_ROOT_ELEMENT = "fragment";

	protected void setUp() throws Exception {
		source = createNewInputSouce();
	}

	public void testAfterPropertiesSet() throws Exception {
		source.afterPropertiesSet();
	}

	public void testAfterPropertesSetException() throws Exception {
		source.setResource(null);
		try {
			source.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected;
		}

		source = createNewInputSouce();
		source.setFragmentRootElementName("");
		try {
			source.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		source = createNewInputSouce();
		source.setFragmentDeserializer(null);
		try {
			source.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Regular usage scenario. ItemReader should pass XML fragments to
	 * deserializer wrapped with StartDocument and EndDocument events.
	 */
	public void testFragmentWrapping() throws Exception {
		source.afterPropertiesSet();

		// see asserts in the mock deserializer
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.destroy();
	}

	/**
	 * Cursor is moved before beginning of next fragment.
	 */
	public void testMoveCursorToNextFragment() throws XMLStreamException, FactoryConfigurationError, IOException {
		Resource resource = new ByteArrayResource(xml.getBytes());
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(resource.getInputStream());

		final int EXPECTED_NUMBER_OF_FRAGMENTS = 2;
		for (int i = 0; i < EXPECTED_NUMBER_OF_FRAGMENTS; i++) {
			assertTrue(source.moveCursorToNextFragment(reader));
			assertTrue(EventHelper.startElementName(reader.peek()).equals("fragment"));
			reader.nextEvent(); // move away from beginning of fragment
		}
		assertFalse(source.moveCursorToNextFragment(reader));
	}

	/**
	 * Save restart data and restore from it.
	 */
	public void testRestart() {
		source.read();
		ExecutionAttributes streamContext = source.getExecutionAttributes();
		assertEquals(1, streamContext.getLong(StaxEventItemReader.READ_COUNT_STATISTICS_NAME));
		List expectedAfterRestart = (List) source.read();

		source = createNewInputSouce();
		source.restoreFrom(streamContext);
		List afterRestart = (List) source.read();
		assertEquals(expectedAfterRestart.size(), afterRestart.size());
	}

	/**
	 * Restore point must not exceed end of file, input source must not be
	 * already initialised when restoring.
	 */
	public void testInvalidRestore() {
		ExecutionAttributes context = new ExecutionAttributes();
		context.putLong(StaxEventItemReader.READ_COUNT_STATISTICS_NAME, 100000);
		try {
			source.restoreFrom(context);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}

		source = createNewInputSouce();
		source.open();
		try {
			source.restoreFrom(new ExecutionAttributes());
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Skipping marked records after rollback.
	 */
	public void testSkip() {
		List first = (List) source.read();
		source.skip();
		List second = (List) source.read();
		assertFalse(first.equals(second));
		source.reset();

		assertEquals(second, source.read());
	}

	/**
	 * Rollback to last commited record.
	 */
	public void testRollback() {

		// rollback between deserializing records
		List first = (List) source.read();
		source.mark();
		List second = (List) source.read();
		assertFalse(first.equals(second));
		source.reset();

		assertEquals(second, source.read());

		// rollback while deserializing record
		source.reset();
		source.setFragmentDeserializer(new ExceptionFragmentDeserializer());
		try {
			source.read();
		}
		catch (Exception expected) {
			source.reset();
		}
		source.setFragmentDeserializer(deserializer);

		assertEquals(second, source.read());
	}

	/**
	 * Statistics return the current record count. Calling read after end of
	 * input does not increase the counter.
	 */
	public void testStreamContext() {
		final int NUMBER_OF_RECORDS = 2;

		for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
			long recordCount = extractRecordCount();
			assertEquals(i, recordCount);
			source.read();
		}

		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
		source.read();
		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
	}

	private long extractRecordCount() {
		return source.getExecutionAttributes().getLong(StaxEventItemReader.READ_COUNT_STATISTICS_NAME);
	}

	public void testCloseWithoutOpen() throws Exception {
		source.close();
		// No error!
	}

	public void testClose() throws Exception {
		MockStaxEventItemReader newSource = new MockStaxEventItemReader();
		Resource resource = new ByteArrayResource(xml.getBytes());
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setFragmentDeserializer(deserializer);

		Object item = newSource.read();
		assertNotNull(item);
		assertTrue(newSource.isOpenCalled());

		newSource.destroy(); // includes close()
		newSource.setOpenCalled(false);
		// calling read again should require re-initialization because of close
		item = newSource.read();
		assertNotNull(item);
		assertTrue(newSource.isOpenCalled());
	}

	public void testOpenBadIOInput() {

		source.setResource(new AbstractResource() {
			public String getDescription() {
				return null;
			}

			public InputStream getInputStream() throws IOException {
				throw new IOException();
			}

			public boolean exists() {
				return true;
			}
		});

		try {
			source.open();
		}
		catch (DataAccessResourceFailureException ex) {
			assertTrue(ex.getCause() instanceof IOException);
		}

	}

	public void testNonExistentResource() throws Exception {

		source.setResource(new NonExistentResource());
		source.afterPropertiesSet();

		try {
			source.open();
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testRuntimeFileCreation() throws Exception {

		source.setResource(new NonExistentResource());
		source.afterPropertiesSet();

		source.setResource(new ByteArrayResource(xml.getBytes()));
		source.read();
	}

	private StaxEventItemReader createNewInputSouce() {
		Resource resource = new ByteArrayResource(xml.getBytes());

		StaxEventItemReader newSource = new StaxEventItemReader();
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setFragmentDeserializer(deserializer);

		return newSource;
	}

	/**
	 * A simple XMLEvent deserializer mock - check for the start and end
	 * document events for the fragment root & end tags + skips the fragment
	 * contents.
	 */
	private static class MockFragmentDeserializer implements EventReaderDeserializer {

		/**
		 * A simple mapFragment implementation checking the
		 * StaxEventReaderItemReader basic read functionality.
		 * @param eventReader
		 * @return list of the events from fragment body
		 */
		public Object deserializeFragment(XMLEventReader eventReader) {
			List fragmentContent;
			try {
				// first event should be StartDocument
				XMLEvent event1 = eventReader.nextEvent();
				assertTrue(event1.isStartDocument());

				// second should be StartElement of the fragment
				XMLEvent event2 = eventReader.nextEvent();
				assertTrue(event2.isStartElement());
				assertTrue(EventHelper.startElementName(event2).equals(FRAGMENT_ROOT_ELEMENT));

				// jump before the end of fragment
				fragmentContent = readRecordsInsideFragment(eventReader);

				// end of fragment
				XMLEvent event3 = eventReader.nextEvent();
				assertTrue(event3.isEndElement());
				assertTrue(EventHelper.endElementName(event3).equals(FRAGMENT_ROOT_ELEMENT));

				// EndDocument should follow the end of fragment
				XMLEvent event4 = eventReader.nextEvent();
				assertTrue(event4.isEndDocument());

			}
			catch (XMLStreamException e) {
				throw new RuntimeException("Error occured in FragmentDeserializer", e);
			}
			return fragmentContent;
		}

		/**
		 * Skips the XML fragment contents.
		 */
		private List readRecordsInsideFragment(XMLEventReader eventReader) throws XMLStreamException {
			XMLEvent eventInsideFragment;
			List events = new ArrayList();
			do {
				eventInsideFragment = eventReader.peek();
				if (eventInsideFragment instanceof EndElement
						&& ((EndElement) eventInsideFragment).getName().getLocalPart().equals(FRAGMENT_ROOT_ELEMENT)) {
					break;
				}
				events.add(eventReader.nextEvent());
			} while (eventInsideFragment != null);

			return events;
		}

	}

	/**
	 * Moves cursor inside the fragment body and causes rollback.
	 */
	private static class ExceptionFragmentDeserializer implements EventReaderDeserializer {

		public Object deserializeFragment(XMLEventReader eventReader) {
			eventReader.next();
			throw new RuntimeException();
		}

	}

	private static class MockStaxEventItemReader extends StaxEventItemReader {

		private boolean openCalled = false;

		public void open() {
			super.open();
			openCalled = true;
		}

		public boolean isOpenCalled() {
			return openCalled;
		}

		public void setOpenCalled(boolean openCalled) {
			this.openCalled = openCalled;
		}
	}

	private class NonExistentResource extends AbstractResource {

		public NonExistentResource() {
		}

		public boolean exists() {
			return false;
		}

		public String getDescription() {
			return "NonExistantResource";
		}

		public InputStream getInputStream() throws IOException {
			return null;
		}
	}
}
