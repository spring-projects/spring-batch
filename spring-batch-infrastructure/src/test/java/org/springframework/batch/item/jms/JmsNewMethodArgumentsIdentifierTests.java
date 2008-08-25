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

import static org.junit.Assert.assertEquals;

import javax.jms.Message;

import org.easymock.EasyMock;
import org.junit.Test;


/**
 * @author Dave Syer
 *
 */
public class JmsNewMethodArgumentsIdentifierTests {
	
	private JmsNewMethodArgumentsIdentifier<String> newMethodArgumentsIdentifier = new JmsNewMethodArgumentsIdentifier<String>(); 

	@Test
	public void testIsNewForMessage() throws Exception {
		Message message = EasyMock.createMock(Message.class);
		EasyMock.expect(message.getJMSRedelivered()).andReturn(true);
		EasyMock.replay(message);
		assertEquals(false, newMethodArgumentsIdentifier.isNew(new Object[]{message}));
		
		EasyMock.verify(message);
	}

	@Test
	public void testIsNewForNonMessage() throws Exception {
		assertEquals(false, newMethodArgumentsIdentifier.isNew(new Object[]{"foo"}));
	}
}
