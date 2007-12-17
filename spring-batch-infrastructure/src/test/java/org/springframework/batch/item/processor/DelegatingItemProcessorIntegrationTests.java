package org.springframework.batch.item.processor;

import java.util.List;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.io.sample.domain.FooService;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for {@link ItemProcessorAdapter}.
 * 
 * @author Robert Kasanicky
 */
public class DelegatingItemProcessorIntegrationTests extends AbstractDependencyInjectionSpringContextTests {

	private ItemProcessorAdapter processor;
	
	private FooService fooService;
	

	protected String getConfigPath() {
		return "delegating-item-processor.xml";
	}

	/**
	 * Regular usage scenario - input object should be passed to
	 * the service the injected invoker points to.
	 */
	public void testProcess() throws Exception {
		Foo foo;
		while ((foo = fooService.generateFoo()) != null) {
			processor.process(foo);
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
	public void setProcessor(ItemProcessorAdapter processor) {
		this.processor = processor;
	}

	//setter for auto-injection
	public void setFooService(FooService fooService) {
		this.fooService = fooService;
	}
}
