/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.repeat.jms;

import java.util.ArrayList;
import java.util.List;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.container.jms.BatchMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = "/org/springframework/batch/infrastructure/jms/jms-context.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AsynchronousTests {

	@Autowired
	private BatchMessageListenerContainer container;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void onSetUp() {
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_BARS");

		// Queue is now drained...
		assertNull(foo);

		// Add a couple of messages...
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");

	}

	@AfterEach
	void onTearDown() throws Exception {
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

	private final List<String> list = new ArrayList<>();

	private void assertInitialState() {
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);
	}

	@Test
	void testSunnyDay() throws Exception {

		assertInitialState();

		container.setMessageListener((SessionAwareMessageListener<Message>) (message, session) -> {
			list.add(message.toString());
			String text = ((TextMessage) message).getText();
			jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
		});

		container.initializeProxy();

		container.start();

		// Need to sleep for at least a second here...
		waitFor(list, 2, 2000);

		assertEquals(2, list.size());

		String foo = (String) jmsTemplate.receiveAndConvert("queue");
		assertNull(foo);

		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(2, count);

	}

	@Test
	void testRollback() throws Exception {

		assertInitialState();

		// Prevent us from being overwhelmed after rollback
		container.setRecoveryInterval(500);

		container.setMessageListener((SessionAwareMessageListener<Message>) (message, session) -> {
			list.add(message.toString());
			final String text = ((TextMessage) message).getText();
			jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
			// This causes the DB to rollback but not the message
			if (text.equals("bar")) {
				throw new RuntimeException("Rollback!");
			}
		});

		container.initializeProxy();

		container.start();

		// Need to sleep here, but not too long or the
		// container goes into its own recovery cycle and spits out the bad
		// message...
		waitFor(list, 2, 500);

		container.stop();

		// We rolled back so the messages might come in many times...
		assertTrue(list.size() >= 1);

		String text = "";
		List<String> msgs = new ArrayList<>();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);

		assertTrue(msgs.contains("foo"), "Foo not on queue");

	}

	/**
	 * @param list resource to monitor
	 * @param timeout how long to monitor for
	 * @throws InterruptedException If interrupted while waiting
	 */
	private void waitFor(List<String> list, int size, int timeout) throws InterruptedException {
		int count = 0;
		int max = timeout / 50;
		while (count < max && list.size() < size) {
			Thread.sleep(50);
		}
	}

}
