package org.springframework.batch.item.xml;

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
import javax.xml.transform.Source;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.ClassUtils;
import org.springframework.xml.transform.StaxSource;

/**
 * Tests for {@link StaxEventItemReader}.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventItemReaderTests {

	// object under test
	private StaxEventItemReader<List<XMLEvent>> source;

	// test xml input
	private String xml = "<root> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	private Unmarshaller unmarshaller = new MockFragmentUnmarshaller();

	private static final String FRAGMENT_ROOT_ELEMENT = "fragment";

	private ExecutionContext executionContext;

	@Before
	public void setUp() throws Exception {
		this.executionContext = new ExecutionContext();
		source = createNewInputSouce();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		source.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertesSetException() throws Exception {

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
		source.setUnmarshaller(null);
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
	 * unmarshaller wrapped with StartDocument and EndDocument events.
	 */
	@Test
	public void testFragmentWrapping() throws Exception {
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.close(executionContext);
	}

	/**
	 * Cursor is moved before beginning of next fragment.
	 */
	@Test
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
	@Test
	public void testRestart() throws Exception {

		source.open(executionContext);
		source.read();
		source.update(executionContext);

		assertEquals(1, executionContext.getLong(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count"));
		List<XMLEvent> expectedAfterRestart = source.read();

		source = createNewInputSouce();
		source.open(executionContext);
		List<XMLEvent> afterRestart = source.read();
		assertEquals(expectedAfterRestart.size(), afterRestart.size());

	}

	@Test
	public void testRestoreWorksFromClosedStream() throws Exception {
		source.close(executionContext);
		source.update(executionContext);
	}

	/**
	 * Statistics return the current record count. Calling read after end of
	 * input does not increase the counter.
	 */
	@Test
	public void testExecutionContext() throws Exception {
		final int NUMBER_OF_RECORDS = 2;
		source.open(executionContext);
		source.update(executionContext);

		for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
			long recordCount = extractRecordCount();
			assertEquals(i, recordCount);
			source.read();
			source.update(executionContext);
		}

		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
		source.read();
		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
	}

	private long extractRecordCount() {
		return executionContext.getLong(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count");
	}

	@Test
	public void testCloseWithoutOpen() throws Exception {
		source.close(null);
		// No error!
	}

	@Test
	public void testClose() throws Exception {
		MockStaxEventItemReader newSource = new MockStaxEventItemReader();
		Resource resource = new ByteArrayResource(xml.getBytes());
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setUnmarshaller(unmarshaller);

		newSource.open(executionContext);

		Object item = newSource.read();
		assertNotNull(item);
		assertTrue(newSource.isOpenCalled());

		newSource.close(null);
		newSource.setOpenCalled(false);
		// calling read again should require re-initialization because of close
		try {
			newSource.read();
			fail("Expected ReaderNotOpenException");
		}
		catch (Exception e) {
			// expected
		}
	}

	@Test
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
			source.open(executionContext);
		}
		catch (ItemStreamException ex) {
			// expected
		}

	}

	@Test
	public void testNonExistentResource() throws Exception {

		source.setResource(new NonExistentResource());
		source.afterPropertiesSet();

		
		source.open(executionContext);
		assertNull(source.read());
		
	}

	@Test
	public void testRuntimeFileCreation() throws Exception {

		source.setResource(new NonExistentResource());
		source.afterPropertiesSet();

		source.setResource(new ByteArrayResource(xml.getBytes()));
		source.open(executionContext);
		source.read();
	}

	private StaxEventItemReader<List<XMLEvent>> createNewInputSouce() {
		Resource resource = new ByteArrayResource(xml.getBytes());

		StaxEventItemReader<List<XMLEvent>> newSource = new StaxEventItemReader<List<XMLEvent>>();
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setUnmarshaller(unmarshaller);
		newSource.setSaveState(true);

		return newSource;
	}

	/**
	 * A simple XMLEvent unmarshaller mock - check for the start and end
	 * document events for the fragment root & end tags + skips the fragment
	 * contents.
	 */
	private static class MockFragmentUnmarshaller implements Unmarshaller {

		/**
		 * Skips the XML fragment contents.
		 */
		private List<XMLEvent> readRecordsInsideFragment(XMLEventReader eventReader) throws XMLStreamException {
			XMLEvent eventInsideFragment;
			List<XMLEvent> events = new ArrayList<XMLEvent>();
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

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}

		/**
		 * A simple mapFragment implementation checking the
		 * StaxEventReaderItemReader basic read functionality.
		 * 
		 * @param source
		 * @return list of the events from fragment body
		 */
		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			StaxSource staxSource = (StaxSource) source;
			XMLEventReader eventReader = staxSource.getXMLEventReader();
			List<XMLEvent> fragmentContent;
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

	}

	private static class MockStaxEventItemReader extends StaxEventItemReader<List<XMLEvent>> {

		private boolean openCalled = false;

		public void open(ExecutionContext executionContext) {
			super.open(executionContext);
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
