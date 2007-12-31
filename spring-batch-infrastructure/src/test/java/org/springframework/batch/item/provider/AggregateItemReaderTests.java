package org.springframework.batch.item.provider;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemReader;

public class AggregateItemReaderTests extends TestCase {

	private MockControl inputControl;
	private ItemReader input;
	private AggregateItemReader provider;

	public void setUp() {

		//create mock for input
		inputControl = MockControl.createControl(ItemReader.class);
		input = (ItemReader) inputControl.getMock();

		//create provider
		provider = new AggregateItemReader();
		provider.setItemReader(input);
	}

	public void testNext() throws Exception {

		//set-up mock input
		input.read();
		inputControl.setReturnValue(AggregateItemReader.BEGIN_RECORD);
		input.read();
		inputControl.setReturnValue("line",3);
		input.read();
		inputControl.setReturnValue(AggregateItemReader.END_RECORD);
		input.read();
		inputControl.setReturnValue(null);
		inputControl.replay();

		//read object
		Object result = provider.read();

		//it should be collection of 3 strings "line"
		assertTrue(result instanceof Collection);
		Collection lines = (Collection)result;
		assertEquals(3, lines.size());

		for (Iterator i = lines.iterator(); i.hasNext();) {
			assertEquals("line", i.next());
		}

		//read object again - it should return null
		assertNull(provider.read());

		//verify method calls
		inputControl.verify();
	}
}
