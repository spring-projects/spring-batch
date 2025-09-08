/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.lang.reflect.Method;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MethodMapTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * Convenient factory bean that creates a transactional proxy around a
 * {@link JobOperator}.
 *
 * @see JobOperator
 * @see TaskExecutorJobOperator
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class JobOperatorFactoryBean implements FactoryBean<JobOperator>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(JobOperatorFactoryBean.class);

	private PlatformTransactionManager transactionManager;

	private TransactionAttributeSource transactionAttributeSource;

	private JobRegistry jobRegistry;

	private JobRepository jobRepository;

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private TaskExecutor taskExecutor;

	private MeterRegistry meterRegistry = Metrics.globalRegistry;

	private final ProxyFactory proxyFactory = new ProxyFactory();

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.jobRepository, "JobRepository must not be null");
		Assert.notNull(this.jobRegistry, "JobRegistry must not be null");
		if (this.transactionManager == null) {
			this.transactionManager = new ResourcelessTransactionManager();
			logger.info("No transaction manager has been set, defaulting to ResourcelessTransactionManager.");
		}
		if (this.taskExecutor == null) {
			logger.info("No TaskExecutor has been set, defaulting to synchronous executor.");
			this.taskExecutor = new SyncTaskExecutor();
		}
		if (this.transactionAttributeSource == null) {
			this.transactionAttributeSource = new MethodMapTransactionAttributeSource();
			DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
			Method stopMethod = TaskExecutorJobOperator.class.getMethod("stop", JobExecution.class);
			Method abandonMethod = TaskExecutorJobOperator.class.getMethod("abandon", JobExecution.class);
			Method recoverMethod = TaskExecutorJobOperator.class.getMethod("recover", JobExecution.class);
			((MethodMapTransactionAttributeSource) this.transactionAttributeSource).addTransactionalMethod(stopMethod,
					transactionAttribute);
			((MethodMapTransactionAttributeSource) this.transactionAttributeSource)
				.addTransactionalMethod(abandonMethod, transactionAttribute);
			((MethodMapTransactionAttributeSource) this.transactionAttributeSource)
				.addTransactionalMethod(recoverMethod, transactionAttribute);
		}
	}

	/**
	 * Setter for the job registry.
	 * @param jobRegistry the job registry to set
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Setter for the job repository.
	 * @param jobRepository the job repository to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Setter for the job parameters converter.
	 * @param jobParametersConverter the job parameters converter to set
	 * @deprecated since 6.0 with nor replacement. Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public void setJobParametersConverter(JobParametersConverter jobParametersConverter) {
		this.jobParametersConverter = jobParametersConverter;
	}

	/**
	 * Set the TaskExecutor. (Optional)
	 * @param taskExecutor instance of {@link TaskExecutor}.
	 * @since 6.0
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the meter registry to use for metrics. Defaults to
	 * {@link Metrics#globalRegistry}.
	 * @param meterRegistry the meter registry
	 * @since 6.0
	 */
	public void setMeterRegistry(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Setter for the transaction manager.
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the transaction attributes source to use in the created proxy.
	 * @param transactionAttributeSource the transaction attributes source to use in the
	 * created proxy.
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		Assert.notNull(transactionAttributeSource, "transactionAttributeSource must not be null.");
		this.transactionAttributeSource = transactionAttributeSource;
	}

	@Override
	public Class<?> getObjectType() {
		return JobOperator.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public JobOperator getObject() throws Exception {
		TransactionInterceptor advice = new TransactionInterceptor((TransactionManager) this.transactionManager,
				this.transactionAttributeSource);
		this.proxyFactory.addAdvice(advice);
		this.proxyFactory.setProxyTargetClass(false);
		this.proxyFactory.addInterface(JobOperator.class);
		this.proxyFactory.setTarget(getTarget());
		return (JobOperator) this.proxyFactory.getProxy(getClass().getClassLoader());
	}

	@SuppressWarnings("removal")
	private TaskExecutorJobOperator getTarget() throws Exception {
		TaskExecutorJobOperator taskExecutorJobOperator = new TaskExecutorJobOperator();
		taskExecutorJobOperator.setJobRegistry(this.jobRegistry);
		taskExecutorJobOperator.setJobRepository(this.jobRepository);
		taskExecutorJobOperator.setTaskExecutor(this.taskExecutor);
		taskExecutorJobOperator.setMeterRegistry(this.meterRegistry);
		taskExecutorJobOperator.setJobParametersConverter(this.jobParametersConverter);
		taskExecutorJobOperator.afterPropertiesSet();
		return taskExecutorJobOperator;
	}

}
