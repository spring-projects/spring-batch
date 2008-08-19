package org.springframework.batch.item.adapter;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.sample.FooService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for {@link PropertyExtractingDelegatingItemWriter}
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "pe-delegating-item-writer.xml")
public class PropertyExtractingDelegatingItemProccessorIntegrationTests {

	@Autowired
	private PropertyExtractingDelegatingItemWriter<Foo> processor;

	@Autowired
	private FooService fooService;

	/*
	 * Regular usage scenario - input object should be passed to the service the injected invoker points to.
	 */
	@Test
	public void testProcess() throws Exception {
		Foo foo;
		while ((foo = fooService.generateFoo()) != null) {
			processor.write(Collections.singletonList(foo));
		}

		List<Foo> input = fooService.getGeneratedFoos();
		List<Foo> processed = fooService.getProcessedFooNameValuePairs();
		assertEquals(input.size(), processed.size());
		assertFalse(fooService.getProcessedFooNameValuePairs().isEmpty());

		for (int i = 0; i < input.size(); i++) {
			Foo inputFoo = input.get(i);
			Foo outputFoo = processed.get(i);
			assertEquals(inputFoo.getName(), outputFoo.getName());
			assertEquals(inputFoo.getValue(), outputFoo.getValue());
			assertEquals(0, outputFoo.getId());
		}

	}

}
