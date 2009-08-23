package org.springframework.batch.item.support;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;

/**
 * Tests for {@link CompositeItemProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemProcessorTests {

	private CompositeItemProcessor<Object, Object> composite = new CompositeItemProcessor<Object, Object>();
	
	private ItemProcessor<Object, Object> processor1;
	private ItemProcessor<Object, Object> processor2;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		processor1 = createMock(ItemProcessor.class);
		processor2 = createMock(ItemProcessor.class);
		
		composite.setDelegates(new ArrayList<ItemProcessor<Object,Object>>() {{ 
			add(processor1); add(processor2); 
		}});
		
		composite.afterPropertiesSet();
	}
	
	/**
	 * Regular usage scenario - item is passed through the processing chain,
	 * return value of the of the last transformation is returned by the composite.
	 */
	@Test
	public void testTransform() throws Exception {
		Object item = new Object();
		Object itemAfterFirstTransfromation = new Object();
		Object itemAfterSecondTransformation = new Object();

		expect(processor1.process(item)).andReturn(itemAfterFirstTransfromation);
		
		expect(processor2.process(itemAfterFirstTransfromation)).andReturn(itemAfterSecondTransformation);
		
		replay(processor1);
		replay(processor2);
		
		assertSame(itemAfterSecondTransformation, composite.process(item));

		verify(processor1);
		verify(processor2);
	}
	
	/**
	 * The list of transformers must not be null or empty and 
	 * can contain only instances of {@link ItemProcessor}.
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		
		// value not set
		composite.setDelegates(null);
		try {
			composite.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
		// empty list
		composite.setDelegates(new ArrayList<ItemProcessor<Object,Object>>());
		try {
			composite.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
	}
	
	@Test
	public void testFilteredItemInFirstProcessor() throws Exception{
		
		Object item = new Object();
		expect(processor1.process(item)).andReturn(null);
		replay(processor1, processor2);
		Assert.assertEquals(null,composite.process(item));
		verify(processor1,processor2);
	}
}
