package org.springframework.batch.item.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.ClassUtils;

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

	// test xml input
	private String emptyXml = "<root></root>";

	// test xml input
	private String missingXml = "<root><misc1/><misc2>foo</misc2></root>";

	private String fooXml = "<root xmlns=\"urn:org.test.foo\"> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	private String mixedXml = "<fragment xmlns=\"urn:org.test.foo\"> <fragment xmlns=\"urn:org.test.bar\"> <misc1/> </fragment> <misc2/> <fragment xmlns=\"urn:org.test.bar\"> testString </fragment> </fragment>";

	private String invalidXml = "<root> </fragment> <misc1/> </root>";

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
	 * Regular usage scenario. ItemReader should pass XML fragments to unmarshaller wrapped with StartDocument and
	 * EndDocument events.
	 */
	@Test
	public void testFragmentWrapping() throws Exception {
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.close();
	}

	@Test
	public void testFragmentNamespace() throws Exception {

		source.setResource(new ByteArrayResource(fooXml.getBytes()));
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.close();
	}

	@Test
	public void testFragmentMixedNamespace() throws Exception {

		source.setResource(new ByteArrayResource(mixedXml.getBytes()));
		source.setFragmentRootElementName("{urn:org.test.bar}" + FRAGMENT_ROOT_ELEMENT);
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.close();
	}

	@Test
	public void testFragmentInvalid() throws Exception {

		source.setResource(new ByteArrayResource(invalidXml.getBytes()));
		source.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		source.afterPropertiesSet();
		source.open(executionContext);
		// Should fail before it gets to the marshaller
		try {
			assertNotNull(source.read());
			fail("Expected NonTransientResourceException");
		}
		catch (NonTransientResourceException e) {
			// expected
		}
		assertNull(source.read()); // after an error there is no more output

		source.close();
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
	 * Empty document works OK.
	 */
	@Test
	public void testMoveCursorToNextFragmentOnEmpty() throws XMLStreamException, FactoryConfigurationError, IOException {
		Resource resource = new ByteArrayResource(emptyXml.getBytes());
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(resource.getInputStream());

		assertFalse(source.moveCursorToNextFragment(reader));
	}

	/**
	 * Document with no fragments works OK.
	 */
	@Test
	public void testMoveCursorToNextFragmentOnMissing() throws XMLStreamException, FactoryConfigurationError, IOException {
		Resource resource = new ByteArrayResource(missingXml.getBytes());
		XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(resource.getInputStream());
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

		assertEquals(1, executionContext.getInt(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count"));
		List<XMLEvent> expectedAfterRestart = source.read();

		source = createNewInputSouce();
		source.open(executionContext);
		List<XMLEvent> afterRestart = source.read();
		assertEquals(expectedAfterRestart.size(), afterRestart.size());

	}

	@Test
	public void testRestoreWorksFromClosedStream() throws Exception {
		source.close();
		source.update(executionContext);
	}

	/**
	 * Statistics return the current record count. Calling read after end of input does not increase the counter.
	 */
	@Test
	public void testExecutionContext() throws Exception {
		final int NUMBER_OF_RECORDS = 2;
		source.open(executionContext);
		source.update(executionContext);

		for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
			int recordCount = extractRecordCount();
			assertEquals(i, recordCount);
			source.read();
			source.update(executionContext);
		}

		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
		source.read();
		assertEquals(NUMBER_OF_RECORDS, extractRecordCount());
	}

	private int extractRecordCount() {
		return executionContext.getInt(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count");
	}

	@Test
	public void testCloseWithoutOpen() throws Exception {
		source.close();
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

		newSource.close();
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
	public void testOpenBadIOInput() throws Exception {

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
			fail();
		}
		catch (ItemStreamException ex) {
			// expected
		}

		// read() should then return a null
		assertNull(source.read());
		source.close();

	}

	@Test
	public void testNonExistentResource() throws Exception {

		source.setResource(new NonExistentResource());
		source.afterPropertiesSet();

		source.setStrict(false);
		source.open(executionContext);
		assertNull(source.read());

	}

	@Test
	public void testDirectoryResource() throws Exception {

		FileSystemResource resource = new FileSystemResource("target/data");
		resource.getFile().mkdirs();
		assertTrue(resource.getFile().isDirectory());
		source.setResource(resource);
		source.afterPropertiesSet();

		source.setStrict(false);
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

	@Test(expected = ItemStreamException.class)
	public void testStrictness() throws Exception {

		source.setResource(new NonExistentResource());
		source.setStrict(true);
		source.afterPropertiesSet();

		source.open(executionContext);

	}

	/**
	 * Make sure the reader doesn't end up in inconsistent state if there's an error during unmarshalling (BATCH-1738).
	 * After an error during <code>read</code> the next <code>read</code> call should continue with reading the next
	 * fragment.
	 */
	@Test
	public void exceptionDuringUnmarshalling() throws Exception {
		source.setUnmarshaller(new TroublemakerUnmarshaller());
		source.afterPropertiesSet();

		source.open(executionContext);
		try {
			source.read();
			fail();
		}
		catch (UnmarshallingFailureException expected) {
			assert expected.getMessage() == TroublemakerUnmarshaller.MESSAGE;
		}

		try {
			source.read();
			fail();
		}
		catch (UnmarshallingFailureException expected) {
			assert expected.getMessage() == TroublemakerUnmarshaller.MESSAGE;
		}
		assertNull(source.read());
	}

	/**
	 * Stub emulating problems during unmarshalling.
	 */
	private static class TroublemakerUnmarshaller implements Unmarshaller {

		public static final String MESSAGE = "Unmarshallers on strike.";

		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			throw new UnmarshallingFailureException(MESSAGE);
		}

		@SuppressWarnings("rawtypes")
		public boolean supports(Class clazz) {
			return true;
		}

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
	 * A simple XMLEvent unmarshaller mock - check for the start and end document events for the fragment root & end
	 * tags + skips the fragment contents.
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

		@SuppressWarnings("rawtypes")
		public boolean supports(Class clazz) {
			return true;
		}

		/**
		 * A simple mapFragment implementation checking the StaxEventReaderItemReader basic read functionality.
		 * 
		 * @param source
		 * @return list of the events from fragment body
		 */
		public Object unmarshal(Source source) throws XmlMappingException, IOException {

			List<XMLEvent> fragmentContent;
			try {
				XMLEventReader eventReader = StaxUtils.getXmlEventReader(source);

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
			catch (Exception e) {
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

	private static class NonExistentResource extends AbstractResource {

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
