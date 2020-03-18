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

import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

public class JmsItemWriterTests {

	JmsItemWriter<String> itemWriter = new JmsItemWriter<>();

	@Test
	public void testNoItemTypeSunnyDay() throws Exception {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		jmsTemplate.convertAndSend("foo");
		jmsTemplate.convertAndSend("bar");

		itemWriter.setJmsTemplate(jmsTemplate);
		itemWriter.write(Arrays.asList("foo", "bar"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTemplateWithNoDefaultDestination() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate();
		itemWriter.setJmsTemplate(jmsTemplate);		
	}

}
