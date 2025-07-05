/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.repeat.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;

import org.junit.jupiter.api.Test;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.connection.SessionProxy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(locations = "/org/springframework/batch/jms/jms-context.xml")
@DirtiesContext
class SynchronousTests implements ApplicationContextAware {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private RepeatTemplate repeatTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ApplicationContext applicationContext;

	private final List<String> list = new ArrayList<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@BeforeTransaction
	void onSetUpBeforeTransaction() {
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_BARS");
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
	}

	private void assertInitialState() {
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);
	}

	@Transactional
	@Test
	void testCommit() {

		assertInitialState();

		repeatTemplate.iterate(context -> {
			String text = (String) jmsTemplate.receiveAndConvert("queue");
			list.add(text);
			jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
			return RepeatStatus.continueIf(text != null);
		});

		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(2, count);

		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));

		String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertNull(text);

	}

	@Test
	void testFullRollback() {

		onSetUpBeforeTransaction();

		assertInitialState();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			repeatTemplate.iterate(context -> {
				String text = (String) jmsTemplate.receiveAndConvert("queue");
				list.add(text);
				jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
				return RepeatStatus.continueIf(text != null);
			});
			// force rollback...
			status.setRollbackOnly();
			return null;
		});

		String text = "";
		List<String> msgs = new ArrayList<>();
		while (text != null) {
			text = (String) jmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		// The database portion rolled back...
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);

		// ... and so did the message session. The rollback should have restored
		// the queue, so this should now be non-null
		assertTrue(msgs.contains("foo"), "Foo not on queue");
	}

	@Transactional
	@Test
	void JpaNativeQueryProviderIntegrationTeststestPartialRollback() {

		// The JmsTemplate is used elsewhere outside a transaction, so
		// we need to use one here that is transaction aware.
		final JmsTemplate txJmsTemplate = new JmsTemplate(
				applicationContext.getBean("txAwareConnectionFactory", ConnectionFactory.class));
		txJmsTemplate.setReceiveTimeout(100L);
		txJmsTemplate.setSessionTransacted(true);

		assertInitialState();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {

			repeatTemplate.iterate(context -> {
				String text = (String) txJmsTemplate.receiveAndConvert("queue");
				list.add(text);
				jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
				return RepeatStatus.continueIf(text != null);
			});

			// Simulate a message system failure before the main transaction
			// commits...
			txJmsTemplate.execute((SessionCallback<Void>) session -> {
				try {
					assertTrue(session instanceof SessionProxy, "Not a SessionProxy - wrong spring version?");
					((SessionProxy) session).getTargetSession().rollback();
				}
				catch (JMSException e) {
					throw e;
				}
				catch (Exception e) {
					// swallow it
				}
				return null;
			});

			return null;
		});

		String text = "";
		List<String> msgs = new ArrayList<>();
		while (text != null) {
			text = (String) txJmsTemplate.receiveAndConvert("queue");
			msgs.add(text);
		}

		// The database portion committed...
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(2, count);

		// ...but the JMS session rolled back, so the message is still there
		assertTrue(msgs.contains("foo"), "Foo not on queue");
		assertTrue(msgs.contains("bar"), "Bar not on queue");

	}

}
