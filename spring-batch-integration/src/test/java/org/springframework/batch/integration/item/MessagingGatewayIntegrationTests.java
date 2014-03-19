/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test case showing the use of a MessagingGateway to provide an ItemWriter or
 * ItemProcessor to Spring Batch that is hooked directly into a Sprng
 * Integration MessageChannel.
 * 
 * @author Dave Syer
 * 
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessagingGatewayIntegrationTests {

	@Autowired
	private ItemProcessor<String, String> processor;

	@Autowired
	private ItemWriter<String> writer;

	/**
	 * Just for the sake of being able to make assertions.
	 */
	@Autowired
	private EndService service;

	/**
	 * Just for the sake of being able to make assertions.
	 */
	@Autowired
	private SplitService splitter;

	@Test
	public void testProcessor() throws Exception {
		String result = processor.process("foo");
		assertEquals("foo: 0: 1", result);
		assertNull(processor.process("filter"));
	}

	@Test
	public void testWriter() throws Exception {
		writer.write(Arrays.asList("foo", "bar", "spam"));
		assertEquals(3, splitter.count);
		assertEquals(3, service.count);
	}

	/**
	 * This service is wrapped into an ItemProcessor and used to transform
	 * items. This is where the main business processing could take place in a
	 * real application. To suppress output the service activator can just
	 * return null (same as an ItemProcessor) but remember to set the reply
	 * timeout in the gateway.
	 * 
	 * @author Dave Syer
	 * 
	 */
	@MessageEndpoint
	public static class Activator {
		private int count;

		@ServiceActivator
		public String transform(String input) {
			if (input.equals("filter")) {
				return null;
			}
			return input + ": " + (count++);
		}
	}

	/**
	 * The Splitter is wrapped into an ItemWriter and used to relay items to its
	 * output channel. This one is completely trivial, it just passes the items
	 * on as they are. More complex splitters might filter or enhance the items
	 * before passing them on.
	 * 
	 * @author Dave Syer
	 * 
	 */
	@MessageEndpoint
	public static class SplitService {
		// Just for assertions in the test case
		private int count;

		@Splitter
		public List<String> split(List<String> input) {
			count += input.size();
			return input;
		}
	}

	/**
	 * This is just used to trap the messages sent by the ItemWriter and make an
	 * assertion about them in the the test case. In a real application this
	 * would be the output stage and/or business processing.
	 * 
	 * @author Dave Syer
	 * 
	 */
	@MessageEndpoint
	public static class EndService {
		private int count;

		@ServiceActivator
		public void service(String input) {
			count++;
			return;
		}
	}

}
