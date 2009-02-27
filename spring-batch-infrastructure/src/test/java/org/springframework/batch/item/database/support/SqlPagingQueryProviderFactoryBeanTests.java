/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.database.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.support.DatabaseType;
import org.springframework.batch.support.DatabaseTypeTestUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * @author Dave Syer
 */
public class SqlPagingQueryProviderFactoryBeanTests {

	private SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();

	public SqlPagingQueryProviderFactoryBeanTests() throws Exception {
		factory.setSelectClause("id, name, age");
		factory.setFromClause("foo");
		factory.setWhereClause("bar = 1");
		factory.setSortKey("id");
		DataSource dataSource = DatabaseTypeTestUtils.getMockDataSource(DatabaseType.HSQL.getProductName(), "100.0.0");
		factory.setDataSource(dataSource);
		EasyMock.replay(dataSource);
	}

	@Test
	public void testFactory() throws Exception {
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		assertNotNull(provider);
	}

	@Test
	public void testType() throws Exception {
		assertEquals(PagingQueryProvider.class, factory.getObjectType());
	}

	@Test
	public void testSingleton() throws Exception {
		assertEquals(true, factory.isSingleton());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoDataSource() throws Exception {
		factory.setDataSource(null);
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		assertNotNull(provider);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoSortKey() throws Exception {
		factory.setSortKey(null);
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		assertNotNull(provider);
	}

	@Test
	public void testWhereClause() throws Exception {
		factory.setWhereClause("x=y");
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		String query = provider.generateFirstPageQuery(100);
		assertTrue("Wrong query: "+query, query.contains("x=y"));
	}

	@Test
	public void testAscending() throws Exception {
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		String query = provider.generateFirstPageQuery(100);
		assertTrue("Wrong query: "+query, query.contains("ASC"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWrongDatabaseType() throws Exception {
		factory.setDatabaseType("NoSuchDb");
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		assertNotNull(provider);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingMetaData() throws Exception {
		factory.setDataSource(DatabaseTypeTestUtils.getMockDataSource(new MetaDataAccessException("foo")));
		PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
		assertNotNull(provider);
	}

	@Test
	public void testAllDatabaseTypes() throws Exception {
		for (DatabaseType type : DatabaseType.values()) {
			factory.setDatabaseType(type.name());
			PagingQueryProvider provider = (PagingQueryProvider) factory.getObject();
			assertNotNull(provider);			
		}
	}

}
