package org.springframework.batch.item.xml.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.oxm.Unmarshaller;

/**
 * Tests for {@link UnmarshallingEventReaderDeserializer}
 * 
 * @author Robert Kasanicky
 */
public class UnmarshallingFragmentDeserializerTests extends TestCase {

	// object under test
	private UnmarshallingEventReaderDeserializer<?> deserializer;
	
	private XMLEventReader eventReader;
	private String xml = "<root> </root>";
	
	private Unmarshaller unmarshaller;
	
	

	protected void setUp() throws Exception {
		Resource input = new ByteArrayResource(xml.getBytes());
		eventReader = XMLInputFactory.newInstance().createXMLEventReader(input.getInputStream());
		unmarshaller = createMock(Unmarshaller.class);
		//unmarshallerControl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		deserializer = new UnmarshallingEventReaderDeserializer<Object>(unmarshaller);
	}

	/**
	 * Regular scenario when deserializer returns the object provided by Unmarshaller
	 */
	public void testSuccessfulDeserialization() throws Exception {
		Object expectedResult = new Object();
		expect(unmarshaller.unmarshal(isA(Source.class))).andReturn(expectedResult);
		replay(unmarshaller);
		
		Object result = deserializer.deserializeFragment(eventReader);
		
		assertEquals(expectedResult, result);
		
		verify(unmarshaller);
	}
	
	/**
	 * Appropriate exception rethrown in case of failure.
	 */
	public void testFailedDeserialization() throws Exception {
		expect(unmarshaller.unmarshal(isA(Source.class))).andThrow(new IOException());
		replay(unmarshaller);
		
		try {
			deserializer.deserializeFragment(eventReader);
			fail("Exception expected");
		}
		catch (DataAccessException e) {
			// expected
		}
		
		verify(unmarshaller);
	}
	
	/**
	 * It makes no sense to create UnmarshallingFragmentDeserializer with null Unmarshaller,
	 * therefore it should cause exception.
	 */
	public void testExceptionOnNullUnmarshaller() {
		try {
			deserializer = new UnmarshallingEventReaderDeserializer<Object>(null);
			fail("Exception expected");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
	}
}
