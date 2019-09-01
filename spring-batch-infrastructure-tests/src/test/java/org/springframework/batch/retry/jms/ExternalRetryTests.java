/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.retry.jms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.lang.Nullable;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/jms/jms-context.xml")
public class ExternalRetryTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	private RetryTemplate retryTemplate;

	private ItemReader<String> provider;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Before
	public void onSetUp() throws Exception {
		getMessages(); // drain queue
		jdbcTemplate.execute("delete from T_BARS");
		jmsTemplate.convertAndSend("queue", "foo");
		provider = new ItemReader<String>() {
			@Nullable
			@Override
			public String read() {
				String text = (String) jmsTemplate.receiveAndConvert("queue");
				list.add(text);
				return text;
			}
		};
		retryTemplate = new RetryTemplate();
	}

	private void assertInitialState() {
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(0, count);
	}

	private List<String> list = new ArrayList<>();

	private List<Object> recovered = new ArrayList<>();

	/*
	 * Message processing is successful on the second attempt but must receive
	 * the message again.
	 */
	@Test
	public void testExternalRetrySuccessOnSecondAttempt() throws Exception {

		assertInitialState();

		final ItemWriter<Object> writer = new ItemWriter<Object>() {
			@Override
			public void write(final List<?> texts) {

				for (Object text : texts) {

					jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(),
							text);
					if (list.size() == 1) {
						throw new RuntimeException("Rollback!");
					}

				}

			}
		};

		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						final Object item = provider.read();
						RetryCallback<Object, Exception> callback = new RetryCallback<Object, Exception>() {
							@Override
							public Object doWithRetry(RetryContext context) throws Exception {
								writer.write(Collections.singletonList(item));
								return null;
							}
						};
						return retryTemplate.execute(callback, new DefaultRetryState(item));
					}
					catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			});
			fail("Expected Exception");
		}
		catch (Exception e) {

			assertEquals("Rollback!", e.getMessage());

			// Client of retry template has to take care of rollback. This would
			// be a message listener container in the MDP case.

		}

		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					final String item = provider.read();
					RetryCallback<Object, Exception> callback = new RetryCallback<Object, Exception>() {
						@Override
						public Object doWithRetry(RetryContext context) throws Exception {
							writer.write(Collections.singletonList(item));
							return null;
						}
					};
					return retryTemplate.execute(callback, new DefaultRetryState(item));
				}
				catch (Exception e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		});

		List<String> msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/*
	 * Message processing fails on both attempts.
	 */
	@Test
	public void testExternalRetryWithRecovery() throws Exception {

		assertInitialState();

		final String item = provider.read();
		final RetryCallback<String, Exception> callback = new RetryCallback<String, Exception>() {
			@Override
			public String doWithRetry(RetryContext context) throws Exception {
				jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), item);
				throw new RuntimeException("Rollback!");
			}
		};

		final RecoveryCallback<String> recoveryCallback = new RecoveryCallback<String>() {
			@Override
			public String recover(RetryContext context) {
				recovered.add(item);
				return item;
			}
		};

		String result = "start";

		for (int i = 0; i < 4; i++) {
			try {
				result = new TransactionTemplate(transactionManager).execute(new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {
						try {
							return retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState(item));
						}
						catch (Exception e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					}
				});
			}
			catch (Exception e) {

				if (i < 3)
					assertEquals("Rollback!", e.getMessage());

				// Client of retry template has to take care of rollback. This
				// would
				// be a message listener container in the MDP case.

			}
		}

		// Last attempt should return last item.
		assertEquals("foo", result);

		List<String> msgs = getMessages();

		assertEquals(1, recovered.size());

		// The database portion committed once...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(0, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());

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
