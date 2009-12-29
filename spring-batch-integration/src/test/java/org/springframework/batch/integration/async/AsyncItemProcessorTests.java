package org.springframework.batch.integration.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class AsyncItemProcessorTests {

	private AsyncItemProcessor<String, String> processor = new AsyncItemProcessor<String, String>();

	private ItemProcessor<String, String> delegate = new ItemProcessor<String, String>() {
		public String process(String item) throws Exception {
			return item + item;
		};
	};

	@Test(expected = IllegalArgumentException.class)
	public void testNoDelegate() throws Exception {
		processor.afterPropertiesSet();
	}

	@Test
	public void testExecution() throws Exception {
		processor.setDelegate(delegate);
		Future<String> result = processor.process("foo");
		assertEquals("foofoo", result.get());
	}

	@Test
	public void testMultiExecution() throws Exception {
		processor.setDelegate(delegate);
		processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
		List<Future<String>> list = new ArrayList<Future<String>>();
		for (int count = 0; count < 10; count++) {
			list.add(processor.process("foo" + count));
		}
		for (Future<String> future : list) {
			assertTrue(future.get().matches("foo.*foo.*"));
		}
	}

}
