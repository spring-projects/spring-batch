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

import io.micrometer.observation.ObservationRegistry;

import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
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
 * Customization is possible by extending the class and overriding getters.
 * <p>
 * A typical usage of this class is as follows: <pre class="code">
 * &#064;Configuration
 * public class MyJobConfiguration extends DefaultBatchConfiguration {
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
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
@Import({ ScopeConfiguration.class, BatchObservabilityBeanPostProcessor.class })
public class DefaultBatchConfiguration implements ApplicationContextAware {

	protected ApplicationContext applicationContext;

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
		jobOperatorFactoryBean.setJobRegistry(getJobRegistry());
		jobOperatorFactoryBean.setTransactionManager(getTransactionManager());
		jobOperatorFactoryBean.setObservationRegistry(getObservationRegistry());
		jobOperatorFactoryBean.setJobParametersConverter(getJobParametersConverter());
		jobOperatorFactoryBean.setTaskExecutor(getTaskExecutor());
		try {
			jobOperatorFactoryBean.afterPropertiesSet();
			return jobOperatorFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job operator", e);
		}
	}

	protected JobRegistry getJobRegistry() {
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
	 * Return the {@link ObservationRegistry} to use for the job operator. Defaults to
	 * {@link ObservationRegistry#NOOP}.
	 * @return The ObservationRegistry to use for the job operator
	 * @since 6.0
	 */
	protected ObservationRegistry getObservationRegistry() {
		return ObservationRegistry.NOOP;
	}

	/**
	 * Return the transaction manager to use for the job operator. Defaults to
	 * {@link ResourcelessTransactionManager}.
	 * @return The transaction manager to use for the job operator
	 */
	protected PlatformTransactionManager getTransactionManager() {
		return new ResourcelessTransactionManager();
	}

	/**
	 * Return the {@link TaskExecutor} to use in the job operator. Defaults to
	 * {@link SyncTaskExecutor}.
	 * @return the {@link TaskExecutor} to use in the job operator.
	 */
	protected TaskExecutor getTaskExecutor() {
		return new SyncTaskExecutor();
	}

	/**
	 * Return the {@link JobParametersConverter} to use in the job operator. Defaults to
	 * {@link DefaultJobParametersConverter}
	 * @return the {@link JobParametersConverter} to use in the job operator.
	 * @deprecated since 6.0 with no replacement and scheduled for removal in 6.2 or
	 * later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	protected JobParametersConverter getJobParametersConverter() {
		return new DefaultJobParametersConverter();
	}

}
