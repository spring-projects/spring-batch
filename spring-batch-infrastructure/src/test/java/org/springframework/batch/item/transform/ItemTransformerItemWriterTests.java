package org.springframework.batch.item.transform;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link ItemTransformerItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class ItemTransformerItemWriterTests extends TestCase {

	private ItemTransformerItemWriter<Object, Object> processor = new ItemTransformerItemWriter<Object, Object>();

	private ItemTransformer<Object, Object> transformer;
	private ItemWriter<Object> itemWriter;

	private MockControl<ItemTransformer> tControl = MockControl.createControl(ItemTransformer.class);
	private MockControl<ItemWriter> outControl = MockControl.createControl(ItemWriter.class);

	protected void setUp() throws Exception {
		transformer = tControl.getMock();
		itemWriter = outControl.getMock();
		
		processor.setItemTransformer(transformer);
		processor.setDelegate(itemWriter);
		processor.afterPropertiesSet();
	}

	/**
	 * Regular usage scenario - item is passed to transformer
	 * and the result of transformation is passed to output source.
	 */
	public void testProcess() throws Exception {
		Object item = new Object();
		Object itemAfterTransformation = new Object();

		transformer.transform(item);
		tControl.setReturnValue(itemAfterTransformation);
		
		itemWriter.write(itemAfterTransformation);
		outControl.setVoidCallable();
		
		tControl.replay();
		outControl.replay();

		processor.write(item);

		tControl.verify();
		outControl.verify();
	}
	
	/**
	 * Item transformer must be set.
	 */
	public void testAfterPropertiesSet() throws Exception {
		
		// value not set
		processor.setItemTransformer(null);
		try {
			processor.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
