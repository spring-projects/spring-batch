/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base {@link Configuration} class that provides common infrastructure beans for enabling
 * and using Spring Batch.
 * <p>
 * This configuration class configures and registers the following beans in the
 * application context:
 *
 * <ul>
 * <li>a {@link ResourcelessJobRepository} named "jobRepository"</li>
 * <li>a {@link TaskExecutorJobOperator} named "jobOperator"</li>
 * <li>a {@link org.springframework.batch.core.scope.StepScope} named "stepScope"</li>
 * <li>a {@link org.springframework.batch.core.scope.JobScope} named "jobScope"</li>
 * </ul>
 *
 * <p>
 * A typical usage of this class is as follows: <pre class="code">
 * &#064;Configuration
 * &#064;Import(DefaultBatchConfiguration.class)
 * public class MyJobConfiguration {
 *
 *     &#064;Bean
 *     public Job job(JobRepository jobRepository) {
 *         return new JobBuilder("myJob", jobRepository)
 *                 // define job flow as needed
 *                 .build();
 *     }
 *
 * }
 * </pre>
 *
 * Customization is possible by defining configurable artefacts (transaction manager,
 * task executor, etc) as beans in the application context.
 *
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
@Import(ScopeConfiguration.class)
public class DefaultBatchConfiguration implements ApplicationContextAware {

	protected ApplicationContext applicationContext;

	@Autowired
	protected ObjectProvider<PlatformTransactionManager> transactionManagerObjectProvider;

	@Autowired
	protected ObjectProvider<JobParametersConverter> jobParametersConverterObjectProvider;

	@Autowired
	protected ObjectProvider<TaskExecutor> taskExecutorObjectProvider;

	@Autowired
	protected ObjectProvider<JobRegistry> jobRegistryObjectProvider;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	public JobRepository jobRepository() {
		return new ResourcelessJobRepository();
	}

	@Bean
	public JobOperator jobOperator(JobRepository jobRepository) throws BatchConfigurationException {
		JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
		jobOperatorFactoryBean.setJobRepository(jobRepository);
		jobOperatorFactoryBean.setJobRegistry(jobRegistryObjectProvider.getIfAvailable(() -> getDefaultJobRegistry()));
		jobOperatorFactoryBean.setTransactionManager(
				transactionManagerObjectProvider.getIfAvailable(() -> getDefaultTransactionManager()));
		jobOperatorFactoryBean.setJobParametersConverter(
				jobParametersConverterObjectProvider.getIfAvailable(() -> getDefaultJobParametersConverter()));
		jobOperatorFactoryBean.setTaskExecutor(taskExecutorObjectProvider.getIfAvailable(() -> getDefaultTaskExecutor()));
        // TODO configure meter registry and transaction attribute source
		try {
			jobOperatorFactoryBean.afterPropertiesSet();
			return jobOperatorFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job operator", e);
		}
	}

    /**
     * Return the default {@link JobRegistry} to use for the job operator. By default, it
     * is populated with jobs from the application context.
     * @return The job registry to use for the job operator
     */
	private JobRegistry getDefaultJobRegistry() {
		MapJobRegistry jobRegistry = new MapJobRegistry();
		this.applicationContext.getBeansOfType(Job.class).values().forEach(job -> {
			try {
				jobRegistry.register(job);
			}
			catch (DuplicateJobException e) {
				throw new BatchConfigurationException(e);
			}
		});
		return jobRegistry;
	}

	/**
	 * Return the default {@link PlatformTransactionManager} to use for the job operator.
	 * @return The transaction manager to use for the job operator
	 */
	private PlatformTransactionManager getDefaultTransactionManager() {
		return new ResourcelessTransactionManager();
	}

	/**
	 * Return the default {@link TaskExecutor} to use in the job operator.
	 * @return the {@link TaskExecutor} to use in the job operator.
	 */
	private TaskExecutor getDefaultTaskExecutor() {
		return new SyncTaskExecutor();
	}

	/**
	 * Return the default {@link JobParametersConverter} to use in the job operator.
	 * @return the {@link JobParametersConverter} to use in the job operator.
	 */
	private JobParametersConverter getDefaultJobParametersConverter() {
		return new DefaultJobParametersConverter();
	}

}
