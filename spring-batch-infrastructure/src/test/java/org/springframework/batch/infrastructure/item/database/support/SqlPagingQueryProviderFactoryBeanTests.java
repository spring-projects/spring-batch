/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.PagingQueryProvider;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.batch.infrastructure.support.DatabaseTypeTestUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * @author Dave Syer
 * @author Michael Minella
 */
class SqlPagingQueryProviderFactoryBeanTests {

	private final SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();

	SqlPagingQueryProviderFactoryBeanTests() throws Exception {
		factory.setSelectClause("id, name, age");
		factory.setFromClause("foo");
		factory.setWhereClause("bar = 1");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("id", Order.ASCENDING);
		factory.setSortKeys(sortKeys);
		DataSource dataSource = DatabaseTypeTestUtils.getMockDataSource(DatabaseType.HSQL.getProductName(), "100.0.0");
		factory.setDataSource(dataSource);
	}

	@Test
	void testFactory() throws Exception {
		PagingQueryProvider provider = factory.getObject();
		assertNotNull(provider);
	}

	@Test
	void testType() {
		assertEquals(PagingQueryProvider.class, factory.getObjectType());
	}

	@Test
	void testSingleton() {
		assertTrue(factory.isSingleton());
	}

	@Test
	void testNoDataSource() {
		factory.setDataSource(null);
		assertThrows(IllegalArgumentException.class, factory::getObject);
	}

	@Test
	void testNoSortKey() {
		factory.setSortKeys(null);
		assertThrows(IllegalArgumentException.class, factory::getObject);
	}

	@Test
	void testWhereClause() throws Exception {
		factory.setWhereClause("x=y");
		PagingQueryProvider provider = factory.getObject();
		String query = provider.generateFirstPageQuery(100);
		assertTrue(query.contains("x=y"), "Wrong query: " + query);
	}

	@Test
	void testAscending() throws Exception {
		PagingQueryProvider provider = factory.getObject();
		String query = provider.generateFirstPageQuery(100);
		assertTrue(query.contains("ASC"), "Wrong query: " + query);
	}

	@Test
	void testWrongDatabaseType() {
		factory.setDatabaseType("NoSuchDb");
		assertThrows(IllegalArgumentException.class, factory::getObject);
	}

	@Test
	void testMissingMetaData() throws Exception {
		factory.setDataSource(DatabaseTypeTestUtils.getMockDataSource(new MetaDataAccessException("foo")));
		assertThrows(IllegalArgumentException.class, factory::getObject);
	}

	@Test
	void testAllDatabaseTypes() throws Exception {
		for (DatabaseType type : DatabaseType.values()) {
			factory.setDatabaseType(type.name());
			PagingQueryProvider provider = factory.getObject();
			assertNotNull(provider);
		}
	}

}
