package org.springframework.batch.io.xml.oxm;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.xml.oxm.UnmarshallingFragmentDeserializer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.oxm.Unmarshaller;

/**
 * Tests for {@link UnmarshallingFragmentDeserializer}
 * 
 * @author Robert Kasanicky
 */
public class UnmarshallingFragmentDeserializerTests extends TestCase {

	// object under test
	private UnmarshallingFragmentDeserializer deserializer;
	
	private XMLEventReader eventReader;
	private String xml = "<root> </root>";
	
	private Unmarshaller unmarshaller;
	private MockControl unmarshallerControl = MockControl.createStrictControl(Unmarshaller.class);
	
	

	protected void setUp() throws Exception {
		Resource input = new ByteArrayResource(xml.getBytes());
		eventReader = XMLInputFactory.newInstance().createXMLEventReader(input.getInputStream());
		unmarshaller = (Unmarshaller) unmarshallerControl.getMock();
		unmarshallerControl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		deserializer = new UnmarshallingFragmentDeserializer(unmarshaller);
	}

	/**
	 * Regular scenario when deserializer returns the object provided by Unmarshaller
	 */
	public void testSuccessfulDeserialization() throws Exception {
		Object expectedResult = new Object();
		unmarshaller.unmarshal(null);
		unmarshallerControl.setReturnValue(expectedResult);
		unmarshallerControl.replay();
		
		Object result = deserializer.deserializeFragment(eventReader);
		
		assertEquals(expectedResult, result);
		
		unmarshallerControl.verify();
	}
	
	/**
	 * Appropriate exception rethrown in case of failure.
	 */
	public void testFailedDeserialization() throws Exception {
		unmarshaller.unmarshal(null);
		unmarshallerControl.setThrowable(new IOException());
		unmarshallerControl.replay();
		
		try {
			deserializer.deserializeFragment(eventReader);
			fail("Exception expected");
		}
		catch (DataAccessException e) {
			// expected
		}
		
		unmarshallerControl.verify();
	}
	
	/**
	 * It makes no sense to create UnmarshallingFragmentDeserializer with null Unmarshaller,
	 * therefore it should cause exception.
	 */
	public void testExceptionOnNullUnmarshaller() {
		try {
			deserializer = new UnmarshallingFragmentDeserializer(null);
			fail("Exception expected");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
	}
}
