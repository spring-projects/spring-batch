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
package org.springframework.batch.core.repository.support;

import java.sql.Connection;

import javax.sql.DataSource;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author Lucas Ward
 * 
 */
public class JobRepositoryFactoryBeanTests extends TestCase {

	private JobRepositoryFactoryBean factory;

	private MockControl incrementerControl = MockControl.createControl(DataFieldMaxValueIncrementerFactory.class);

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private String tablePrefix = "TEST_BATCH_PREFIX_";

	private MockControl txControl;

	private MockControl dataSourceControl;

	protected void setUp() throws Exception {
		super.setUp();

		factory = new JobRepositoryFactoryBean();
		dataSourceControl = MockControl.createControl(DataSource.class);
		dataSource = (DataSource) dataSourceControl.getMock();
		txControl = MockControl.createControl(PlatformTransactionManager.class);
		transactionManager = (PlatformTransactionManager) txControl.getMock();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
		incrementerFactory = (DataFieldMaxValueIncrementerFactory) incrementerControl.getMock();
		factory.setIncrementerFactory(incrementerFactory);
		factory.setTablePrefix(tablePrefix);
	}

	public void testNoDatabaseType() throws Exception {

		try {
			incrementerFactory.isSupportedIncrementerType(null);
			incrementerControl.setReturnValue(false);
			incrementerFactory.getSupportedIncrementerTypes();
			incrementerControl.setReturnValue(new String[0]);
			incrementerControl.replay();
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("unsupported database type") >= 0);
		}
	}

	public void testMissingDataSource() throws Exception {

		factory.setDataSource(null);
		try {
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("DataSource") >= 0);
		}
	}

	public void testMissingTransactionManager() throws Exception {

		factory.setTransactionManager(null);
		try {
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("TransactionManager") >= 0);
		}
	}

	public void testInvalidDatabaseType() throws Exception {

		factory.setDatabaseType("foo");
		try {
			incrementerFactory.isSupportedIncrementerType("foo");
			incrementerControl.setReturnValue(false);
			incrementerFactory.getSupportedIncrementerTypes();
			incrementerControl.setReturnValue(new String[0]);
			incrementerControl.replay();
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("foo") >= 0);
		}
	}

	public void testCreateRepository() throws Exception {
		String databaseType = "foo";
		factory.setDatabaseType(databaseType);

		incrementerFactory.isSupportedIncrementerType("foo");
		incrementerControl.setReturnValue(true);
		incrementerFactory.getSupportedIncrementerTypes();
		incrementerControl.setReturnValue(new String[0]);
		incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_EXECUTION_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerFactory.getIncrementer(databaseType, tablePrefix + "STEP_EXECUTION_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerControl.replay();

		factory.afterPropertiesSet();
		factory.getObject();

		incrementerControl.verify();
	}

	public void testTransactionAttributesForCreateMethodNullHypothesis() throws Exception {
		testCreateRepository();
		JobRepository repository = (JobRepository) factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txControl.expectAndReturn(transactionManager.getTransaction(transactionDefinition), null);
		txControl.replay();
		try {
			repository.createJobExecution(new JobSupport("job"), new JobParameters());
			// we expect an exception from the txControl because we provided the
			// wrong meta data
			fail("Expected IllegalArgumentException");
		}
		catch (AssertionFailedError e) {
			// expected exception from txControl - wrong isolation level used in
			// comparison
			assertEquals("Unexpected method call", e.getMessage().substring(3, 25));
		}
	}

	public void testTransactionAttributesForCreateMethod() throws Exception {
		testCreateRepository();
		JobRepository repository = (JobRepository) factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
		txControl.expectAndReturn(transactionManager.getTransaction(transactionDefinition), null);
		dataSourceControl.expectAndReturn(dataSource.getConnection(), MockControl.createControl(Connection.class)
				.getMock());
		dataSourceControl.replay();
		txControl.replay();
		try {
			repository.createJobExecution(new JobSupport("job"), new JobParameters());
			// we expect an exception but not from the txControl because we
			// provided the correct meta data
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected exception from DataSourceUtils
			assertEquals("No Statement specified", e.getMessage());
		}
	}

	public void testSetTransactionAttributesForCreateMethod() throws Exception {
		factory.setIsolationLevelForCreate("ISOLATION_READ_UNCOMMITTED");
		testCreateRepository();
		JobRepository repository = (JobRepository) factory.getObject();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
				DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDefinition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_READ_UNCOMMITTED);
		txControl.expectAndReturn(transactionManager.getTransaction(transactionDefinition), null);
		dataSourceControl.expectAndReturn(dataSource.getConnection(), MockControl.createControl(Connection.class)
				.getMock());
		dataSourceControl.replay();
		txControl.replay();
		try {
			repository.createJobExecution(new JobSupport("job"), new JobParameters());
			// we expect an exception but not from the txControl because we
			// provided the correct meta data
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected exception from DataSourceUtils
			assertEquals("No Statement specified", e.getMessage());
		}
	}

	private static class StubIncrementer implements DataFieldMaxValueIncrementer {

		public int nextIntValue() throws DataAccessException {
			return 0;
		}

		public long nextLongValue() throws DataAccessException {
			return 0;
		}

		public String nextStringValue() throws DataAccessException {
			return null;
		}

	}
}
