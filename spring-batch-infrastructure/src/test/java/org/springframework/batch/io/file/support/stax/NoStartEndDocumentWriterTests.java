package org.springframework.batch.io.file.support.stax;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.events.XMLEvent;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.file.support.stax.NoStartEndDocumentStreamWriter;

/**
 * Tests for {@link NoStartEndDocumentStreamWriter}
 * 
 * @author Robert Kasanicky
 */
public class NoStartEndDocumentWriterTests extends TestCase {

	// object under test
	private NoStartEndDocumentStreamWriter writer;
	
	private XMLEventWriter wrappedWriter;
	private MockControl wrappedWriterControl = MockControl.createStrictControl(XMLEventWriter.class);
	
	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	
	
	protected void setUp() throws Exception {
		wrappedWriter = (XMLEventWriter) wrappedWriterControl.getMock();
		writer = new NoStartEndDocumentStreamWriter(wrappedWriter);
	}


	/**
	 * StartDocument and EndDocument events are not passed to the wrapped writer.
	 */
	public void testNoStartEnd() throws Exception {
		XMLEvent event = eventFactory.createComment("testEvent");
		
		//mock expects only a single event
		wrappedWriter.add(event);
		wrappedWriterControl.setVoidCallable();
		wrappedWriterControl.replay();
		
		writer.add(eventFactory.createStartDocument());
		writer.add(event);
		writer.add(eventFactory.createEndDocument());
		
		wrappedWriterControl.verify();
	}
}
