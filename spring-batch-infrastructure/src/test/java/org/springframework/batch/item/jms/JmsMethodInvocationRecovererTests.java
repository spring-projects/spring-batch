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

import org.junit.Test;
import org.springframework.jms.core.JmsOperations;

/**
 * @author Dave Syer
 * @author Will Schipp
 * 
 */
public class JmsMethodInvocationRecovererTests {

	private JmsMethodInvocationRecoverer<String> itemReader = new JmsMethodInvocationRecoverer<>();

	@Test
	public void testRecoverWithNoDestination() throws Exception {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		jmsTemplate.convertAndSend("foo");

		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.recover(new Object[] { "foo" }, null);

	}

}
