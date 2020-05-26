/*
 * Copyright 2017-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.jms.builder;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.jms.core.JmsOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
public class JmsItemWriterBuilderTests {

	@Test
	public void testNoItem() throws Exception {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		JmsItemWriter<String> itemWriter = new JmsItemWriterBuilder<String>().jmsTemplate(jmsTemplate).build();
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		itemWriter.write(Arrays.asList("foo", "bar"));
		verify(jmsTemplate, times(2)).convertAndSend(argCaptor.capture());
		assertEquals("Expected foo", "foo", argCaptor.getAllValues().get(0));
		assertEquals("Expected bar", "bar", argCaptor.getAllValues().get(1));
	}
	
	@Test
	public void testNullJmsTemplate() {
		try {
			new JmsItemWriterBuilder<String>().build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException ise) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"jmsTemplate is required.", ise.getMessage());
		}
	}
}
