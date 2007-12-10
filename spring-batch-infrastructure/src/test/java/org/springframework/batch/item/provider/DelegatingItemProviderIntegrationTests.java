package org.springframework.batch.item.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.io.sample.domain.FooService;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for {@link DelegatingItemProvider}.
 * 
 * @author Robert Kasanicky
 */
public class DelegatingItemProviderIntegrationTests extends AbstractDependencyInjectionSpringContextTests {

	private DelegatingItemProvider provider;
	
	private FooService fooService;
	

	protected String getConfigPath() {
		return "delegating-item-provider.xml";
	}
	
	/**
	 * Regular usage scenario - items are retrieved from
	 * the service injected invoker points to.
	 */ 
	public void testNext() throws Exception {
		List returnedItems = new ArrayList();
		Object item;
		while ((item = provider.next()) != null) {
			returnedItems.add(item);
		}
		
		List input = fooService.getGeneratedFoos();
		assertEquals(input.size(), returnedItems.size());
		assertFalse(returnedItems.isEmpty());
		
		for (int i = 0; i<input.size(); i++) {
			assertSame(input.get(i), returnedItems.get(i));
		}
	}
	
	/**
	 * getKey(..) is implemented trivially.
	 */
	public void testGetKey() {
		Object item = new Object();
		assertSame(item, provider.getKey(item));
	}
	
	/**
	 * Recover not supported.
	 */
	public void testRecover() {
		assertFalse(provider.recover(null, null));
	}

	public void setProvider(DelegatingItemProvider provider) {
		this.provider = provider;
	}

	public void setFooService(FooService fooService) {
		this.fooService = fooService;
	}
	
	
}
