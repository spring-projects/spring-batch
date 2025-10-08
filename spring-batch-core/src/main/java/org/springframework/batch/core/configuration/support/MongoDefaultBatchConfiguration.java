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

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.job.DefaultJobKeyGenerator;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobKeyGenerator;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.mongodb.MongoSequenceIncrementer;
import org.springframework.batch.core.repository.support.MongoJobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.annotation.Isolation;

/**
 * Base {@link Configuration} class that provides common MongoDB-based infrastructure
 * beans for enabling and using Spring Batch.
 * <p>
 * This configuration class configures and registers the following beans in the
 * application context:
 *
 * <ul>
 * <li>a {@link JobRepository} named "jobRepository"</li>
 * <li>a {@link JobOperator} named "jobOperator"</li>
 * <li>a {@link org.springframework.batch.core.scope.StepScope} named "stepScope"</li>
 * <li>a {@link org.springframework.batch.core.scope.JobScope} named "jobScope"</li>
 * </ul>
 *
 * Customization is possible by extending the class and overriding getters.
 * <p>
 * A typical usage of this class is as follows: <pre class="code">
 * &#064;Configuration
 * public class MyJobConfiguration extends MongoDefaultBatchConfiguration {
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
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
@Configuration(proxyBeanMethods = false)
public class MongoDefaultBatchConfiguration extends DefaultBatchConfiguration {

	@Bean
	@Override
	public JobRepository jobRepository() throws BatchConfigurationException {
		MongoJobRepositoryFactoryBean jobRepositoryFactoryBean = new MongoJobRepositoryFactoryBean();
		try {
			jobRepositoryFactoryBean.setMongoOperations(getMongoOperations());
			jobRepositoryFactoryBean.setTransactionManager(getTransactionManager());
			jobRepositoryFactoryBean.setIsolationLevelForCreateEnum(getIsolationLevelForCreate());
			jobRepositoryFactoryBean.setValidateTransactionState(getValidateTransactionState());
			jobRepositoryFactoryBean.setJobKeyGenerator(getJobKeyGenerator());
			jobRepositoryFactoryBean.setJobInstanceIncrementer(getJobInstanceIncrementer());
			jobRepositoryFactoryBean.setJobExecutionIncrementer(getJobExecutionIncrementer());
			jobRepositoryFactoryBean.setStepExecutionIncrementer(getStepExecutionIncrementer());
			jobRepositoryFactoryBean.afterPropertiesSet();
			return jobRepositoryFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job repository", e);
		}
	}

	/*
	 * Getters to customize the configuration of infrastructure beans
	 */

	protected MongoOperations getMongoOperations() {
		String errorMessage = " To use the default configuration, a MongoOperations bean named 'mongoTemplate'"
				+ " should be defined in the application context but none was found. Override getMongoOperations()"
				+ " to provide the MongoOperations for Batch meta-data.";
		if (this.applicationContext.getBeansOfType(MongoOperations.class).isEmpty()) {
			throw new BatchConfigurationException(
					"Unable to find a MongoOperations bean in the application context." + errorMessage);
		}
		else {
			if (!this.applicationContext.containsBean("mongoTemplate")) {
				throw new BatchConfigurationException(errorMessage);
			}
		}
		return this.applicationContext.getBean("mongoTemplate", MongoOperations.class);
	}

	@Override
	protected MongoTransactionManager getTransactionManager() {
		String errorMessage = " To use the default configuration, a MongoTransactionManager bean named 'transactionManager'"
				+ " should be defined in the application context but none was found. Override getTransactionManager()"
				+ " to provide the transaction manager to use for the job repository.";
		if (this.applicationContext.getBeansOfType(MongoTransactionManager.class).isEmpty()) {
			throw new BatchConfigurationException(
					"Unable to find a MongoTransactionManager bean in the application context." + errorMessage);
		}
		else {
			if (!this.applicationContext.containsBean("transactionManager")) {
				throw new BatchConfigurationException(errorMessage);
			}
		}
		return this.applicationContext.getBean("transactionManager", MongoTransactionManager.class);
	}

	/**
	 * Return the value of the {@code validateTransactionState} parameter. Defaults to
	 * {@code true}.
	 * @return true if the transaction state should be validated, false otherwise
	 */
	protected boolean getValidateTransactionState() {
		return true;
	}

	/**
	 * Return the transaction isolation level when creating job executions. Defaults to
	 * {@link Isolation#SERIALIZABLE}.
	 * @return the transaction isolation level when creating job executions
	 */
	protected Isolation getIsolationLevelForCreate() {
		return Isolation.SERIALIZABLE;
	}

	/**
	 * A custom implementation of the {@link JobKeyGenerator}. The default, if not
	 * injected, is the {@link DefaultJobKeyGenerator}.
	 * @return the generator that creates the key used in identifying {@link JobInstance}
	 * objects
	 * @since 5.1
	 */
	protected JobKeyGenerator getJobKeyGenerator() {
		return new DefaultJobKeyGenerator();
	}

	/**
	 * Return the incrementer to be used to generate ids for new job instances.
	 * @return the incrementer to be used to generate ids for new job instances
	 * @since 6.0
	 */
	protected DataFieldMaxValueIncrementer getJobInstanceIncrementer() {
		return new MongoSequenceIncrementer(getMongoOperations(), "BATCH_JOB_INSTANCE_SEQ");
	}

	/**
	 * Return the incrementer to be used to generate ids for new job executions.
	 * @return the incrementer to be used to generate ids for new job executions
	 * @since 6.0
	 */
	protected DataFieldMaxValueIncrementer getJobExecutionIncrementer() {
		return new MongoSequenceIncrementer(getMongoOperations(), "BATCH_JOB_EXECUTION_SEQ");
	}

	/**
	 * Return the incrementer to be used to generate ids for new step executions.
	 * @return the incrementer to be used to generate ids for new step executions
	 * @since 6.0
	 */
	protected DataFieldMaxValueIncrementer getStepExecutionIncrementer() {
		return new MongoSequenceIncrementer(getMongoOperations(), "BATCH_STEP_EXECUTION_SEQ");
	}

}
