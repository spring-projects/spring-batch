package org.springframework.batch.item.processor;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.OutputSource;

/**
 * Tests for {@link TransformerOutputSourceItemProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class TransformerOutputSourceItemProcessorTests extends TestCase {

	private TransformerOutputSourceItemProcessor processor = new TransformerOutputSourceItemProcessor();

	private ItemTransformer transformer;
	private OutputSource outputSource;

	private MockControl tControl = MockControl.createControl(ItemTransformer.class);
	private MockControl outControl = MockControl.createControl(OutputSource.class);

	protected void setUp() throws Exception {
		transformer = (ItemTransformer) tControl.getMock();
		outputSource = (OutputSource) outControl.getMock();
		
		processor.setItemTransformer(transformer);
		processor.setOutputSource(outputSource);
		
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
