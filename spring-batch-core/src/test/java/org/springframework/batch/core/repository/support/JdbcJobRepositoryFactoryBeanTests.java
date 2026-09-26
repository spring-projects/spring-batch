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

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
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

	@Test
	void inheritedLoggerRemainsAvailableToSubclasses() {
		assertThat(new InheritedLoggerRepositoryBean().inheritedLogger()).isNotNull();
	}

	@Test
	void doesNotFailOrWarnWhenScopedDataSourcesCannotBeResolved() throws Exception {

		String warning = "The DataSource configured for the JobRepository does not appear to match";
		StringWriter output = new StringWriter();
		WriterAppender appender = WriterAppender.newBuilder().setName("test").setTarget(output).build();
		Logger logger = (Logger) LogManager.getLogger(JdbcJobRepositoryFactoryBean.class);
		Level originalLevel = logger.getLevel();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.WARN);

		try {
			DataSource scopedDataSource = createScopedDataSourceProxy();
			createFactoryBean(scopedDataSource, new JdbcTransactionManager(scopedDataSource)).afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);

			output.getBuffer().setLength(0);
			createFactoryBean(createScopedDataSourceProxy(), new JdbcTransactionManager(createScopedDataSourceProxy()))
				.afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);

			output.getBuffer().setLength(0);
			DataSource plainTarget = mock();
			createFactoryBean(createClassBasedScopedDataSourceProxy(), new JdbcTransactionManager(plainTarget))
				.afterPropertiesSet();
			assertThat(output.toString()).doesNotContain(warning);

			output.getBuffer().setLength(0);
			DataSource repositoryDataSource = mock();
			createFactoryBean(repositoryDataSource, createScopedTransactionManager(repositoryDataSource))
				.afterPropertiesSet();
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

	private DataSource createScopedDataSourceProxy() {
		return createScopedDataSourceProxy(JdbcDataSource.class, false);
	}

	private DataSource createClassBasedScopedDataSourceProxy() {
		return createScopedDataSourceProxy(TransactionAwareDataSourceProxy.class, true);
	}

	private PlatformTransactionManager createScopedTransactionManager(DataSource dataSource) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerScope("inactive", inactiveScope());
		RootBeanDefinition targetDefinition = new RootBeanDefinition(JdbcTransactionManager.class);
		targetDefinition.setScope("inactive");
		targetDefinition.setInstanceSupplier(() -> new JdbcTransactionManager(dataSource));
		BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, "transactionManager");
		BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(targetHolder, beanFactory, false);
		beanFactory.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
		return (PlatformTransactionManager) beanFactory.getBean(proxyHolder.getBeanName());
	}

	private DataSource createScopedDataSourceProxy(Class<?> targetType, boolean proxyTargetClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerScope("inactive", inactiveScope());
		RootBeanDefinition targetDefinition = new RootBeanDefinition(targetType);
		targetDefinition.setScope("inactive");
		BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, "dataSource");
		BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(targetHolder, beanFactory,
				proxyTargetClass);
		beanFactory.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
		return (DataSource) beanFactory.getBean(proxyHolder.getBeanName());
	}

	private Scope inactiveScope() {
		return new Scope() {

			@Override
			public Object get(String name, ObjectFactory<?> objectFactory) {
				throw new IllegalStateException("Scope 'inactive' is not active");
			}

			@Override
			public Object remove(String name) {
				return null;
			}

			@Override
			public void registerDestructionCallback(String name, Runnable callback) {
			}

			@Override
			public Object resolveContextualObject(String key) {
				return null;
			}

			@Override
			public String getConversationId() {
				return null;
			}
		};
	}

	private static class InheritedLoggerRepositoryBean extends JdbcJobRepositoryFactoryBean {

		Log inheritedLogger() {
			return logger;
		}

	}

}
