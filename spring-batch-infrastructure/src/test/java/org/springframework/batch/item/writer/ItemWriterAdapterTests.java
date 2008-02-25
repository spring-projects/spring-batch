package org.springframework.batch.item.writer;

import java.util.List;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.io.sample.domain.FooService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for {@link ItemWriterAdapter}.
 * 
 * @author Robert Kasanicky
 */
public class ItemWriterAdapterTests extends AbstractDependencyInjectionSpringContextTests {

	private ItemWriter processor;
	
	private FooService fooService;
	

	protected String getConfigPath() {
		return "delegating-item-writer.xml";
	}

	/**
	 * Regular usage scenario - input object should be passed to
	 * the service the injected invoker points to.
	 */
	public void testProcess() throws Exception {
		Foo foo;
		while ((foo = fooService.generateFoo()) != null) {
			processor.write(foo);
		}
		
		List input = fooService.getGeneratedFoos();
		List processed = fooService.getProcessedFoos();
		assertEquals(input.size(), processed.size());
		assertFalse(fooService.getProcessedFoos().isEmpty());
		
		for (int i = 0; i < input.size(); i++) {
			assertSame(input.get(i), processed.get(i));
		}
		
	}
	
	//setter for auto-injection
	public void setProcessor(ItemWriter processor) {
		this.processor = processor;
	}

	//setter for auto-injection
	public void setFooService(FooService fooService) {
		this.fooService = fooService;
	}
}
