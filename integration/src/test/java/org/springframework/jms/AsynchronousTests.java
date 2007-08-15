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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

public class AsynchronousTests extends AbstractDependencyInjectionSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.classPackageAsResourcePath(getClass()) + "/asynch.xml" };
	}

	private DefaultMessageListenerContainer container;

	private JmsTemplate jmsTemplate;

	private JdbcTemplate jdbcTemplate;

	private PlatformTransactionManager transactionManager;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setContainer(DefaultMessageListenerContainer container) {
		this.container = container;
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		jdbcTemplate.execute("delete from T_FOOS");
		jmsTemplate.convertAndSend("queue", "foo");
	}

	protected void onTearDown() throws Exception {
		super.onTearDown();
		container.stop();
		// Need to give the container time to shutdown
		Thread.sleep(1000L);
	}

	List list = new ArrayList();

	private void assertInitialState() {
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);
	}

	public void testSunnyDay() throws Exception {

		assertInitialState();

		container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				list.add(message.toString());
				jdbcTemplate.execute("INSERT into T_FOOS (id,name,foo_date) values (1,'bar',null)");
			}
		});

		container.start();

		// Need to sleep for at least a second here...
		Thread.sleep(1000L);

		assertEquals(1, list.size());

		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals(null, foo);

		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

	}

	public void testRollback() throws Exception {

		assertInitialState();

		container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				list.add(message.toString());
				new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						jdbcTemplate.execute("INSERT into T_FOOS (id,name,foo_date) values (1,'bar',null)");
						// This causes the DB to rollback but not the message
						throw new RuntimeException("Rollback!");
					}
				});
			}
		});

		container.start();

		// Need to sleep for at least a second here...
		Thread.sleep(3000L);

		// We rolled back so the message might come in many times...
		assertTrue(list.size() > 1);

		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals("foo", foo);

	}
}
