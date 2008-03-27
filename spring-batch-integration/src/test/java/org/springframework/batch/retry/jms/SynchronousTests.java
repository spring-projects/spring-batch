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

package org.springframework.batch.retry.jms;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.callback.ItemWriterRetryCallback;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class SynchronousTests extends AbstractTransactionalDataSourceSpringContextTests {

	private JmsTemplate jmsTemplate;

	private RetryTemplate retryTemplate;

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	protected String[] getConfigLocations() {
		return new String[] { "/org/springframework/batch/jms/jms-context.xml" };
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
		jmsTemplate.convertAndSend("queue", "foo");
		final String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertNotNull(text);
		retryTemplate = new RetryTemplate();
	}

	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
	}

	private void assertInitialState() {
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);
	}

	List list = new ArrayList();

	/**
	 * Message processing is successful on the second attempt without having to receive the message again.
	 * 
	 * @throws Exception
	 */
	public void testInternalRetrySuccessOnSecondAttempt() throws Exception {

		assertInitialState();

		/*
		 * We either want the JMS receive to be outside a transaction, or we need the database transaction in the retry
		 * to be PROPAGATION_NESTED. Otherwise JMS will roll back when the retry callback is eventually successful
		 * because of the previous exception. PROPAGATION_REQUIRES_NEW is wrong because it doesn't allow the outer
		 * transaction to fail and rollback the inner one.
		 */
		final String text = (String) jmsTemplate.receiveAndConvert("queue");
		assertNotNull(text);

		retryTemplate.execute(new RetryCallback() {
			public Object doWithRetry(RetryContext status) throws Throwable {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
				return transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {

						list.add(text);
						System.err.println("Inserting: [" + list.size() + "," + text + "]");
						jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						        new Integer(list.size()), text });
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}
						return text;

					}
				});

			}
		});

		// force commit...
		setComplete();
		endTransaction();

		startNewTransaction();

		List msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/**
	 * Message processing is successful on the second attempt without having to receive the message again - uses
	 * JmsItemProvider internally.
	 * 
	 * @throws Exception
	 */
	public void testInternalRetrySuccessOnSecondAttemptWithItemProvider() throws Exception {

		assertInitialState();

		JmsItemReader provider = new JmsItemReader();
		// provider.setItemType(Message.class);
		provider.setJmsTemplate(jmsTemplate);
		jmsTemplate.setDefaultDestinationName("queue");

		retryTemplate.execute(new ItemWriterRetryCallback(provider.read(), new AbstractItemWriter() {
			public void write(final Object text) {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {

						list.add(text);
						System.err.println("Inserting: [" + list.size() + "," + text + "]");
						jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						        new Integer(list.size()), text });
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}

						return text;

					}
				});

			}
		}));

		// force commit...
		setComplete();
		endTransaction();

		startNewTransaction();

		List msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/**
	 * Message processing is successful on the second attempt without having to receive the message again.
	 * 
	 * @throws Exception
	 */
	public void testInternalRetrySuccessOnFirstAttemptRollbackOuter() throws Exception {

		assertInitialState();

		/*
		 * We either want the JMS receive to be outside a transaction, or we need the database transaction in the retry
		 * to be PROPAGATION_NESTED. Otherwise JMS will roll back when the retry callback is eventually successful
		 * because of the previous exception. PROPAGATION_REQUIRES_NEW is wrong because it doesn't allow the outer
		 * transaction to fail and rollback the inner one.
		 */
		final String text = (String) jmsTemplate.receiveAndConvert("queue");

		retryTemplate.execute(new RetryCallback() {
			public Object doWithRetry(RetryContext status) throws Throwable {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_NESTED);
				return transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {

						list.add(text);
						System.err.println("Inserting: [" + list.size() + "," + text + "]");
						jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						        new Integer(list.size()), text });
						return text;

					}
				});

			}
		});

		// The database transaction has committed...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// force rollback...
		endTransaction();

		startNewTransaction();

		List msgs = getMessages();

		// The database portion rolled back...
		count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		// ... and so did the message session.
		assertEquals("[foo]", msgs.toString());
	}

	/**
	 * Message processing is successful on the second attempt but must receive the message again.
	 * 
	 * @throws Exception
	 */
	public void testExternalRetrySuccessOnSecondAttempt() throws Exception {

		assertInitialState();

		// force commit so that the retry executes in its own transaction (not
		// nested)...
		setComplete();
		endTransaction();

		retryTemplate.execute(new RetryCallback() {
			public Object doWithRetry(RetryContext status) throws Throwable {

				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				return transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {

						// The receive is inside the retry and the
						// transaction...
						final String text = (String) jmsTemplate.receiveAndConvert("queue");
						list.add(text);
						jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)", new Object[] {
						        new Integer(list.size()), text });
						if (list.size() == 1) {
							throw new RuntimeException("Rollback!");
						}
						return text;

					}
				});

			}
		});

		startNewTransaction();

		List msgs = getMessages();

		// The database portion committed once...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(1, count);

		// ... and so did the message session.
		assertEquals("[]", msgs.toString());
	}

	/**
	 * Message processing fails.
	 * 
	 * @throws Exception
	 */
	public void testExternalRetryFailOnSecondAttempt() throws Exception {

		assertInitialState();

		// force commit so that the retry executes in its own transaction (not
		// nested)...
		setComplete();
		endTransaction();

		try {

			retryTemplate.execute(new RetryCallback() {
				public Object doWithRetry(RetryContext status) throws Throwable {

					TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
					return transactionTemplate.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {

							// The receieve is inside the retry and the
							// transaction...
							final String text = (String) jmsTemplate.receiveAndConvert("queue");
							list.add(text);
							jdbcTemplate.update("INSERT into T_FOOS (id,name,foo_date) values (?,?,null)",
							        new Object[] { new Integer(list.size()), text });
							throw new RuntimeException("Rollback!");

						}
					});

				}
			});

			/*
			 * N.B. the message can be re-directed to an error queue by setting an error destination in a
			 * JmsItemProvider.
			 */
			fail("Expected RuntimeException");

		} catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
			// expected
		}

		startNewTransaction();

		List msgs = getMessages();

		// The database portion rolled back...
		int count = jdbcTemplate.queryForInt("select count(*) from T_FOOS");
		assertEquals(0, count);

		// ... and so did the message session.
		assertTrue(msgs.contains("foo"));
	}

	private List getMessages() {
		String next = "";
		List msgs = new ArrayList();
		while (next != null) {
			next = (String) jmsTemplate.receiveAndConvert("queue");
			if (next != null)
				msgs.add(next);
		}
		return msgs;
	}
}
