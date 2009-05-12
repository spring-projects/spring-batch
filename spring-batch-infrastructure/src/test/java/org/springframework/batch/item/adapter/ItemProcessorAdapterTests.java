package org.springframework.batch.item.adapter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link ItemProcessorAdapter}.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "delegating-item-processor.xml")
public class ItemProcessorAdapterTests {

	@Autowired
	private ItemProcessorAdapter<Foo,String> processor;

	@Test
	public void testProcess() throws Exception {
		Foo item = new Foo(0,"foo",1);
		assertEquals("foo", processor.process(item));
	}

}
