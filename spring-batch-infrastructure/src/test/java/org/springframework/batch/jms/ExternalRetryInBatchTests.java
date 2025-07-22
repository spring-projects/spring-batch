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

package org.springframework.batch.jms;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(locations = "/org/springframework/batch/jms/jms-context.xml")
class ExternalRetryInBatchTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	private RetryTemplate retryTemplate;

	@Autowired
	private RepeatTemplate repeatTemplate;

	private ItemReader<String> provider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void onSetUp() {
		getMessages(); // drain queue
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_BARS");
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		provider = () -> {
			String text = (String) jmsTemplate.receiveAndConvert("queue");
			list.add(text);
			return text;
		};
		retryTemplate = new RetryTemplate();
	}

	@AfterEach
	void onTearDown() {
		getMessages(); // drain queue
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_BARS");
	}

	private void assertInitialState() {
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);
	}

	private final List<String> list = new ArrayList<>();

	private final List<String> recovered = new ArrayList<>();

	@Test
	void testExternalRetryRecoveryInBatch() {
		assertInitialState();

		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true)));

		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));

		// In a real container this could be an outer retry loop with an
		// *internal* retry policy.
		for (int i = 0; i < 4; i++) {
			try {
				new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
					try {

						repeatTemplate.iterate(context -> {

							final String item = provider.read();

							if (item == null) {
								return RepeatStatus.FINISHED;
							}

							RetryCallback<String, Exception> callback = context12 -> {
								// No need for transaction here: the whole
								// batch will roll
								// back. When it comes back for recovery this
								// code is not
								// executed...
								jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)",
										list.size(), item);
								throw new RuntimeException("Rollback!");
							};

							RecoveryCallback<String> recoveryCallback = context1 -> {
								// aggressive commit on a recovery
								RepeatSynchronizationManager.setCompleteOnly();
								recovered.add(item);
								return item;
							};

							retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState(item));

							return RepeatStatus.CONTINUABLE;

						});
						return null;

					}
					catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				});
			}
			catch (Exception e) {

				if (i == 0 || i == 2) {
					assertEquals("Rollback!", e.getMessage());
				}
				else {
					throw e;
				}

			}
		}

		List<String> msgs = getMessages();

		assertEquals(2, recovered.size());

		// The database portion committed once...
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);

		// ... and so did the message session.
		// Both messages were failed and recovered after last retry attempt:
		assertEquals("[]", msgs.toString());
		assertEquals("[foo, bar]", recovered.toString());

	}

	private List<String> getMessages() {
		String next = "";
		List<String> msgs = new ArrayList<>();
		while (next != null) {
			next = (String) jmsTemplate.receiveAndConvert("queue");
			if (next != null)
				msgs.add(next);
		}
		return msgs;
	}

}
