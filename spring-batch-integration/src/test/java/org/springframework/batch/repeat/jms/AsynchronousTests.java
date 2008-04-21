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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.batch.container.jms.BatchMessageListenerContainer;
import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.util.ClassUtils;

public class AsynchronousTests extends AbstractDependencyInjectionSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(ExternalRetryInBatchTests.class,
				"jms-context.xml") };
	}

	private BatchMessageListenerContainer container;

	private JmsTemplate jmsTemplate;

	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setContainer(BatchMessageListenerContainer container) {
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

		// Queue is now drained...
		assertNull(foo);

		// Add a couple of messages...
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
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
				String text = ((TextMessage) message).getText();
				jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						new Integer(list.size()), text });
			}
		});

		container.start();

		// Need to sleep for at least a second here...
		Thread.sleep(1000L);

		System.err.println(jdbcTemplate.queryForList("select * from T_FOOS"));

		assertEquals(2, list.size());

		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals(null, foo);

		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(2, count);

	}

	public void testRollback() throws Exception {

		assertInitialState();

		container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				list.add(message.toString());
				final String text = ((TextMessage) message).getText();
				jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						new Integer(list.size()), text });
				// This causes the DB to rollback but not the message
				if (text.equals("bar")) {
					throw new RuntimeException("Rollback!");
				}
			}
		});

		container.start();

		// Need to sleep for at least a second here...
		Thread.sleep(3000L);

		// We rolled back so the messages might come in many times...
		assertTrue(list.size() >= 1);

		System.err.println(jdbcTemplate.queryForList("select * from T_FOOS"));

		String text = "";
		List msgs = new ArrayList();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}
		System.err.println(msgs);

		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		assertTrue("Foo not on queue", msgs.contains("foo"));

	}
}
