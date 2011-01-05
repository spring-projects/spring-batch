/*
 * Copyright 2006-2007 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository}. Declares abstract methods for providing DAO
 * object implementations.
 * 
 * @see JobRepositoryFactoryBean
 * @see MapJobRepositoryFactoryBean
 * 
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJobRepositoryFactoryBean implements FactoryBean, InitializingBean {

	private PlatformTransactionManager transactionManager;

	private ProxyFactory proxyFactory;

	private String isolationLevelForCreate = DEFAULT_ISOLATION_LEVEL;

	private boolean validateTransactionState = true;

	/**
	 * Default value for isolation level in create* method.
	 */
	private static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_SERIALIZABLE";

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link StepExecutionDao} implementation.
	 */
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	/**
	 * @return fully configured {@link ExecutionContextDao} implementation.
	 */
	protected abstract ExecutionContextDao createExecutionContextDao() throws Exception;

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * 
	 * @return JobRepository.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<JobRepository> getObjectType() {
		return JobRepository.class;
	}

	public boolean isSingleton() {
		return true;
	}

	/**
	 * Flag to determine whether to check for an existing transaction when a
	 * JobExecution is created. Defaults to true because it is usually a
	 * mistake, and leads to problems with restartability and also to deadlocks
	 * in multi-threaded steps.
	 * 
	 * @param validateTransactionState the flag to set
	 */
	public void setValidateTransactionState(boolean validateTransactionState) {
		this.validateTransactionState = validateTransactionState;
	}

	/**
	 * public setter for the isolation level to be used for the transaction when
	 * job execution entities are initially created. The default is
	 * ISOLATION_SERIALIZABLE, which prevents accidental concurrent execution of
	 * the same job (ISOLATION_REPEATABLE_READ would work as well).
	 * 
	 * @param isolationLevelForCreate the isolation level name to set
	 * 
	 * @see SimpleJobRepository#createJobExecution(String,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreate(String isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * The transaction manager used in this factory. Useful to inject into steps
	 * and jobs, to ensure that they are using the same instance.
	 * 
	 * @return the transactionManager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	/**
	 * Convenience method for clients to grab the {@link JobRepository} without
	 * a cast.
	 * @return the {@link JobRepository} from {@link #getObject()}
	 * @throws Exception if the repository could not be created
	 */
	public JobRepository getJobRepository() throws Exception {
		return (JobRepository) getObject();
	}

	private void initializeProxy() throws Exception {
		if (proxyFactory == null) {
			proxyFactory = new ProxyFactory();
			TransactionInterceptor advice = new TransactionInterceptor(transactionManager,
					PropertiesConverter.stringToProperties("create*=PROPAGATION_REQUIRES_NEW,"
							+ isolationLevelForCreate + "\ngetLastJobExecution*=PROPAGATION_REQUIRES_NEW,"
							+ isolationLevelForCreate + "\n*=PROPAGATION_REQUIRED"));
			if (validateTransactionState) {
				DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(new MethodInterceptor() {
					public Object invoke(MethodInvocation invocation) throws Throwable {
						if (TransactionSynchronizationManager.isActualTransactionActive()) {
							throw new IllegalStateException(
									"Existing transaction detected in JobRepository. "
											+ "Please fix this and try again (e.g. remove @Transactional annotations from client).");
						}
						return invocation.proceed();
					}
				});
				NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
				pointcut.addMethodName("create*");
				advisor.setPointcut(pointcut);
				proxyFactory.addAdvisor(advisor);
			}
			proxyFactory.addAdvice(advice);
			proxyFactory.setProxyTargetClass(false);
			proxyFactory.addInterface(JobRepository.class);
			proxyFactory.setTarget(getTarget());
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(transactionManager, "TransactionManager must not be null.");

		initializeProxy();
	}

	private Object getTarget() throws Exception {
		return new SimpleJobRepository(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

	public Object getObject() throws Exception {
		if (proxyFactory == null) {
			afterPropertiesSet();
		}
		return proxyFactory.getProxy();
	}

}
