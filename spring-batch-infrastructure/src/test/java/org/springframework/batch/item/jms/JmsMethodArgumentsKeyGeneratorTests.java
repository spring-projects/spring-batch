/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.jms;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jms.Message;

import org.junit.Test;


/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
public class JmsMethodArgumentsKeyGeneratorTests {
	
	private JmsMethodArgumentsKeyGenerator methodArgumentsKeyGenerator = new JmsMethodArgumentsKeyGenerator(); 

	@Test
	public void testGetKeyFromMessage() throws Exception {
		Message message = mock(Message.class);
		when(message.getJMSMessageID()).thenReturn("foo");

		JmsItemReader<Message> itemReader = new JmsItemReader<>();
		itemReader.setItemType(Message.class);
		assertEquals("foo", methodArgumentsKeyGenerator.getKey(new Object[]{message}));

	}

	@Test
	public void testGetKeyFromNonMessage() throws Exception {
		assertEquals("foo", methodArgumentsKeyGenerator.getKey(new Object[]{"foo"}));
	}

}
