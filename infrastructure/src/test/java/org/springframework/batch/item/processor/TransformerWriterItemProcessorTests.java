package org.springframework.batch.item.processor;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.ItemWriter;

/**
 * Tests for {@link TransformerWriterItemProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class TransformerWriterItemProcessorTests extends TestCase {

	private TransformerWriterItemProcessor processor = new TransformerWriterItemProcessor();

	private ItemTransformer transformer;
	private ItemWriter outputSource;

	private MockControl tControl = MockControl.createControl(ItemTransformer.class);
	private MockControl outControl = MockControl.createControl(ItemWriter.class);

	protected void setUp() throws Exception {
		transformer = (ItemTransformer) tControl.getMock();
		outputSource = (ItemWriter) outControl.getMock();
		
		processor.setItemTransformer(transformer);
		processor.setItemWriter(outputSource);
		
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
		
		outputSource.write(itemAfterTransformation);
		outControl.setVoidCallable();
		
		tControl.replay();
		outControl.replay();

		processor.process(item);

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
