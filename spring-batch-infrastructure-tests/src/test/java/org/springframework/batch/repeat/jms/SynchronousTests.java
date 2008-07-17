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

package org.springframework.batch.repeat.jms;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.jms.connection.SessionProxy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

public class SynchronousTests extends AbstractTransactionalDataSourceSpringContextTests {

	private JmsTemplate jmsTemplate;

	private RepeatTemplate repeatTemplate;

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setRepeatTemplate(RepeatTemplate repeatTemplate) {
		this.repeatTemplate = repeatTemplate;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(ExternalRetryInBatchTests.class,
				"jms-context.xml") };
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
		jmsTemplate.convertAndSend("queue", "bar");
	}

	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
	}

	private void assertInitialState() {
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);
	}

	List<String> list = new ArrayList<String>();

	public void testCommit() throws Exception {

		assertInitialState();

		repeatTemplate.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				String text = (String) jmsTemplate.receiveAndConvert("queue");
				list.add(text);
				jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						new Integer(list.size()), text });
				return new ExitStatus(text != null);
			}
		});

		// force commit...
		setComplete();
		endTransaction();
		startNewTransaction();

		System.err.println(jdbcTemplate.queryForList("select * from T_FOOS"));

		// Database committed so this record should be there...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(2, count);

		// ... the commit should also have cleared the queue, so this should now
		// be null
		String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals(null, text);

	}

	public void testFullRollback() throws Exception {

		assertInitialState();
		repeatTemplate.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				String text = (String) jmsTemplate.receiveAndConvert("queue");
				list.add(text);
				jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						new Integer(list.size()), text });
				return new ExitStatus(text != null);
			}
		});

		// force rollback...
		endTransaction();
		startNewTransaction();

		String text = "";
		List<String> msgs = new ArrayList<String>();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		// The database portion rolled back...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		// ... and so did the message session. The rollback should have restored
		// the queue, so this should now be non-null
		assertTrue("Foo not on queue", msgs.contains("foo"));
	}

	public void testPartialRollback() throws Exception {

		// The JmsTemplate is used elsewhere outside a transaction, so
		// we need to use one here that is transaction aware.
		final JmsTemplate jmsTemplate = new JmsTemplate((ConnectionFactory) applicationContext
				.getBean("txAwareConnectionFactory"));
		jmsTemplate.setReceiveTimeout(100L);
		jmsTemplate.setSessionTransacted(true);

		assertInitialState();
		repeatTemplate.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				String text = (String) jmsTemplate.receiveAndConvert("queue");
				list.add(text);
				jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						new Integer(list.size()), text });
				return new ExitStatus(text != null);
			}
		});

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

		String text = "";
		List<String> msgs = new ArrayList<String>();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		// The database portion committed...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(2, count);

		// ...but the JMS session rolled back, so the message is still there
		assertTrue("Foo not on queue", msgs.contains("foo"));
		assertTrue("Bar not on queue", msgs.contains("bar"));

	}
}
