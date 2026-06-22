/*
 * Copyright 2026-present the original author or authors.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link JdbcJobRepositoryFactoryBean}.
 */
class JdbcJobRepositoryFactoryBeanTests {

	private static final String MISMATCH_WARNING = "The DataSource configured for the JobRepository does not appear to "
			+ "match the DataSource managed by the configured transaction manager.";

	private Logger logger;

	private Level originalLevel;

	private TestAppender appender;

	@BeforeEach
	void setUp() {
		this.logger = (Logger) LogManager.getLogger(JdbcJobRepositoryFactoryBean.class);
		this.originalLevel = this.logger.getLevel();
		this.appender = new TestAppender();
		this.appender.start();
		this.logger.addAppender(this.appender);
		this.logger.setLevel(Level.WARN);
	}

	@AfterEach
	void tearDown() {
		this.logger.removeAppender(this.appender);
		this.logger.setLevel(this.originalLevel);
		this.appender.stop();
	}

	@Test
	void afterPropertiesSetWhenTransactionManagerUsesSameDataSourceDoesNotLogWarning() throws Exception {
		DataSource dataSource = mock();
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource,
				new JdbcTransactionManager(dataSource));

		factoryBean.afterPropertiesSet();

		assertFalse(this.appender.containsMessage(MISMATCH_WARNING));
	}

	@Test
	void afterPropertiesSetWhenTransactionManagerUsesDifferentDataSourceLogsWarning() throws Exception {
		DataSource dataSource = mock();
		DataSource transactionManagerDataSource = mock();
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource,
				new JdbcTransactionManager(transactionManagerDataSource));

		factoryBean.afterPropertiesSet();

		assertTrue(this.appender.containsMessage(MISMATCH_WARNING));
	}

	@Test
	void afterPropertiesSetWhenTransactionManagerIsNotResourceTransactionManagerDoesNotLogWarning() throws Exception {
		DataSource dataSource = mock();
		PlatformTransactionManager transactionManager = mock();
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource, transactionManager);

		factoryBean.afterPropertiesSet();

		assertFalse(this.appender.containsMessage(MISMATCH_WARNING));
	}

	@Test
	void afterPropertiesSetWhenTransactionManagerResourceFactoryIsNotDataSourceDoesNotLogWarning() throws Exception {
		DataSource dataSource = mock();
		ResourceTransactionManager transactionManager = mock();
		when(transactionManager.getResourceFactory()).thenReturn("resource");
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource, transactionManager);

		factoryBean.afterPropertiesSet();

		assertFalse(this.appender.containsMessage(MISMATCH_WARNING));
	}

	@Test
	void afterPropertiesSetWhenTransactionManagerResourceFactoryIsNullDoesNotLogWarning() throws Exception {
		DataSource dataSource = mock();
		ResourceTransactionManager transactionManager = mock();
		when(transactionManager.getResourceFactory()).thenReturn(null);
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource, transactionManager);

		factoryBean.afterPropertiesSet();

		assertFalse(this.appender.containsMessage(MISMATCH_WARNING));
	}

	@Test
	void afterPropertiesSetWhenDataSourceIsTransactionAwareProxyUsesTargetDataSource() throws Exception {
		DataSource targetDataSource = mock();
		TransactionAwareDataSourceProxy dataSource = new TransactionAwareDataSourceProxy(targetDataSource);
		JdbcJobRepositoryFactoryBean factoryBean = createFactoryBean(dataSource,
				new JdbcTransactionManager(targetDataSource));

		factoryBean.afterPropertiesSet();

		assertFalse(this.appender.containsMessage(MISMATCH_WARNING));
	}

	private JdbcJobRepositoryFactoryBean createFactoryBean(DataSource dataSource,
			PlatformTransactionManager transactionManager) {
		JdbcJobRepositoryFactoryBean factoryBean = new JdbcJobRepositoryFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(transactionManager);
		factoryBean.setDatabaseType("H2");
		return factoryBean;
	}

	private static class TestAppender extends AbstractAppender {

		private final List<LogEvent> events = new CopyOnWriteArrayList<>();

		TestAppender() {
			super("test", null, null, false, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			this.events.add(event.toImmutable());
		}

		boolean containsMessage(String message) {
			return this.events.stream().anyMatch(event -> event.getMessage().getFormattedMessage().contains(message));
		}

	}

}
