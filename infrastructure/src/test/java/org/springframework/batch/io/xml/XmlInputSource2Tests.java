package org.springframework.batch.io.xml;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Unit tests for {@link XmlInputSource2}
 * 
 * @author Robert Kasanicky
 */
public class XmlInputSource2Tests extends TestCase {

	private XmlInputSource2 inputSource = new XmlInputSource2();
	
	
	private Resource getInputResource() throws IOException {
		return new FileSystemResource("src/test/resources/org/springframework/batch/io/xml/test1.xml");
	}
	
	//@Override
	protected void setUp() throws Exception {
		inputSource.setRecordElementName("book");
		inputSource.setResource(getInputResource());
		inputSource.setUnmarshaller(new UnmarshallerStub());
		inputSource.setUseSaxParser(true);
	}



	/**
	 * Regular usage scenario.
	 * The actual xml-to-object mapping is delegated to the injected unmarshaller.
	 */
	public void testRead() throws XmlMappingException, IOException {
		MockControl umControl = MockControl.createControl(Unmarshaller.class);
		Unmarshaller unmarshaller = (Unmarshaller) umControl.getMock();
		Object expectedDomainObject = new Object();
		unmarshaller.unmarshal(null);
		umControl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		umControl.setDefaultReturnValue(expectedDomainObject);
		umControl.replay();
		
		inputSource.setUnmarshaller(unmarshaller);
		
		//there are two records in the input file
		assertSame(expectedDomainObject, inputSource.read());
		assertSame(expectedDomainObject, inputSource.read());
		assertNull(inputSource.read());
	}
	
	public void testReadUntilEnd() {
		
	}
	
	/**
	 * In case of rollback uncommited records are read again.
	 */
	public void testRollback() {
		Object uncommited = inputSource.read();
		inputSource.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		Object afterRollback = inputSource.read();
		
		assertEquals(uncommited, afterRollback);
	}
	
	/**
	 * Records once marked to be skipped are not returned when read again.
	 */
	public void testSkip() {
		Object first = inputSource.read();
		inputSource.skip();
		inputSource.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		
		Object second = inputSource.read();
		assertFalse(second.equals(first));
	}
	
	/**
	 * In case of restart the input source should continue from the position when restart data was saved.
	 */
	public void testRestart() {
		inputSource.read();
		RestartData commitPoint = inputSource.getRestartData();
		Object firstAfterCommit = inputSource.read();
		inputSource.restoreFrom(commitPoint);
		assertEquals(firstAfterCommit, inputSource.read());
		
	}
	
	/**
	 * Returns a fixed-length prefix of the original xml string instead of mapped object.
	 * 
	 * @author Robert Kasanicky
	 */
	private static class UnmarshallerStub implements Unmarshaller {

		private static final int PREFIX_LENGTH = 10000;
		
		public boolean supports(Class clazz) {
			return true;
		}

		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			char[] input = new char[PREFIX_LENGTH];  
			SAXSource saxSource = (SAXSource) source;
			saxSource.getInputSource().getCharacterStream().read(input);
			
			return String.valueOf(input);
		}
		
	}
}
