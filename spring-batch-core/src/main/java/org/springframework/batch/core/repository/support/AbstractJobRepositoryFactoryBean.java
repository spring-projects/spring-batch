/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Properties;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.repository.JobRepository;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobRepository}.
 * Declares abstract methods for providing DAO object implementations.
 *
 * @see JdbcJobRepositoryFactoryBean
 * @see MongoJobRepositoryFactoryBean
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public abstract class AbstractJobRepositoryFactoryBean implements FactoryBean<JobRepository>, InitializingBean {

	private PlatformTransactionManager transactionManager;

	private TransactionAttributeSource transactionAttributeSource;

	private final ProxyFactory proxyFactory = new ProxyFactory();

	private String isolationLevelForCreate = DEFAULT_ISOLATION_LEVEL;

	private boolean validateTransactionState = true;

	private static final String TRANSACTION_ISOLATION_LEVEL_PREFIX = "ISOLATION_";

	private static final String TRANSACTION_PROPAGATION_PREFIX = "PROPAGATION_";

	/**
	 * Default value for isolation level in create* method.
	 */
	private static final String DEFAULT_ISOLATION_LEVEL = TRANSACTION_ISOLATION_LEVEL_PREFIX + "SERIALIZABLE";

	protected JobKeyGenerator jobKeyGenerator;

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 * @throws Exception thrown if error occurs creating JobInstanceDao.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 * @throws Exception thrown if error occurs creating JobExecutionDao.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link StepExecutionDao} implementation.
	 * @throws Exception thrown if error occurs creating StepExecutionDao.
	 */
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link ExecutionContextDao} implementation.
	 * @throws Exception thrown if error occurs creating ExecutionContextDao.
	 */
	protected abstract ExecutionContextDao createExecutionContextDao() throws Exception;

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * @return JobRepository.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<JobRepository> getObjectType() {
		return JobRepository.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Flag to determine whether to check for an existing transaction when a JobExecution
	 * is created. Defaults to true because it is usually a mistake, and leads to problems
	 * with restartability and also to deadlocks in multi-threaded steps.
	 * @param validateTransactionState the flag to set
	 */
	public void setValidateTransactionState(boolean validateTransactionState) {
		this.validateTransactionState = validateTransactionState;
	}

	/**
	 * public setter for the isolation level to be used for the transaction when job
	 * execution entities are initially created. The default is ISOLATION_SERIALIZABLE,
	 * which prevents accidental concurrent execution of the same job
	 * (ISOLATION_REPEATABLE_READ would work as well).
	 * @param isolationLevelForCreate the isolation level name to set
	 *
	 * @see SimpleJobRepository#createJobExecution(String,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreate(String isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
	}

	/**
	 * public setter for the isolation level to be used for the transaction when job
	 * execution entities are initially created. The default is ISOLATION_SERIALIZABLE,
	 * which prevents accidental concurrent execution of the same job
	 * (ISOLATION_REPEATABLE_READ would work as well).
	 * @param isolationLevelForCreate the isolation level to set
	 *
	 * @see SimpleJobRepository#createJobExecution(String,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreateEnum(Isolation isolationLevelForCreate) {
		this.setIsolationLevelForCreate(TRANSACTION_ISOLATION_LEVEL_PREFIX + isolationLevelForCreate.name());
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * The transaction manager used in this factory. Useful to inject into steps and jobs,
	 * to ensure that they are using the same instance.
	 * @return the transactionManager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
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

	/**
	 * * Sets the generator for creating the key used in identifying unique {link
	 * JobInstance} objects
	 * @param jobKeyGenerator a {@link JobKeyGenerator}
	 * @since 5.1
	 */
	public void setJobKeyGenerator(JobKeyGenerator jobKeyGenerator) {
		this.jobKeyGenerator = jobKeyGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(transactionManager != null, "TransactionManager must not be null.");
		if (jobKeyGenerator == null) {
			jobKeyGenerator = new DefaultJobKeyGenerator();
		}
		if (this.transactionAttributeSource == null) {
			Properties transactionAttributes = new Properties();
			transactionAttributes.setProperty("create*",
					TRANSACTION_PROPAGATION_PREFIX + Propagation.REQUIRES_NEW + "," + this.isolationLevelForCreate);
			transactionAttributes.setProperty("getLastJobExecution*",
					TRANSACTION_PROPAGATION_PREFIX + Propagation.REQUIRES_NEW + "," + this.isolationLevelForCreate);
			transactionAttributes.setProperty("*", "PROPAGATION_REQUIRED");
			this.transactionAttributeSource = new NameMatchTransactionAttributeSource();
			((NameMatchTransactionAttributeSource) this.transactionAttributeSource)
				.setProperties(transactionAttributes);
		}
	}

	@Override
	public JobRepository getObject() throws Exception {
		TransactionInterceptor advice = new TransactionInterceptor((TransactionManager) this.transactionManager,
				this.transactionAttributeSource);
		if (this.validateTransactionState) {
			DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor((MethodInterceptor) invocation -> {
				if (TransactionSynchronizationManager.isActualTransactionActive()) {
					throw new IllegalStateException("Existing transaction detected in JobRepository. "
							+ "Please fix this and try again (e.g. remove @Transactional annotations from client).");
				}
				return invocation.proceed();
			});
			NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
			pointcut.addMethodName("create*");
			advisor.setPointcut(pointcut);
			this.proxyFactory.addAdvisor(advisor);
		}
		this.proxyFactory.addAdvice(advice);
		this.proxyFactory.setProxyTargetClass(false);
		this.proxyFactory.addInterface(JobRepository.class);
		this.proxyFactory.setTarget(getTarget());
		return (JobRepository) this.proxyFactory.getProxy(getClass().getClassLoader());
	}

	private Object getTarget() throws Exception {
		return new SimpleJobRepository(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

}
