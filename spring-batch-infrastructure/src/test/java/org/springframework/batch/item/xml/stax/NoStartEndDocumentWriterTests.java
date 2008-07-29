package org.springframework.batch.item.xml.stax;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;

/**
 * Tests for {@link NoStartEndDocumentStreamWriter}
 * 
 * @author Robert Kasanicky
 */
public class NoStartEndDocumentWriterTests extends TestCase {

	// object under test
	private NoStartEndDocumentStreamWriter writer;

	private XMLEventWriter wrappedWriter;

	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

	protected void setUp() throws Exception {
		wrappedWriter = createStrictMock(XMLEventWriter.class);
		writer = new NoStartEndDocumentStreamWriter(wrappedWriter);
	}

	/**
	 * StartDocument and EndDocument events are not passed to the wrapped
	 * writer.
	 */
	public void testNoStartEnd() throws Exception {
		XMLEvent event = eventFactory.createComment("testEvent");

		// mock expects only a single event
		wrappedWriter.add(event);
		expectLastCall().once();
		replay(wrappedWriter);

		writer.add(eventFactory.createStartDocument());
		writer.add(event);
		writer.add(eventFactory.createEndDocument());

		verify(wrappedWriter);
	}
}
