/*
 * Copyright 2008-2020 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemCountAware;
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
import org.springframework.util.xml.StaxUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link StaxEventItemReader}.
 * 
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class StaxEventItemReaderTests {

	// object under test
	private StaxEventItemReader<List<XMLEvent>> source;

	// test xml input
	private String xml = "<root> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	// test xml input
	private String xmlMultiFragment = "<root> <fragmentA> <misc1/> </fragmentA> <misc2/> <fragmentB> testString </fragmentB> <fragmentA xmlns=\"urn:org.test.bar\"> testString </fragmentA></root>";

	// test xml input
	private String xmlMultiFragmentNested = "<root> <fragmentA> <misc1/> <fragmentB> nested</fragmentB> <fragmentB> nested </fragmentB></fragmentA> <misc2/> <fragmentB> testString </fragmentB> <fragmentA xmlns=\"urn:org.test.bar\"> testString </fragmentA></root>";

	// test xml input
	private String emptyXml = "<root></root>";

	// test xml input
	private String missingXml = "<root><misc1/><misc2>foo</misc2></root>";

	private String fooXml = "<root xmlns=\"urn:org.test.foo\"> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	private String mixedXml = "<fragment xmlns=\"urn:org.test.foo\"> <fragment xmlns=\"urn:org.test.bar\"> <misc1/> </fragment> <misc2/> <fragment xmlns=\"urn:org.test.bar\"> testString </fragment> </fragment>";

	private String invalidXml = "<root> </fragment> <misc1/> </root>";

	private Unmarshaller unmarshaller = new MockFragmentUnmarshaller();

	private static final String FRAGMENT_ROOT_ELEMENT = "fragment";
	
	private static final String[] MULTI_FRAGMENT_ROOT_ELEMENTS = {"fragmentA", "fragmentB"};

	private ExecutionContext executionContext;

	@Before
	public void setUp() throws Exception {
		this.executionContext = new ExecutionContext();
		source = createNewInputSource();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		source.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertesSetException() throws Exception {

		source = createNewInputSource();
		source.setFragmentRootElementName("");
		try {
			source.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		source = createNewInputSource();
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

	/**
	 * Regular usage scenario with custom encoding.
	 */
	@Test
	public void testCustomEncoding() throws Exception {
		Charset encoding = StandardCharsets.ISO_8859_1;
		ByteBuffer xmlResource = encoding.encode(xml);
		source.setResource(new ByteArrayResource(xmlResource.array()));
		source.setEncoding(encoding.name());
		source.afterPropertiesSet();
		source.open(executionContext);

		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments

		source.close();
	}

	@Test
	public void testItemCountAwareFragment() throws Exception {
		StaxEventItemReader<ItemCountAwareFragment> source = createNewItemCountAwareInputSource();
		source.afterPropertiesSet();
		source.open(executionContext);
		assertEquals(1, source.read().getItemCount());
		assertEquals(2, source.read().getItemCount());
		assertNull(source.read()); // there are only two fragments

		source.close();
	}

	@Test
	public void testItemCountAwareFragmentRestart() throws Exception {
		StaxEventItemReader<ItemCountAwareFragment> source = createNewItemCountAwareInputSource();
		source.afterPropertiesSet();
		source.open(executionContext);
		assertEquals(1, source.read().getItemCount());
		source.update(executionContext);
		source.close();
		source = createNewItemCountAwareInputSource();
		source.afterPropertiesSet();
		source.open(executionContext);
		assertEquals(2, source.read().getItemCount());
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
	
	@Test
	public void testMultiFragment() throws Exception {

		source.setResource(new ByteArrayResource(xmlMultiFragment.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only three fragments

		source.close();
	}	

	@Test
	public void testMultiFragmentNameSpace() throws Exception {

		source.setResource(new ByteArrayResource(xmlMultiFragment.getBytes()));
		source.setFragmentRootElementNames(new String[] {"{urn:org.test.bar}fragmentA", "fragmentB"});
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments (one has wrong namespace)

		source.close();
	}	

	@Test
	public void testMultiFragmentRestart() throws Exception {

		source.setResource(new ByteArrayResource(xmlMultiFragment.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		
		source.update(executionContext);		
		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count"));
		
		source.close();
		
		source = createNewInputSource();
		source.setResource(new ByteArrayResource(xmlMultiFragment.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		
		assertNotNull(source.read());
		assertNull(source.read()); // there are only three fragments

		source.close();
	}	

	@Test
	public void testMultiFragmentNested() throws Exception {

		source.setResource(new ByteArrayResource(xmlMultiFragmentNested.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only three fragments

		source.close();
	}
	
	@Test
	public void testMultiFragmentNestedRestart() throws Exception {

		source.setResource(new ByteArrayResource(xmlMultiFragmentNested.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		// see asserts in the mock unmarshaller
		assertNotNull(source.read());
		assertNotNull(source.read());
		
		source.update(executionContext);		
		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count"));
		
		source.close();
		
		source = createNewInputSource();
		source.setResource(new ByteArrayResource(xmlMultiFragment.getBytes()));
		source.setFragmentRootElementNames(MULTI_FRAGMENT_ROOT_ELEMENTS);
		source.afterPropertiesSet();
		source.open(executionContext);
		
		assertNotNull(source.read());
		assertNull(source.read()); // there are only three fragments

		source.close();
	}	
	
	/**
	 * Cursor is moved before beginning of next fragment.
	 */
	@Test
	public void testMoveCursorToNextFragment() throws XMLStreamException, FactoryConfigurationError, IOException {
		Resource resource = new ByteArrayResource(xml.getBytes());
		XMLEventReader reader = StaxUtils.createDefensiveInputFactory().createXMLEventReader(resource.getInputStream());

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
		XMLEventReader reader = StaxUtils.createDefensiveInputFactory().createXMLEventReader(resource.getInputStream());

		assertFalse(source.moveCursorToNextFragment(reader));
	}

	/**
	 * Document with no fragments works OK.
	 */
	@Test
	public void testMoveCursorToNextFragmentOnMissing() throws XMLStreamException, FactoryConfigurationError, IOException {
		Resource resource = new ByteArrayResource(missingXml.getBytes());
		XMLEventReader reader = StaxUtils.createDefensiveInputFactory().createXMLEventReader(resource.getInputStream());
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

		source = createNewInputSource();
		source.open(executionContext);
		List<XMLEvent> afterRestart = source.read();
		assertEquals(expectedAfterRestart.size(), afterRestart.size());

	}

	/**
	 * Test restart at end of file.
	 */
	@Test
	public void testRestartAtEndOfFile() throws Exception {

		source.open(executionContext);
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read());
		source.update(executionContext);
		source.close();

		assertEquals(3, executionContext.getInt(ClassUtils.getShortName(StaxEventItemReader.class) + ".read.count"));

		source = createNewInputSource();
		source.open(executionContext);
		assertNull(source.read());
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
			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException();
			}

			@Override
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

		FileSystemResource resource = new FileSystemResource("build/data");
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

	@Test
	public void testDtdXml() {
		String xmlWithDtd = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!DOCTYPE rohit [\n<!ENTITY entityex SYSTEM \"file://" +
				new File("src/test/resources/org/springframework/batch/support/existing.txt").getAbsolutePath() +
				"\">\n]>\n<abc>&entityex;</abc>";
		StaxEventItemReader<String> reader = new StaxEventItemReader<>();
		reader.setName("foo");
		reader.setResource(new ByteArrayResource(xmlWithDtd.getBytes()));
		reader.setUnmarshaller(new MockFragmentUnmarshaller() {
			@Override
			public Object unmarshal(Source source) throws XmlMappingException {
				try {
					XMLEventReader xmlEventReader = StaxTestUtils.getXmlEventReader(source);
					xmlEventReader.nextEvent();
					xmlEventReader.nextEvent();
					return xmlEventReader.getElementText();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		reader.setFragmentRootElementName("abc");

		reader.open(new ExecutionContext());

		try {
			reader.read();
			fail("Should fail when XML contains DTD");
		} catch (Exception e) {
			Assert.assertThat(e.getMessage(), Matchers.containsString("Undeclared general entity \"entityex\""));
		}
	}

	/**
	 * Stub emulating problems during unmarshalling.
	 */
	private static class TroublemakerUnmarshaller implements Unmarshaller {

		public static final String MESSAGE = "Unmarshallers on strike.";

		@Override
		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			throw new UnmarshallingFailureException(MESSAGE);
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

	private StaxEventItemReader<List<XMLEvent>> createNewInputSource() {
		Resource resource = new ByteArrayResource(xml.getBytes());

		StaxEventItemReader<List<XMLEvent>> newSource = new StaxEventItemReader<>();
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setUnmarshaller(unmarshaller);
		newSource.setSaveState(true);

		return newSource;
	}

	private StaxEventItemReader<ItemCountAwareFragment> createNewItemCountAwareInputSource() {
		Resource resource = new ByteArrayResource(xml.getBytes());

		StaxEventItemReader<ItemCountAwareFragment> newSource = new StaxEventItemReader<>();
		newSource.setResource(resource);

		newSource.setFragmentRootElementName(FRAGMENT_ROOT_ELEMENT);
		newSource.setUnmarshaller(new ItemCountAwareMockFragmentUnmarshaller());
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
		private List<XMLEvent> readRecordsInsideFragment(XMLEventReader eventReader, QName fragmentName) throws XMLStreamException {
			XMLEvent eventInsideFragment;
			List<XMLEvent> events = new ArrayList<>();
			do {
				eventInsideFragment = eventReader.peek();
				if (eventInsideFragment instanceof EndElement
						&& fragmentName.equals(((EndElement) eventInsideFragment).getName())) {
					break;
				}
				events.add(eventReader.nextEvent());
			} while (eventInsideFragment != null);

			return events;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		/**
		 * A simple mapFragment implementation checking the StaxEventReaderItemReader basic read functionality.
		 * 
		 * @param source
		 * @return list of the events from fragment body
		 */
		@Override
		public Object unmarshal(Source source) throws XmlMappingException, IOException {

			List<XMLEvent> fragmentContent;
			try {
				XMLEventReader eventReader = StaxTestUtils.getXmlEventReader(source);

				// first event should be StartDocument
				XMLEvent event1 = eventReader.nextEvent();
				assertTrue(event1.isStartDocument());

				// second should be StartElement of the fragment
				XMLEvent event2 = eventReader.nextEvent();
				assertTrue(event2.isStartElement());
				assertTrue(isFragmentRootElement(EventHelper.startElementName(event2)));
				QName fragmentName = ((StartElement) event2).getName();

				// jump before the end of fragment
				fragmentContent = readRecordsInsideFragment(eventReader, fragmentName);

				// end of fragment
				XMLEvent event3 = eventReader.nextEvent();
				assertTrue(event3.isEndElement());
				assertTrue(isFragmentRootElement(EventHelper.endElementName(event3)));

				// EndDocument should follow the end of fragment
				XMLEvent event4 = eventReader.nextEvent();
				assertTrue(event4.isEndDocument());

			}
			catch (Exception e) {
				throw new RuntimeException("Error occurred in FragmentDeserializer", e);
			}
			return fragmentContent;
		}
		
		private boolean isFragmentRootElement(String name) {
			return FRAGMENT_ROOT_ELEMENT.equals(name) || Arrays.asList(MULTI_FRAGMENT_ROOT_ELEMENTS).contains(name);
		}

	}

	@SuppressWarnings("unchecked")
	private static class ItemCountAwareMockFragmentUnmarshaller extends MockFragmentUnmarshaller {
		@Override
		public Object unmarshal(Source source) throws XmlMappingException,
		IOException {
			List<XMLEvent> fragment = (List<XMLEvent>) super.unmarshal(source);
			if(fragment != null) {
				return new ItemCountAwareFragment(fragment);
			} else {
				return null;
			}
		}
	}

	private static class ItemCountAwareFragment implements ItemCountAware {

		private int itemCount;

		public ItemCountAwareFragment(List<XMLEvent> fragment) {
		}

		@Override
		public void setItemCount(int count) {
			this.itemCount = count;
		}

		public int getItemCount() {
			return itemCount;
		}

	}

	private static class MockStaxEventItemReader extends StaxEventItemReader<List<XMLEvent>> {

		private boolean openCalled = false;

		@Override
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

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public String getDescription() {
			return "NonExistantResource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}
	}
}
