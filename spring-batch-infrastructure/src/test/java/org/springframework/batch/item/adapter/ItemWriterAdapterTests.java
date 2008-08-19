package org.springframework.batch.item.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.sample.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link ItemWriterAdapter}.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "delegating-item-writer.xml")
public class ItemWriterAdapterTests {

	@Autowired
	private ItemWriter<Foo> processor;

	@Autowired
	private FooService fooService;

	/*
	 * Regular usage scenario - input object should be passed to the service the injected invoker points to.
	 */
	@Test
	public void testProcess() throws Exception {
		Foo foo;
		List<Foo> foos = new ArrayList<Foo>();
		while ((foo = fooService.generateFoo()) != null) {
			foos.add(foo);
		}
		processor.write(foos);

		List<Foo> input = fooService.getGeneratedFoos();
		List<Foo> processed = fooService.getProcessedFoos();
		assertEquals(input.size(), processed.size());
		assertFalse(fooService.getProcessedFoos().isEmpty());

		for (int i = 0; i < input.size(); i++) {
			assertSame(input.get(i), processed.get(i));
		}

	}

}
