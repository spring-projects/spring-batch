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

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.connection.SessionProxy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

public class SynchronousTests extends AbstractTransactionalDataSourceSpringContextTests {

	private JmsTemplate jmsTemplate;

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.classPackageAsResourcePath(getClass()) + "/synch.xml" };
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		jdbcTemplate.execute("delete from T_FOOS");
		jmsTemplate.convertAndSend("queue", "foo");
	}

	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
	}

	private void assertInitialState() {
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);
	}

	public void testCommit() throws Exception {

		assertInitialState();
		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);
		jdbcTemplate.execute("INSERT into T_FOOS (id,name,foo_date) values (1,'bar',null)");

		// force commit...
		setComplete();
		endTransaction();
		startNewTransaction();

		// Database committed so this resord should be there...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// ... the commit should also have cleared the queue, so this should now
		// be null
		foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals(null, foo);

	}

	public void testFullRollback() throws Exception {

		assertInitialState();
		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);
		jdbcTemplate.execute("INSERT into T_FOOS (id,name,foo_date) values (1,'bar',null)");

		// force rollback...
		endTransaction();
		startNewTransaction();

		// The database connection rolled back...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		// ... and so did the message session. The rollback should have restored
		// the queue, so this should now be non-null
		foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);

	}

	public void testPartialRollback() throws Exception {

		assertInitialState();
		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);
		jdbcTemplate.execute("INSERT into T_FOOS (id,name,foo_date) values (1,'bar',null)");

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			public void beforeCommit(boolean readOnly) {
				// Simulate a message system failure before the main transaction
				// commits...
				jmsTemplate.execute(new SessionCallback() {
					public Object doInJms(Session session) throws JMSException {
						try {
							assertTrue("Not a SessionProxy - wrong spring version?", session instanceof SessionProxy);
							((SessionProxy) session).getTargetSession().rollback();
						}
						catch (JMSException e) {
							throw e;
						}
						catch (Exception e) {
							// swallow it
							e.printStackTrace();
						}
						return null;
					}
				});
			}
		});
		// force commit...
		setComplete();
		endTransaction();
		startNewTransaction();

		// The database portion committed...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// ...but the JMS session rolled back, so the message is still there
		foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);

	}
}
