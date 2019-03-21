/*
 * Copyright 2006-2007 the original author or authors.
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
import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/jms/jms-context.xml")
public class SynchronousTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private RetryTemplate retryTemplate;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(ExternalRetryInBatchTests.class,
				"jms-context.xml") };
	}

	@BeforeTransaction
	public void onSetUpBeforeTransaction() throws Exception {
		jdbcTemplate.execute("delete from T_BARS");
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "foo");
		final String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertNotNull(text);
	}

	@Before
	public void onSetUpInTransaction() throws Exception {
		retryTemplate = new RetryTemplate();
	}

	@AfterTransaction
	public void afterTransaction() {
		String foo = "";
		int count = 0;
		while (foo != null && count < 100) {
			foo = (String) jmsTemplate.receiveAndConvert("queue");
			count++;
		}
		jdbcTemplate.execute("delete from T_BARS");
	}

	private void assertInitialState() {
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(0, count);
	}

	List<Object> list = new ArrayList<>();

	/*
	 * Message processing is successful on the second attempt without having to
	 * receive the message again.
	 */
	@Transactional @Test
	public void testInternalRetrySuccessOnSecondAttempt() throws Exception {

		assertInitialState();

		/*
		 * We either want the JMS receive to be outside a transaction, or we
		 * need the database transaction in the retry to be PROPAGATION_NESTED.
		 * Otherwise JMS will roll back when the retry callback is eventually
		 * successful because of the previous exception.
		 * PROPAGATION_REQUIRES_NEW is wrong because it doesn't allow the outer
		 * transaction to fail and rollback the inner one.
		 */
		final String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertNotNull(text);

		retryTemplate.execute(new RetryCallback<String, Exception>() {
			@Override
			public String doWithRetry(RetryContext status) throws Exception {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
				return transactionTemplate.execute(new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {

						list.add(text);
						System.err.println("Inserting: [" + list.size() + "," + text + "]");
						jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}
						return text;

					}
				});

			}
		});

		// Verify the state after transactional processing is complete

		List<String> msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/*
	 * Message processing is successful on the second attempt without having to
	 * receive the message again - uses JmsItemProvider internally.
	 */
	@Transactional @Test
	public void testInternalRetrySuccessOnSecondAttemptWithItemProvider() throws Exception {

		assertInitialState();

		JmsItemReader<Object> provider = new JmsItemReader<>();
		// provider.setItemType(Message.class);
		jmsTemplate.setDefaultDestinationName("queue");
		provider.setJmsTemplate(jmsTemplate);

		final String item = (String) provider.read();

		retryTemplate.execute(new RetryCallback<String, Exception>() {
			@Override
			public String doWithRetry(RetryContext context) throws Exception {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
				return transactionTemplate.execute(new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {

						list.add(item);
						System.err.println("Inserting: [" + list.size() + "," + item + "]");
						jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), item);
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}

						return item;

					}
				});

			}
		});

		// Verify the state after transactional processing is complete

		List<String> msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/*
	 * Message processing is successful on the second attempt without having to
	 * receive the message again.
	 */
	@Transactional @Test
	public void testInternalRetrySuccessOnFirstAttemptRollbackOuter() throws Exception {

		assertInitialState();

		/*
		 * We either want the JMS receive to be outside a transaction, or we
		 * need the database transaction in the retry to be PROPAGATION_NESTED.
		 * Otherwise JMS will roll back when the retry callback is eventually
		 * successful because of the previous exception.
		 * PROPAGATION_REQUIRES_NEW is wrong because it doesn't allow the outer
		 * transaction to fail and rollback the inner one.
		 */

		TransactionTemplate outerTxTemplate = new TransactionTemplate(transactionManager);
		outerTxTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		outerTxTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus outerStatus) {

				final String text = (String) jmsTemplate.receiveAndConvert("queue");

				try {
					retryTemplate.execute(new RetryCallback<String, Exception>() {
						@Override
						public String doWithRetry(RetryContext status) throws Exception {

							TransactionTemplate nestedTxTemplate = new TransactionTemplate(transactionManager);
							nestedTxTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
							return nestedTxTemplate.execute(new TransactionCallback<String>() {
								@Override
								public String doInTransaction(TransactionStatus nestedStatus) {

									list.add(text);
									System.err.println("Inserting: [" + list.size() + "," + text + "]");
									jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
									return text;

								}
							});

						}
					});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				// The nested database transaction has committed...
				int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
				assertEquals(1, count);

				// force rollback...
				outerStatus.setRollbackOnly();

				return null;
			}
		});

		// Verify the state after transactional processing is complete

		List<String> msgs = getMessages();

		// The database portion rolled back...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(0, count);

		// ... and so did the message session.
		assertEquals("[foo]", msgs.toString());

	}

	/*
	 * Message processing is successful on the second attempt but must receive
	 * the message again.
	 */
	@Test
	public void testExternalRetrySuccessOnSecondAttempt() throws Exception {

		assertInitialState();

		retryTemplate.execute(new RetryCallback<String, Exception>() {
			@Override
			public String doWithRetry(RetryContext status) throws Exception {

				// use REQUIRES_NEW  so that the retry executes in its own transaction
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
				return transactionTemplate.execute(new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {

						// The receive is inside the retry and the
						// transaction...
						final String text = (String) jmsTemplate.receiveAndConvert("queue");
						list.add(text);
						jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}
						return text;

					}
				});

			}
		});

		// Verify the state after transactional processing is complete

		List<String> msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());

	}

	/*
	 * Message processing fails.
	 */
	@Transactional @Test
	public void testExternalRetryFailOnSecondAttempt() throws Exception {

		assertInitialState();

		try {

			retryTemplate.execute(new RetryCallback<String, Exception>() {
				@Override
				public String doWithRetry(RetryContext status) throws Exception {

					// use REQUIRES_NEW  so that the retry executes in its own transaction
					TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
					transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
					return transactionTemplate.execute(new TransactionCallback<String>() {
						@Override
						public String doInTransaction(TransactionStatus status) {

							// The receive is inside the retry and the
							// transaction...
							final String text = (String) jmsTemplate.receiveAndConvert("queue");
							list.add(text);
							jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", list.size(), text);
							throw new RuntimeException("Rollback!");

						}
					});

				}
			});

			/*
			 * N.B. the message can be re-directed to an error queue by setting
			 * an error destination in a JmsItemProvider.
			 */
			fail("Expected RuntimeException");

		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
			// expected
		}

		// Verify the state after transactional processing is complete

		List<String> msgs = getMessages();

		// The database portion rolled back...
		int count = jdbcTemplate.queryForObject("select count(*) from T_BARS", Integer.class);
		assertEquals(0, count);

		// ... and so did the message session.
		assertTrue(msgs.contains("foo"));
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
