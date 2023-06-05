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
package org.springframework.batch.core.repository.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class JobRepositoryFactoryBeanTests {

	private JobRepositoryFactoryBean factory;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private final String tablePrefix = "TEST_BATCH_PREFIX_";

	@BeforeEach
	void setUp() {

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
	void testNoDatabaseType() throws Exception {

		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		Connection con = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(con);
		when(con.getMetaData()).thenReturn(dmd);
		when(dmd.getDatabaseProductName()).thenReturn("Oracle");

		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());

		factory.afterPropertiesSet();
		factory.getObject();

	}

	@Test
	void testOracleLobHandler() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();
		LobHandler lobHandler = (LobHandler) ReflectionTestUtils.getField(factory, "lobHandler");
		assertTrue(lobHandler instanceof DefaultLobHandler);

	}

	@Test
	void testCustomLobHandler() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		LobHandler lobHandler = new DefaultLobHandler();
		factory.setLobHandler(lobHandler);

		factory.afterPropertiesSet();
		assertEquals(lobHandler, ReflectionTestUtils.getField(factory, "lobHandler"));

	}

	@Test
	@SuppressWarnings("unchecked")
	void tesDefaultSerializer() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();
		Serializer<Map<String, Object>> serializer = (Serializer<Map<String, Object>>) ReflectionTestUtils
			.getField(factory, "serializer");
		assertTrue(serializer instanceof DefaultExecutionContextSerializer);
	}

	@Test
	void testCustomSerializer() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		ExecutionContextSerializer customSerializer = new DefaultExecutionContextSerializer();
		factory.setSerializer(customSerializer);

		factory.afterPropertiesSet();
		assertEquals(customSerializer, ReflectionTestUtils.getField(factory, "serializer"));
	}

	@Test
	void testDefaultJdbcOperations() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		factory.afterPropertiesSet();

		JdbcOperations jdbcOperations = (JdbcOperations) ReflectionTestUtils.getField(factory, "jdbcOperations");
		assertTrue(jdbcOperations instanceof JdbcTemplate);
	}

	@Test
	void testCustomJdbcOperations() throws Exception {

		factory.setDatabaseType("ORACLE");

		incrementerFactory = mock(DataFieldMaxValueIncrementerFactory.class);
		when(incrementerFactory.isSupportedIncrementerType("ORACLE")).thenReturn(true);
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_SEQ")).thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer("ORACLE", tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		factory.setIncrementerFactory(incrementerFactory);

		JdbcOperations customJdbcOperations = mock(JdbcOperations.class);
		factory.setJdbcOperations(customJdbcOperations);

		factory.afterPropertiesSet();

		assertEquals(customJdbcOperations, ReflectionTestUtils.getField(factory, "jdbcOperations"));
	}

	@Test
	void testMissingDataSource() {

		factory.setDataSource(null);
		Exception exception = assertThrows(IllegalStateException.class, factory::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("DataSource"), "Wrong message: " + message);

	}

	@Test
	void testMissingTransactionManager() {

		factory.setDatabaseType("mockDb");
		factory.setTransactionManager(null);
		when(incrementerFactory.isSupportedIncrementerType("mockDb")).thenReturn(true);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);

		Exception exception = assertThrows(IllegalStateException.class, () -> factory.afterPropertiesSet());
		String message = exception.getMessage();
		assertTrue(message.contains("TransactionManager"), "Wrong message: " + message);

	}

	@Test
	void testInvalidDatabaseType() {

		factory.setDatabaseType("foo");
		when(incrementerFactory.isSupportedIncrementerType("foo")).thenReturn(false);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);

		Exception exception = assertThrows(IllegalStateException.class, () -> factory.afterPropertiesSet());
		String message = exception.getMessage();
		assertTrue(message.contains("foo"), "Wrong message: " + message);

	}

	@Test
	void testCreateRepository() throws Exception {
		String databaseType = "HSQL";
		factory.setDatabaseType(databaseType);

		when(incrementerFactory.isSupportedIncrementerType("HSQL")).thenReturn(true);
		when(incrementerFactory.getSupportedIncrementerTypes()).thenReturn(new String[0]);
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());
		when(incrementerFactory.getIncrementer(databaseType, tablePrefix + "STEP_EXECUTION_SEQ"))
			.thenReturn(new StubIncrementer());

		factory.afterPropertiesSet();
		factory.getObject();
	}

	@Disabled
	@Test
	void testTransactionAttributesForCreateMethodNullHypothesis() throws Exception {
		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		Error error = assertThrows(AssertionError.class,
				() -> repository.createJobExecution("foo", new JobParameters()));
		assertEquals("Unexpected method call", error.getMessage().substring(3, 25));
	}

	@Test
	void testTransactionAttributesForCreateMethod() throws Exception {

		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		Connection conn = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(conn);
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> repository.createJobExecution("foo", new JobParameters()));
		assertEquals("No Statement specified", exception.getMessage());

	}

	@Test
	void testSetTransactionAttributesForCreateMethod() throws Exception {

		factory.setIsolationLevelForCreateEnum(Isolation.READ_UNCOMMITTED);
		testCreateRepository();
		JobRepository repository = factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_READ_UNCOMMITTED);
		when(transactionManager.getTransaction(transactionDefinition)).thenReturn(null);
		Connection conn = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(conn);
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> repository.createJobExecution("foo", new JobParameters()));
		assertEquals("No Statement specified", exception.getMessage());
	}

	@Test
	public void testCustomTransactionAttributesSource() throws Exception {
		// given
		TransactionAttributeSource transactionAttributeSource = Mockito.mock(TransactionAttributeSource.class);
		this.factory.setTransactionAttributeSource(transactionAttributeSource);

		// when
		testCreateRepository();
		JobRepository repository = this.factory.getObject();

		// then
		Advised target = (Advised) repository;
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor transactionInterceptor) {
				Assertions.assertEquals(transactionAttributeSource,
						transactionInterceptor.getTransactionAttributeSource());
			}
		}
	}

	@Test
	void testInvalidCustomLobType() {
		factory.setClobType(Integer.MAX_VALUE);
		assertThrows(IllegalStateException.class, this::testCreateRepository);
	}

	@Test
	void testCustomLobType() throws Exception {
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
