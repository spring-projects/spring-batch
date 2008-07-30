package org.springframework.batch.item.adapter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.sample.FooService;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ItemReaderAdapter}.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "delegating-item-provider.xml")
public class ItemReaderAdapterTests {

	@Autowired
	private ItemReaderAdapter<Foo> provider;

	@Autowired
	private FooService fooService;

	/*
	 * Regular usage scenario - items are retrieved from the service injected invoker points to.
	 */
	@Test
	public void testNext() throws Exception {
		List<Object> returnedItems = new ArrayList<Object>();
		Object item;
		while ((item = provider.read()) != null) {
			returnedItems.add(item);
		}

		List<Foo> input = fooService.getGeneratedFoos();
		assertEquals(input.size(), returnedItems.size());
		assertFalse(returnedItems.isEmpty());

		for (int i = 0; i < input.size(); i++) {
			assertSame(input.get(i), returnedItems.get(i));
		}
	}

}
