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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

public class ElasticsearchItemReaderTest {
	
	private ElasticsearchItemReader<Object> reader;
	
	@Mock
	private ElasticsearchOperations elasticsearchOperations;
	
	private SearchQuery query;
	
	@Before
	public void setUp() throws Exception {
		initMocks(this);
		query = new NativeSearchQueryBuilder().build();
		reader = new ElasticsearchItemReader<Object>(elasticsearchOperations, query, Object.class);
		reader.afterPropertiesSet();
	}
	
	@After
	public void tearDown() {
		query = null;
		elasticsearchOperations = null;
	}

	@Test(expected=IllegalStateException.class)
	public void shouldFailAssertionOnNullElasticsearchOperations() throws Exception {
		
		try {
			new ElasticsearchItemReader<Object>(null, null, null).afterPropertiesSet();
			fail("Assertion should have thrown exception on null ElasticsearchOperations");
		}catch(IllegalStateException e) {
			assertEquals("An ElasticsearchOperations implementation is required.", e.getMessage());
			throw e;
		}
	}
	
	@Test(expected=IllegalStateException.class)
	public void shouldFailAssertionOnNullQuery() throws Exception {
		
		try {
			new ElasticsearchItemReader<Object>(elasticsearchOperations, null, null).afterPropertiesSet();
			fail("Assertion shold have thrown exception on null Query");
		}catch(IllegalStateException e) {
			assertEquals("A query is required.", e.getMessage());
			throw e;
		}
	}
	
	@Test(expected=IllegalStateException.class)
	public void shouldFailAssertionOnNullTargetType() throws Exception {
		
		try {
			new ElasticsearchItemReader<Object>(elasticsearchOperations, query, null).afterPropertiesSet();
			fail("Assertion shold have thrown exception on null Target Type");
		}catch(IllegalStateException e) {
			assertEquals("A target type to convert the input into is required.", e.getMessage());
			throw e;
		}
	}
	
	@Test
	public void shouldQueryForList() {
		
		when(elasticsearchOperations.queryForList(query, Object.class)).thenReturn(asList());
		
		reader.doPageRead();
		
		verify(elasticsearchOperations).queryForList(query, Object.class);
	}
}