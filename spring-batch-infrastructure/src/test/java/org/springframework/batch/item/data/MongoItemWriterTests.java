/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.item.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings("serial")
public class MongoItemWriterTests {

	private MongoItemWriter<Object> writer;
	@Mock
	private MongoOperations template;
	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new MongoItemWriter<Object>();
		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new MongoItemWriter<Object>();

		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		} catch (IllegalStateException iae) {
		}

		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteNoTransactionNoCollection() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.write(items);

		verify(template).save(items.get(0));
		verify(template).save(items.get(1));
	}

	@Test
	public void testWriteNoTransactionWithCollection() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).save(items.get(0), "collection");
		verify(template).save(items.get(1), "collection");
	}

	@Test
	public void testWriteNoTransactionNoItems() throws Exception {
		writer.write(null);

		verifyZeroInteractions(template);
	}

	@Test
	public void testWriteTransactionNoCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An exception was thrown while writing: " + e.getMessage());
				}

				return null;
			}
		});

		verify(template).save(items.get(0));
		verify(template).save(items.get(1));
	}

	@Test
	public void testWriteTransactionWithCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An exception was thrown while writing: " + e.getMessage());
				}

				return null;
			}
		});

		verify(template).save(items.get(0), "collection");
		verify(template).save(items.get(1), "collection");
	}

	@Test
	public void testWriteTransactionFails() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected exception thrown");
					}
					throw new RuntimeException("force rollback");
				}
			});
		} catch (RuntimeException re) {
			assertEquals(re.getMessage(), "force rollback");
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
	}

	/**
	 * A pointless use case but validates that the flag is still honored.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWriteTransactionReadOnly() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
			transactionTemplate.setReadOnly(true);
			transactionTemplate.execute(new TransactionCallback<Void>() {

				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected exception thrown");
					}
					return null;
				}
			});
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
	}

	@Test
	public void testRemoveNoTransactionNoCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.write(items);

		verify(template).remove(items.get(0));
		verify(template).remove(items.get(1));
	}

	@Test
	public void testRemoveNoTransactionWithCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
			add(new Object());
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).remove(items.get(0), "collection");
		verify(template).remove(items.get(1), "collection");
	}
	
	// BATCH-2018
	@Test
	public void testResourceKeyCollision() throws Exception {
		final int limit = 5000;
		@SuppressWarnings("unchecked")
		final MongoItemWriter<String>[] writers = new MongoItemWriter[limit];
		final String[] results = new String[limit];
		for(int i = 0; i< limit; i++) {
			final int index = i;
			MongoOperations mongoOperations = mock(MongoOperations.class);
			
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation)
						throws Throwable {
					String val = (String) invocation.getArguments()[0];
					if(results[index] == null) {
						results[index] = val;
					} else {
						results[index] += val;
					}
					return null;
				}
			}).when(mongoOperations).save(any(String.class));
			writers[i] = new MongoItemWriter<String>();
			writers[i].setTemplate(mongoOperations);
		}
		
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
            @Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					for(int i=0; i< limit; i++) {
						writers[i].write(Collections.singletonList(String.valueOf(i)));
					}
				}
				catch (Exception e) {
					throw new IllegalStateException("Unexpected Exception", e);
				}
				return null;
			}
		});		
		
		for(int i=0; i< limit; i++) {
			assertEquals(String.valueOf(i), results[i]);
		}				
	}
	
}
