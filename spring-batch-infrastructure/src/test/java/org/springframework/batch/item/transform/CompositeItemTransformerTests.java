package org.springframework.batch.item.transform;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.easymock.MockControl;

/**
 * Tests for {@link CompositeItemTransformer}.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemTransformerTests extends TestCase {

	private CompositeItemTransformer<Object, Object> composite = new CompositeItemTransformer<Object, Object>();
	
	private ItemTransformer<Object, Object> transformer1;
	private ItemTransformer<Object, Object> transformer2;

	private MockControl<ItemTransformer> tControl1 = MockControl.createControl(ItemTransformer.class);
	private MockControl<ItemTransformer> tControl2 = MockControl.createControl(ItemTransformer.class);
	
	protected void setUp() throws Exception {
		transformer1 = tControl1.getMock();
		transformer2 = tControl2 .getMock();
		
		composite.setItemTransformers(new ArrayList<ItemTransformer>() {{ 
			add(transformer1); add(transformer2); 
		}});
		
		composite.afterPropertiesSet();
	}
	
	/**
	 * Regular usage scenario - item is passed through the processing chain,
	 * return value of the of the last transformation is returned by the composite.
	 */
	public void testTransform() throws Exception {
		Object item = new Object();
		Object itemAfterFirstTransfromation = new Object();
		Object itemAfterSecondTransformation = new Object();

		transformer1.transform(item);
		tControl1.setReturnValue(itemAfterFirstTransfromation);
		
		transformer2.transform(itemAfterFirstTransfromation);
		tControl2.setReturnValue(itemAfterSecondTransformation);
		
		tControl1.replay();
		tControl2.replay();

		assertSame(itemAfterSecondTransformation, composite.transform(item));

		tControl1.verify();
		tControl2.verify();
	}
	
	/**
	 * The list of transformers must not be null or empty and 
	 * can contain only instances of {@link ItemTransformer}.
	 */
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
