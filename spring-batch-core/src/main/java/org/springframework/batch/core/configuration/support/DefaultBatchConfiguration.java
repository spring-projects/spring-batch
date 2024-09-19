/*
 * Copyright 2012-2024 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import javax.sql.DataSource;
import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DateToStringConverter;
import org.springframework.batch.core.converter.LocalDateTimeToStringConverter;
import org.springframework.batch.core.converter.LocalDateToStringConverter;
import org.springframework.batch.core.converter.LocalTimeToStringConverter;
import org.springframework.batch.core.converter.StringToDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateTimeConverter;
import org.springframework.batch.core.converter.StringToLocalTimeConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;

/**
 * Base {@link Configuration} class that provides common JDBC-based infrastructure beans
 * for enabling and using Spring Batch.
 * <p>
 * This configuration class configures and registers the following beans in the
 * application context:
 *
 * <ul>
 * <li>a {@link JobRepository} named "jobRepository"</li>
 * <li>a {@link JobExplorer} named "jobExplorer"</li>
 * <li>a {@link JobLauncher} named "jobLauncher"</li>
 * <li>a {@link JobRegistry} named "jobRegistry"</li>
 * <li>a {@link JobOperator} named "JobOperator"</li>
 * <li>a {@link JobRegistryBeanPostProcessor} named "jobRegistryBeanPostProcessor"</li>
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
@Import(ScopeConfiguration.class)
public class DefaultBatchConfiguration implements ApplicationContextAware {

	protected ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	public JobRepository jobRepository() throws BatchConfigurationException {
		JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
		try {
			jobRepositoryFactoryBean.setDataSource(getDataSource());
			jobRepositoryFactoryBean.setTransactionManager(getTransactionManager());
			jobRepositoryFactoryBean.setDatabaseType(getDatabaseType());
			jobRepositoryFactoryBean.setIncrementerFactory(getIncrementerFactory());
			jobRepositoryFactoryBean.setJobKeyGenerator(getJobKeyGenerator());
			jobRepositoryFactoryBean.setClobType(getClobType());
			jobRepositoryFactoryBean.setTablePrefix(getTablePrefix());
			jobRepositoryFactoryBean.setSerializer(getExecutionContextSerializer());
			jobRepositoryFactoryBean.setConversionService(getConversionService());
			jobRepositoryFactoryBean.setJdbcOperations(getJdbcOperations());
			jobRepositoryFactoryBean.setCharset(getCharset());
			jobRepositoryFactoryBean.setMaxVarCharLength(getMaxVarCharLength());
			jobRepositoryFactoryBean.setIsolationLevelForCreateEnum(getIsolationLevelForCreate());
			jobRepositoryFactoryBean.setValidateTransactionState(getValidateTransactionState());
			jobRepositoryFactoryBean.afterPropertiesSet();
			return jobRepositoryFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job repository", e);
		}
	}

	/**
	 * Define a job launcher.
	 * @return a job launcher
	 * @throws BatchConfigurationException if unable to configure the default job launcher
	 * @deprecated Since 5.2. Use {@link #jobLauncher(JobRepository)} instead
	 */
	@Deprecated(forRemoval = true)
	public JobLauncher jobLauncher() throws BatchConfigurationException {
		return jobLauncher(jobRepository());
	}

	/**
	 * Define a job launcher bean.
	 * @param jobRepository the job repository
	 * @return a job launcher
	 * @throws BatchConfigurationException if unable to configure the default job launcher
	 * @since 5.2
	 */
	@Bean
	public JobLauncher jobLauncher(JobRepository jobRepository) throws BatchConfigurationException {
		TaskExecutorJobLauncher taskExecutorJobLauncher = new TaskExecutorJobLauncher();
		taskExecutorJobLauncher.setJobRepository(jobRepository);
		taskExecutorJobLauncher.setTaskExecutor(getTaskExecutor());
		try {
			taskExecutorJobLauncher.afterPropertiesSet();
			return taskExecutorJobLauncher;
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job launcher", e);
		}
	}

	@Bean
	public JobExplorer jobExplorer() throws BatchConfigurationException {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(getDataSource());
		jobExplorerFactoryBean.setTransactionManager(getTransactionManager());
		jobExplorerFactoryBean.setJdbcOperations(getJdbcOperations());
		jobExplorerFactoryBean.setJobKeyGenerator(getJobKeyGenerator());
		jobExplorerFactoryBean.setCharset(getCharset());
		jobExplorerFactoryBean.setTablePrefix(getTablePrefix());
		jobExplorerFactoryBean.setConversionService(getConversionService());
		jobExplorerFactoryBean.setSerializer(getExecutionContextSerializer());
		try {
			jobExplorerFactoryBean.afterPropertiesSet();
			return jobExplorerFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job explorer", e);
		}
	}

	@Bean
	public JobRegistry jobRegistry() throws BatchConfigurationException {
		return new MapJobRegistry();
	}

	/**
	 * Define a job operator.
	 * @return a job operator
	 * @throws BatchConfigurationException if unable to configure the default job operator
	 * @deprecated Since 5.2. Use
	 * {@link #jobOperator(JobRepository, JobExplorer, JobRegistry, JobLauncher)} instead
	 */
	@Deprecated(forRemoval = true)
	public JobOperator jobOperator() throws BatchConfigurationException {
		return jobOperator(jobRepository(), jobExplorer(), jobRegistry(), jobLauncher());
	}

	/**
	 * Define a job operator bean.
	 * @param jobRepository a job repository
	 * @param jobExplorer a job explorer
	 * @param jobRegistry a job registry
	 * @param jobLauncher a job launcher
	 * @return a job operator
	 * @throws BatchConfigurationException if unable to configure the default job operator
	 * @since 5.2
	 */
	@Bean
	public JobOperator jobOperator(JobRepository jobRepository, JobExplorer jobExplorer, JobRegistry jobRegistry,
			JobLauncher jobLauncher) throws BatchConfigurationException {
		JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
		jobOperatorFactoryBean.setTransactionManager(getTransactionManager());
		jobOperatorFactoryBean.setJobRepository(jobRepository);
		jobOperatorFactoryBean.setJobExplorer(jobExplorer);
		jobOperatorFactoryBean.setJobRegistry(jobRegistry);
		jobOperatorFactoryBean.setJobLauncher(jobLauncher);
		try {
			jobOperatorFactoryBean.afterPropertiesSet();
			return jobOperatorFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job operator", e);
		}
	}

	/**
	 * Defines a {@link JobRegistryBeanPostProcessor}.
	 * @return a {@link JobRegistryBeanPostProcessor}
	 * @throws BatchConfigurationException if unable to register the bean
	 * @since 5.1
	 * @deprecated Use {@link #jobRegistrySmartInitializingSingleton(JobRegistry)} instead
	 */
	@Deprecated(forRemoval = true)
	public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() throws BatchConfigurationException {
		JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
		jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry());
		try {
			jobRegistryBeanPostProcessor.afterPropertiesSet();
			return jobRegistryBeanPostProcessor;
		}
		catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job registry BeanPostProcessor", e);
		}
	}

	/**
	 * Define a {@link JobRegistrySmartInitializingSingleton} bean.
	 * @param jobRegistry the job registry to populate
	 * @throws BatchConfigurationException if unable to register the bean
	 * @return a bean of type {@link JobRegistrySmartInitializingSingleton}
	 * @since 5.2
	 */
	@Bean
	public JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton(JobRegistry jobRegistry)
			throws BatchConfigurationException {
		JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton = new JobRegistrySmartInitializingSingleton();
		jobRegistrySmartInitializingSingleton.setJobRegistry(jobRegistry);
		try {
			jobRegistrySmartInitializingSingleton.afterPropertiesSet();
			return jobRegistrySmartInitializingSingleton;
		}
		catch (Exception e) {
			throw new BatchConfigurationException(
					"Unable to configure the default job registry SmartInitializingSingleton", e);
		}
	}

	/*
	 * Getters to customize the configuration of infrastructure beans
	 */

	/**
	 * Return the data source to use for Batch meta-data. Defaults to the bean of type
	 * {@link DataSource} and named "dataSource" in the application context.
	 * @return The data source to use for Batch meta-data
	 */
	protected DataSource getDataSource() {
		String errorMessage = " To use the default configuration, a data source bean named 'dataSource'"
				+ " should be defined in the application context but none was found. Override getDataSource()"
				+ " to provide the data source to use for Batch meta-data.";
		if (this.applicationContext.getBeansOfType(DataSource.class).isEmpty()) {
			throw new BatchConfigurationException(
					"Unable to find a DataSource bean in the application context." + errorMessage);
		}
		else {
			if (!this.applicationContext.containsBean("dataSource")) {
				throw new BatchConfigurationException(errorMessage);
			}
		}
		return this.applicationContext.getBean("dataSource", DataSource.class);
	}

	/**
	 * Return the transaction manager to use for the job repository. Defaults to the bean
	 * of type {@link PlatformTransactionManager} and named "transactionManager" in the
	 * application context.
	 * @return The transaction manager to use for the job repository
	 */
	protected PlatformTransactionManager getTransactionManager() {
		String errorMessage = " To use the default configuration, a transaction manager bean named 'transactionManager'"
				+ " should be defined in the application context but none was found. Override getTransactionManager()"
				+ " to provide the transaction manager to use for the job repository.";
		if (this.applicationContext.getBeansOfType(PlatformTransactionManager.class).isEmpty()) {
			throw new BatchConfigurationException(
					"Unable to find a PlatformTransactionManager bean in the application context." + errorMessage);
		}
		else {
			if (!this.applicationContext.containsBean("transactionManager")) {
				throw new BatchConfigurationException(errorMessage);
			}
		}
		return this.applicationContext.getBean("transactionManager", PlatformTransactionManager.class);
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
	 * Return the length of long string columns in database. Do not override this if you
	 * haven't modified the schema. Note this value will be used for the exit message in
	 * both {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao} and also the
	 * short version of the execution context in {@link JdbcExecutionContextDao} . For
	 * databases with multi-byte character sets this number can be smaller (by up to a
	 * factor of 2 for 2-byte characters) than the declaration of the column length in the
	 * DDL for the tables. Defaults to
	 * {@link AbstractJdbcBatchMetadataDao#DEFAULT_EXIT_MESSAGE_LENGTH}
	 */
	protected int getMaxVarCharLength() {
		return AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;
	}

	/**
	 * Return the prefix of Batch meta-data tables. Defaults to
	 * {@link AbstractJdbcBatchMetadataDao#DEFAULT_TABLE_PREFIX}.
	 * @return the prefix of meta-data tables
	 */
	protected String getTablePrefix() {
		return AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;
	}

	/**
	 * Return the {@link Charset} to use when serializing/deserializing the execution
	 * context. Defaults to "UTF-8".
	 * @return the charset to use when serializing/deserializing the execution context
	 */
	protected Charset getCharset() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * Return the {@link JdbcOperations}. If this property is not overridden, a new
	 * {@link JdbcTemplate} will be created for the configured data source by default.
	 * @return the {@link JdbcOperations} to use
	 */
	protected JdbcOperations getJdbcOperations() {
		return new JdbcTemplate(getDataSource());
	}

	/**
	 * A custom implementation of the {@link ExecutionContextSerializer}. The default, if
	 * not injected, is the {@link DefaultExecutionContextSerializer}.
	 * @return the serializer to use to serialize/deserialize the execution context
	 */
	protected ExecutionContextSerializer getExecutionContextSerializer() {
		return new DefaultExecutionContextSerializer();
	}

	/**
	 * Return the value from {@link java.sql.Types} class to indicate the type to use for
	 * a CLOB
	 * @return the value from {@link java.sql.Types} class to indicate the type to use for
	 * a CLOB
	 */
	protected int getClobType() {
		return Types.CLOB;
	}

	/**
	 * Return the factory for creating {@link DataFieldMaxValueIncrementer}
	 * implementations used to increment entity IDs in meta-data tables.
	 * @return the factory for creating {@link DataFieldMaxValueIncrementer}
	 * implementations.
	 */
	protected DataFieldMaxValueIncrementerFactory getIncrementerFactory() {
		return new DefaultDataFieldMaxValueIncrementerFactory(getDataSource());
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
	 * Return the database type. The default will be introspected from the JDBC meta-data
	 * of the data source.
	 * @return the database type
	 * @throws MetaDataAccessException if an error occurs when trying to get the database
	 * type of JDBC meta-data
	 *
	 */
	protected String getDatabaseType() throws MetaDataAccessException {
		return DatabaseType.fromMetaData(getDataSource()).name();
	}

	/**
	 * Return the {@link TaskExecutor} to use in the the job launcher. Defaults to
	 * {@link SyncTaskExecutor}.
	 * @return the {@link TaskExecutor} to use in the the job launcher.
	 */
	protected TaskExecutor getTaskExecutor() {
		return new SyncTaskExecutor();
	}

	/**
	 * Return the conversion service to use in the job repository and job explorer. This
	 * service is used to convert job parameters from String literal to typed values and
	 * vice versa.
	 * @return the {@link ConfigurableConversionService} to use.
	 */
	protected ConfigurableConversionService getConversionService() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new DateToStringConverter());
		conversionService.addConverter(new StringToDateConverter());
		conversionService.addConverter(new LocalDateToStringConverter());
		conversionService.addConverter(new StringToLocalDateConverter());
		conversionService.addConverter(new LocalTimeToStringConverter());
		conversionService.addConverter(new StringToLocalTimeConverter());
		conversionService.addConverter(new LocalDateTimeToStringConverter());
		conversionService.addConverter(new StringToLocalDateTimeConverter());
		return conversionService;
	}

}
