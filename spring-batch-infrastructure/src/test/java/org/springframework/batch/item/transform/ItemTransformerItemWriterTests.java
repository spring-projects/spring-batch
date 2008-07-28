package org.springframework.batch.item.transform;

import static org.junit.Assert.fail;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link ItemTransformerItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class ItemTransformerItemWriterTests {

	private ItemTransformerItemWriter<Object, Object> processor = new ItemTransformerItemWriter<Object, Object>();

	@SuppressWarnings("unchecked")
	private ItemTransformer<Object, Object> transformer = EasyMock.createMock(ItemTransformer.class);
	@SuppressWarnings("unchecked")
	private ItemWriter<Object> itemWriter = EasyMock.createMock(ItemWriter.class);

	@Before
	public void setUp() throws Exception {		
		processor.setItemTransformer(transformer);
		processor.setDelegate(itemWriter);
		processor.afterPropertiesSet();
	}

	/**
	 * Regular usage scenario - item is passed to transformer
	 * and the result of transformation is passed to output source.
	 */
	@Test
	public void testProcess() throws Exception {
		Object item = new Object();
		Object itemAfterTransformation = new Object();

		EasyMock.expect(transformer.transform(item)).andReturn(itemAfterTransformation);
		
		itemWriter.write(itemAfterTransformation);
		EasyMock.expectLastCall();
		
		EasyMock.replay(itemWriter, transformer);

		processor.write(item);

		EasyMock.verify(itemWriter, transformer);
	}
	
	/**
	 * Item transformer must be set.
	 */
	@Test
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
