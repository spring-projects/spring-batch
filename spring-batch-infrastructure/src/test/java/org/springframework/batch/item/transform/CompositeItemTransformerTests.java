package org.springframework.batch.item.transform;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CompositeItemTransformer}.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemTransformerTests {

	private CompositeItemTransformer<Object, Object> composite = new CompositeItemTransformer<Object, Object>();
	
	private ItemTransformer<Object, Object> transformer1;
	private ItemTransformer<Object, Object> transformer2;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		transformer1 = createMock(ItemTransformer.class);
		transformer2 = createMock(ItemTransformer.class);
		
		composite.setItemTransformers(new ArrayList<ItemTransformer>() {{ 
			add(transformer1); add(transformer2); 
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

		expect(transformer1.transform(item)).andReturn(itemAfterFirstTransfromation);
		
		expect(transformer2.transform(itemAfterFirstTransfromation)).andReturn(itemAfterSecondTransformation);
		
		replay(transformer1);
		replay(transformer2);
		
		assertSame(itemAfterSecondTransformation, composite.transform(item));

		verify(transformer1);
		verify(transformer2);
	}
	
	/**
	 * The list of transformers must not be null or empty and 
	 * can contain only instances of {@link ItemTransformer}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAfterPropertiesSet() throws Exception {
		
		// value not set
		composite.setItemTransformers(null);
		try {
			composite.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
		// empty list
		composite.setItemTransformers(new ArrayList<ItemTransformer>());
		try {
			composite.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		
	}
}
