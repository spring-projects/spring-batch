package org.springframework.batch.integration.async;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AsyncItemProcessorMessagingGatewayTests {

	private AsyncItemProcessor<String, String> processor = new AsyncItemProcessor<String, String>();

	@Autowired
	private ItemProcessor<String, String> delegate;

	@Test
	public void testMultiExecution() throws Exception {
		processor.setDelegate(delegate);
		processor.setTaskExecutor(new SimpleAsyncTaskExecutor());
		List<Future<String>> list = new ArrayList<Future<String>>();
		for (int count = 0; count < 10; count++) {
			list.add(processor.process("foo" + count));
		}
		for (Future<String> future : list) {
			String value = future.get();
			/**
			 * TODO: this delegate is a Spring Integration MessagingGateway. It
			 * can easily return null because of a timeout, but that will be
			 * treated by Batch as a filtered item, whereas it is really more
			 * like a skip. Maybe we should have an option to throw an exception
			 * in the processor if an unexpected null value comes back?
			 */
			assertNotNull(value);
			assertTrue(value.matches("foo.*foo.*"));
		}
	}

	@MessageEndpoint
	public static class Doubler {
		@ServiceActivator
		public String cat(String value) {
			return value + value;
		}
	}

}
