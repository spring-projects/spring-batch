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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.sql.DataSource;

import org.springframework.batch.container.jms.BatchMessageListenerContainer;
import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.util.ClassUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/jms/jms-context.xml")
public class AsynchronousTests {

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(ExternalRetryInBatchTests.class,
				"jms-context.xml") };
	}

	@Autowired
	private BatchMessageListenerContainer container;

	@Autowired
	private JmsTemplate jmsTemplate;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUp() throws Exception {
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		simpleJdbcTemplate.getJdbcOperations().execute("delete from T_BARS");

		// Queue is now drained...
		assertNull(foo);

		// Add a couple of messages...
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		
	}

	@After
	public void onTearDown() throws Exception {
		container.stop();
		// Need to give the container time to shutdown
		Thread.sleep(1000L);
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
	}

	private volatile List<String> list = new ArrayList<String>();

	private void assertInitialState() {
		int count = simpleJdbcTemplate.queryForInt("select count(*) from T_BARS");
		assertEquals(0, count);
	}

	@Test
	public void testSunnyDay() throws Exception {

		assertInitialState();

		container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				list.add(message.toString());
				String text = ((TextMessage) message).getText();
				simpleJdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
			}
		});

		container.initializeProxy();
		
		container.start();

		// Need to sleep for at least a second here...
		waitFor(list,2,2000);

		System.err.println(simpleJdbcTemplate.queryForList("select * from T_BARS"));

		assertEquals(2, list.size());

		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertEquals(null, foo);

		int count = simpleJdbcTemplate.queryForInt("select count(*) from T_BARS");
		assertEquals(2, count);

	}

	@Test
	public void testRollback() throws Exception {

		assertInitialState();
		
		// Prevent us from being overwhelmed after rollback
		container.setRecoveryInterval(500);

		container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				list.add(message.toString());
				final String text = ((TextMessage) message).getText();
				simpleJdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
				// This causes the DB to rollback but not the message
				if (text.equals("bar")) {
					throw new RuntimeException("Rollback!");
				}
			}
		});
		
		container.initializeProxy();

		container.start();

		// Need to sleep here, but not too long or the
		// container goes into its own recovery cycle and spits out the bad
		// message...
		waitFor(list,2,500);

		container.stop();

		// We rolled back so the messages might come in many times...
		assertTrue(list.size() >= 1);

		String text = "";
		List<String> msgs = new ArrayList<String>();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		int count = simpleJdbcTemplate.queryForInt("select count(*) from T_BARS");
		assertEquals(0, count);

		assertTrue("Foo not on queue", msgs.contains("foo"));

	}

	/**
	 * @param list
	 * @param timeout
	 * @throws InterruptedException 
	 */
	private void waitFor(List<String> list, int size, int timeout) throws InterruptedException {
		int count = 0;
		int max = timeout / 50;
		while (count<max && list.size()<size) {
			Thread.sleep(50);
		}
	}

}
