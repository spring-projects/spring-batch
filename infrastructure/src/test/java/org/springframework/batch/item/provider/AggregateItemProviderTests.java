package org.springframework.batch.item.provider;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.file.FieldSetMapper;

public class AggregateItemProviderTests extends TestCase {

	private MockControl inputControl;
	private InputSource input;
	private AggregateItemProvider provider;

	public void setUp() {

		//create mock for input
		inputControl = MockControl.createControl(InputSource.class);
		input = (InputSource) inputControl.getMock();

		//create provider
		provider = new AggregateItemProvider();
		provider.setInputSource(input);
	}

	public void testNext() {

		//set-up mock input
		input.read();
		inputControl.setReturnValue(FieldSetMapper.BEGIN_RECORD);
		input.read();
		inputControl.setReturnValue("line",3);
		input.read();
		inputControl.setReturnValue(FieldSetMapper.END_RECORD);
		input.read();
		inputControl.setReturnValue(null);
		inputControl.replay();

		//read object
		Object result = provider.next();

		//it should be collection of 3 strings "line"
		assertTrue(result instanceof Collection);
		Collection lines = (Collection)result;
		assertEquals(3, lines.size());

		for (Iterator i = lines.iterator(); i.hasNext();) {
			assertEquals("line", i.next());
		}

		//read object again - it should return null
		assertNull(provider.next());

		//verify method calls
		inputControl.verify();
	}
}
