/*
 * Copyright 2026 the original author or authors.
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

import java.io.StringWriter;

import javax.sql.DataSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcJobRepositoryFactoryBeanTests {

	@Test
	void warnsOnlyWhenDataSourcesDoNotMatch() throws Exception {

		String warning = "The DataSource configured for the JobRepository does not appear to match";
		StringWriter output = new StringWriter();
		WriterAppender appender = WriterAppender.newBuilder().setName("test").setTarget(output).build();
		Logger logger = (Logger) LogManager.getLogger(JdbcJobRepositoryFactoryBean.class);
		Level originalLevel = logger.getLevel();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.WARN);

		try {
			createFactoryBean(mock(), new JdbcTransactionManager(mock())).afterPropertiesSet();
			assertThat(output.toString()).contains(warning);

			output.getBuffer().setLength(0);
			DataSource targetDataSource = mock();
			createFactoryBean(new TransactionAwareDataSourceProxy(targetDataSource),
					new JdbcTransactionManager(targetDataSource))
				.afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);

			output.getBuffer().setLength(0);
			createFactoryBean(mock(), mock(PlatformTransactionManager.class)).afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);

			output.getBuffer().setLength(0);
			ResourceTransactionManager transactionManager = mock();
			when(transactionManager.getResourceFactory()).thenReturn(new Object());
			createFactoryBean(mock(), transactionManager).afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);
		}
		finally {
			logger.removeAppender(appender);
			logger.setLevel(originalLevel);
			appender.stop();
		}
	}

	private JdbcJobRepositoryFactoryBean createFactoryBean(DataSource dataSource,
			PlatformTransactionManager transactionManager) {
		JdbcJobRepositoryFactoryBean factoryBean = new JdbcJobRepositoryFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setTransactionManager(transactionManager);
		factoryBean.setDatabaseType("H2");
		return factoryBean;
	}

}
