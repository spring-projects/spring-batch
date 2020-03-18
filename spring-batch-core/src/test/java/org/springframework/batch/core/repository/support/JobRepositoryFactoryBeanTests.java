/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.core.serializer.Serializer;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lucas Ward
 * @author Will Schipp
 *
 */
public class JobRepositoryFactoryBeanTests {

	private JobRepositoryFactoryBean factory;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private String tablePrefix = "TEST_BATCH_PREFIX_";

	@Before
	public void setUp() throws Exception {

		factory = new JobRepositoryFactoryBean();
		dataSource = mock(DataSource.class);
		transactionManager = mock(PlatformTransactionManager.class);
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		factory.setIncrementerFactory(incrementerFactory);
		factory.setTablePrefix(tablePrefix);

	}

	@Test
	public void testNoDatabaseType() throws Exception {

		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		Connection con = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(con);
		when(con.getMetaData()).thenReturn(dmd);
		when(dmd.getDatabaseProductName()).thenReturn("Oracle");

		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());

		factory.afterPropertiesSet();
		factory.getObject();

	}

	@Test
	public void testOracleLobHandler() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();
		LobHandler lobHandler = (LobHandler) ReflectionTestUtils.getField(factory, "lobHandler");
		assertTrue(lobHandler instanceof DefaultLobHandler);

	}

	@Test
	public void testCustomLobHandler() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		LobHandler lobHandler = new DefaultLobHandler();
		factory.setLobHandler(lobHandler);

		factory.afterPropertiesSet();
		assertEquals(lobHandler, ReflectionTestUtils.getField(factory, "lobHandler"));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void tesDefaultSerializer() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();
		Serializer<Map<String, Object>> serializer = (Serializer<Map<String,Object>>) ReflectionTestUtils.getField(factory, "serializer");
		assertTrue(serializer instanceof Jackson2ExecutionContextStringSerializer);
	}

	@Test
	public void testCustomSerializer() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		ExecutionContextSerializer customSerializer = new DefaultExecutionContextSerializer();
		factory.setSerializer(customSerializer);

		factory.afterPropertiesSet();
		assertEquals(customSerializer, ReflectionTestUtils.getField(factory, "serializer"));
	}
	
	@Test
	public void testDefaultJdbcOperations() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();
		
		JdbcOperations jdbcOperations = (JdbcOperations) ReflectionTestUtils.getField(factory, "jdbcOperations");
		assertTrue(jdbcOperations instanceof JdbcTemplate);
	}	

	@Test
	public void testCustomJdbcOperations() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);
		
		JdbcOperations customJdbcOperations = mock(JdbcOperations.class);
		factory.setJdbcOperations(customJdbcOperations);
		
		factory.afterPropertiesSet();
		
		assertEquals(customJdbcOperations, ReflectionTestUtils.getField(factory, "jdbcOperations"));
	}	
	
	@Test
	public void testMissingDataSource() throws Exception {

		factory.setDataSource(null);
		try {
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.contains("DataSource"));
		}

	}

	@Test
	public void testMissingTransactionManager() throws Exception {

		factory.setDatabaseType("mockDb");
		factory.setTransactionManager(null);
		try {
			when(incrementerFactory.isSupportedIncrementerType("mockDb")).thenReturn(true);
			when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);

			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.contains("TransactionManager"));
		}

	}

	@Test
	public void testInvalidDatabaseType() throws Exception {

		factory.setDatabaseType("foo");
		try {
			when(incrementerFactory.isSupportedIncrementerType("foo")).thenReturn(false);
			when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.contains("foo"));
		}

	}

	@Test
	public void testCreateRepository() throws Exception {
		String databaseType = "HSQL";
		factory.setDatabaseType(databaseType);

		when(incrementerFactory.isSupportedIncrementerType("HSQL")).thenReturn(true);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_EXECUTION_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "STEP_EXECUTION_SEQ")).thenReturn(new StubIncrementer());

		factory.afterPropertiesSet();
		factory.getObject();
	}

	@Ignore
	@Test
	public void testTransactionAttributesForCreateMethodNullHypothesis() throws Exception {
		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		try {
			repository.createJobExecution("foo", new JobParameters());
			// we expect an exception from the txControl because we provided the
			// wrong meta data
			fail("Expected IllegalArgumentException");
		}
		catch (AssertionError e) {
			// expected exception from txControl - wrong isolation level used in
			// comparison
			assertEquals("Unexpected method call", e.getMessage().substring(3, 25));
		}

	}

	@Test
	public void testTransactionAttributesForCreateMethod() throws Exception {

		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		Connection conn = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(conn);
		try {
			repository.createJobExecution("foo", new JobParameters());
			// we expect an exception but not from the txControl because we
			// provided the correct meta data
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected exception from DataSourceUtils
			assertEquals("No Statement specified", e.getMessage());
		}

	}

	@Test
	public void testSetTransactionAttributesForCreateMethod() throws Exception {

		factory.setIsolationLevelForCreate("ISOLATION_READ_UNCOMMITTED");
		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_READ_UNCOMMITTED);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		Connection conn = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(conn);
		try {
			repository.createJobExecution("foo", new JobParameters());
			// we expect an exception but not from the txControl because we
			// provided the correct meta data
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected exception from DataSourceUtils
			assertEquals("No Statement specified", e.getMessage());
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidCustomLobType() throws Exception {
		factory.setClobType(Integer.MAX_VALUE);
		testCreateRepository();
	}

	@Test
	public void testCustomLobType() throws Exception {
		factory.setClobType(Types.ARRAY);
		testCreateRepository();
		JobRepository repository = factory.getObject();
		assertNotNull(repository);
	}

	private static class StubIncrementer implements DataFieldMaxValueIncrementer {

		@Override
		public int nextIntValue() throws DataAccessException {
			return 0;
		}

		@Override
		public long nextLongValue() throws DataAccessException {
			return 0;
		}

		@Override
		public String nextStringValue() throws DataAccessException {
			return null;
		}

	}

}
