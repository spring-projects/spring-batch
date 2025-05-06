/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core.repository.explore.support;

import java.util.Properties;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobExplorer}. It
 * declares abstract methods for providing DAO object implementations.
 *
 * @see JobExplorerFactoryBean
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public abstract class AbstractJobExplorerFactoryBean implements FactoryBean<JobExplorer>, InitializingBean {

	private static final String TRANSACTION_ISOLATION_LEVEL_PREFIX = "ISOLATION_";

	private static final String TRANSACTION_PROPAGATION_PREFIX = "PROPAGATION_";

	private PlatformTransactionManager transactionManager;

	private TransactionAttributeSource transactionAttributeSource;

	private final ProxyFactory proxyFactory = new ProxyFactory();

	/**
	 * Creates a job instance data access object (DAO).
	 * @return a fully configured {@link JobInstanceDao} implementation.
	 * @throws Exception thrown if error occurs during JobInstanceDao creation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * Creates a job execution data access object (DAO).
	 * @return a fully configured {@link JobExecutionDao} implementation.
	 * @throws Exception thrown if error occurs during JobExecutionDao creation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;

	/**
	 * Creates a step execution data access object (DAO).
	 * @return a fully configured {@link StepExecutionDao} implementation.
	 * @throws Exception thrown if error occurs during StepExecutionDao creation.
	 */
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	/**
	 * Creates an execution context instance data access object (DAO).
	 * @return fully configured {@link ExecutionContextDao} implementation.
	 * @throws Exception thrown if error occurs during ExecutionContextDao creation.
	 */
	protected abstract ExecutionContextDao createExecutionContextDao() throws Exception;

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 * @since 5.0
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * The transaction manager used in this factory. Useful to inject into steps and jobs,
	 * to ensure that they are using the same instance.
	 * @return the transactionManager
	 * @since 5.0
	 */
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * Set the transaction attributes source to use in the created proxy.
	 * @param transactionAttributeSource the transaction attributes source to use in the
	 * created proxy.
	 * @since 5.0
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		Assert.notNull(transactionAttributeSource, "transactionAttributeSource must not be null.");
		this.transactionAttributeSource = transactionAttributeSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.transactionManager, "TransactionManager must not be null.");
		if (this.transactionAttributeSource == null) {
			Properties transactionAttributes = new Properties();
			String transactionProperties = String.join(",", TRANSACTION_PROPAGATION_PREFIX + Propagation.SUPPORTS,
					TRANSACTION_ISOLATION_LEVEL_PREFIX + Isolation.DEFAULT);
			transactionAttributes.setProperty("get*", transactionProperties);
			transactionAttributes.setProperty("find*", transactionProperties);
			this.transactionAttributeSource = new NameMatchTransactionAttributeSource();
			((NameMatchTransactionAttributeSource) this.transactionAttributeSource)
				.setProperties(transactionAttributes);
		}
	}

	/**
	 * Returns the type of object to be returned from {@link #getObject()}.
	 * @return {@code JobExplorer.class}
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<JobExplorer> getObjectType() {
		return JobExplorer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public JobExplorer getObject() throws Exception {
		TransactionInterceptor advice = new TransactionInterceptor((TransactionManager) this.transactionManager,
				this.transactionAttributeSource);
		proxyFactory.addAdvice(advice);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.addInterface(JobExplorer.class);
		proxyFactory.setTarget(getTarget());
		return (JobExplorer) proxyFactory.getProxy(getClass().getClassLoader());
	}

	private JobExplorer getTarget() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

}
