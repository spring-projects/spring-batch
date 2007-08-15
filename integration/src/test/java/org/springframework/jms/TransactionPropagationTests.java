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

package org.springframework.jms;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

public class TransactionPropagationTests extends AbstractDependencyInjectionSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.classPackageAsResourcePath(getClass()) + "/tx.xml" };
	}

	private JmsTemplate jmsTemplate;

	private PlatformTransactionManager transactionManager;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		jmsTemplate.convertAndSend("queue", "spam");
	}

	List list = new ArrayList();

	public void testRollbackOuterTransaction() throws Exception {

		final DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				TransactionDefinition.PROPAGATION_MANDATORY);

		try {

			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {

					new TransactionTemplate(transactionManager, transactionDefinition)
							.execute(new TransactionCallback() {
								public Object doInTransaction(TransactionStatus status) {
									String msg = (String) jmsTemplate.receiveAndConvert("queue");
									list.add(msg);
									return null;
								}
							});

					new TransactionTemplate(transactionManager, transactionDefinition)
							.execute(new TransactionCallback() {
								public Object doInTransaction(TransactionStatus status) {
									String msg = (String) jmsTemplate.receiveAndConvert("queue");
									list.add(msg);
									throw new RuntimeException("Rollback!");
								}
							});

					return null;
				}
			});

			fail("Expected RuntimeException");

		}
		catch (RuntimeException e) {
			// Expected
			assertEquals("Rollback!", e.getMessage());
		}

		List msgs = getMessages();
		System.err.println(list);
		System.err.println(msgs);

		// 2 received
		assertEquals(2, list.size());
		// but both rolled back...
		assertEquals(3, msgs.size());
	}

	public void testRollbackSingleTransaction() throws Exception {

		try {

			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {

					String msg = (String) jmsTemplate.receiveAndConvert("queue");
					list.add(msg);
					msg = (String) jmsTemplate.receiveAndConvert("queue");
					list.add(msg);
					throw new RuntimeException("Rollback!");

				}
			});

			fail("Expected RuntimeException");

		}
		catch (RuntimeException e) {
			// Expected
			assertEquals("Rollback!", e.getMessage());
		}

		List msgs = getMessages();
		System.err.println(list);
		System.err.println(msgs);

		// 2 received
		assertEquals(2, list.size());
		// but both rolled back...
		assertEquals(3, msgs.size());
	}

	private List getMessages() {
		String next = "";
		List msgs = new ArrayList();
		while (next != null) {
			next = (String) jmsTemplate.receiveAndConvert("queue");
			if (next != null)
				msgs.add(next);
		}
		return msgs;
	}
}
