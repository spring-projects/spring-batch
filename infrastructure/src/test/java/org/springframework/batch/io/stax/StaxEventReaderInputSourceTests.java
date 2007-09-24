package org.springframework.batch.io.stax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Tests for {@link StaxEventReaderInputSource}.
 * 
 * @author Robert Kasanicky
 */
public class StaxEventReaderInputSourceTests extends TestCase {

	// object under test
	private StaxEventReaderInputSource source;

	// test xml input
	private String xml = "<root> <fragment> <misc1/> </fragment> <misc2/> <fragment> testString </fragment> </root>";

	private FragmentDeserializer deserializer = new FragmentDeserializerMock();

	private static final String FRAGMENT_ROOT_ELEMENT = "fragment";

	protected void setUp() throws Exception {
		source = createNewInputSouce();
	}

	/**
	 * InputSource should pass XML fragments to deserializer wrapped with
	 * StartDocument and EndDocument events.
	 */
	public void testFragmentWrapping() {
		// see asserts in the mock deserializer
		assertNotNull(source.read());
		assertNotNull(source.read());
		assertNull(source.read()); // there are only two fragments
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
		RestartData restartData = source.getRestartData();
		List expectedAfterRestart = (List) source.read();

		source = createNewInputSouce();
		source.restoreFrom(restartData);
		List afterRestart = (List) source.read();
		assertEquals(expectedAfterRestart.size(), afterRestart.size());
	}

	/**
	 * Skipping marked records after rollback.
	 */
	public void testSkip() {
		List first = (List) source.read();
		source.skip();
		List second = (List) source.read();
		assertFalse(first.equals(second));
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		assertEquals(second, source.read());
	}

	/**
	 * Rollback to last commited record. 
	 */
	public void testRollback() {
		
		//rollback between deserializing records
		List first = (List) source.read();
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		List second = (List) source.read();
		assertFalse(first.equals(second));
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		assertEquals(second, source.read());
		
		
		//rollback while deserializing record
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		source.setFragmentDeserializer(new ExceptionFragmentDeserializer());
		try {
			source.read();
		}
		catch (Exception expected) {
			source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		}
		source.setFragmentDeserializer(deserializer);
	
		assertEquals(second, source.read());
	}

	/**
	 * Statistics return the current record count. Calling read after end of
	 * input does not increase the counter.
	 */
	public void testStatistics() {
		final int NUMBER_OF_RECORDS = 2;

		for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
			int recordCount = extractRecordCountFrom(source.getStatistics());
			assertEquals(i, recordCount);
			source.read();
		}

		assertEquals(NUMBER_OF_RECORDS, extractRecordCountFrom(source.getStatistics()));
		source.read();
		assertEquals(NUMBER_OF_RECORDS, extractRecordCountFrom(source.getStatistics()));
	}

	private int extractRecordCountFrom(Properties statistics) {
		return Integer.valueOf(
				source.getStatistics().getProperty(StaxEventReaderInputSource.READ_COUNT_STATISTICS_NAME)).intValue();
	}

	private StaxEventReaderInputSource createNewInputSouce() {
		Resource resource = new ByteArrayResource(xml.getBytes());

		StaxEventReaderInputSource newSource = new StaxEventReaderInputSource();
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
	private static class FragmentDeserializerMock implements FragmentDeserializer {

		/**
		 * A simple mapFragment implementation checking the
		 * StaxEventReaderInputSource basic read functionality.
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
	private class ExceptionFragmentDeserializer implements FragmentDeserializer {

		public Object deserializeFragment(XMLEventReader eventReader) {
			eventReader.next();
			throw new RuntimeException();
		}

	}

}
