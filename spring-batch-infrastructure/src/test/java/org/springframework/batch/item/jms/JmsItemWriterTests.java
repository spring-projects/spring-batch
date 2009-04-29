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

package org.springframework.batch.item.jms;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

public class JmsItemWriterTests {

	JmsItemWriter<String> itemWriter = new JmsItemWriter<String>();

	@Test
	public void testNoItemTypeSunnyDay() throws Exception {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		jmsTemplate.convertAndSend("foo");
		EasyMock.expectLastCall();
		jmsTemplate.convertAndSend("bar");
		EasyMock.expectLastCall();
		EasyMock.replay(jmsTemplate);

		itemWriter.setJmsTemplate(jmsTemplate);
		itemWriter.write(Arrays.asList("foo", "bar"));
		EasyMock.verify(jmsTemplate);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testTemplateWithNoDefaultDestination() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate();
		itemWriter.setJmsTemplate(jmsTemplate);		
	}

}
