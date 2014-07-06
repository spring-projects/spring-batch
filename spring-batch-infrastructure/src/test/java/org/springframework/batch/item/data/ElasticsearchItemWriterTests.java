/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.item.data;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class ElasticsearchItemWriterTests {
	
	@Document(indexName="test_index", type="test_type")
	public class DummyDocument {
		
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
	
	private ElasticsearchItemWriter writer;
	
	@Mock
	private ElasticsearchOperations elasticsearchOperations;
	
	private TransactionTemplate transactionTemplate;
	
	private DummyDocument dummyDocument;
	
	@Before
	public void setUp() throws Exception {
		initMocks(this);
		transactionTemplate = new TransactionTemplate(new ResourcelessTransactionManager());
		writer = new ElasticsearchItemWriter(elasticsearchOperations);
		writer.afterPropertiesSet();
		dummyDocument = new DummyDocument();
	}
	
	@After
	public void tearDown() {
		transactionTemplate = null;
		writer = null;
		dummyDocument = null;
	}
	
	@Test(expected=IllegalStateException.class)
	public void shouldFailAssertion() throws Exception {
		
		new ElasticsearchItemWriter(null).afterPropertiesSet();
		fail("Assertion shold have thrown exception on null ElasticsearchOperations");
	}
	
	@Test
	public void shouldNotWriteWhenNoTransactionIsActiveAndNoItem() throws Exception {
		
		writer.write(null);
		verifyZeroInteractions(elasticsearchOperations);
		
		writer.write(new ArrayList<IndexQuery>(0));
		verifyZeroInteractions(elasticsearchOperations);
	}
	
	@Test
	public void shouldWriteItemWhenNoTransactionIsActive() throws Exception {
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withObject(dummyDocument);
		
		List<IndexQuery> items = asList(builder.build());
		
		writer.write(items);
		
		verify(elasticsearchOperations).index(items.iterator().next());
	}
	
	@Test
	public void shouldWriteItemWhenInTransaction() throws Exception {
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withObject(dummyDocument);
		
		final List<IndexQuery> items = asList(builder.build());

		transactionTemplate.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An error occured while writing: " + e.getMessage());
				}
				
				return null;
			}
		});
		
		verify(elasticsearchOperations).index(items.iterator().next());
	}
	
	@Test
	public void shouldNotWriteItemWhenTransactionFails() throws Exception {
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withObject(dummyDocument);
		
		final List<IndexQuery> items = asList(builder.build());

		try {
			transactionTemplate.execute(new TransactionCallback<Void>() {

				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected error occured");
					}
					throw new RuntimeException("rollback");
				}
			});
		} catch (RuntimeException re) {
			// ignore
		} catch (Throwable t) {
			fail("Unexpected error occured");
		}

		verifyZeroInteractions(elasticsearchOperations);
	}
	
	@Test
	public void shouldNotWriteItemWhenTransactionIsReadOnly() throws Exception {
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withObject(dummyDocument);
		
		final List<IndexQuery> items = asList(builder.build());

		try {
			
			transactionTemplate.setReadOnly(true);
			transactionTemplate.execute(new TransactionCallback<Void>() {

				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					} catch (Exception ignore) {
						fail("unexpected error occured");
					}
					return null;
				}
			});
		} catch (Throwable t) {
			fail("unexpected error occured");
		}

		verifyZeroInteractions(elasticsearchOperations);
	}
	
	@Test
	public void shouldRemoveItemWhenNoTransactionIsActive() throws Exception {
		
		writer.setDelete(true);
		
		dummyDocument.setId("123456");
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withId(dummyDocument.getId());
		builder.withObject(dummyDocument);
		
		final List<IndexQuery> items = asList(builder.build());

		writer.write(items);

		verify(elasticsearchOperations).delete(items.iterator().next().getObject().getClass(), items.iterator().next().getId());
	}
	
	@Test
	public void shouldRemoveItemWhenInTransaction() throws Exception {
		
		writer.setDelete(true);
		
		dummyDocument.setId("123456");
		
		IndexQueryBuilder builder = new IndexQueryBuilder();
		builder.withId(dummyDocument.getId());
		builder.withObject(dummyDocument);
		
		final List<IndexQuery> items = asList(builder.build());

		transactionTemplate.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				} catch (Exception e) {
					fail("An error occured while writing: " + e.getMessage());
				}
				
				return null;
			}
		});
		
		verify(elasticsearchOperations).delete(items.iterator().next().getObject().getClass(), items.iterator().next().getId());
	}
}