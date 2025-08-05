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
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DateToStringConverter;
import org.springframework.batch.core.converter.LocalDateTimeToStringConverter;
import org.springframework.batch.core.converter.LocalDateToStringConverter;
import org.springframework.batch.core.converter.LocalTimeToStringConverter;
import org.springframework.batch.core.converter.StringToDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateTimeConverter;
import org.springframework.batch.core.converter.StringToLocalTimeConverter;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.jdbc.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Types;

/**
 * Base {@link Configuration} class that provides common JDBC-based infrastructure beans
 * for enabling and using Spring Batch.
 * <p>
 * This configuration class configures and registers the following beans in the
 * application context:
 *
 * <ul>
 * <li>a {@link JobRepository} named "jobRepository"</li>
 * <li>a {@link JobRegistry} named "jobRegistry"</li>
 * <li>a {@link JobOperator} named "jobOperator"</li>
 * <li>a {@link org.springframework.batch.core.scope.StepScope} named "stepScope"</li>
 * <li>a {@link org.springframework.batch.core.scope.JobScope} named "jobScope"</li>
 * </ul>
 *
 * Customization is possible by extending the class and overriding getters.
 * <p>
 * A typical usage of this class is as follows: <pre class="code">
 * &#064;Configuration
 * public class MyJobConfiguration extends JdbcDefaultBatchConfiguration {
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
public class JdbcDefaultBatchConfiguration extends DefaultBatchConfiguration {

	@Bean
	@Override
	public JobRepository jobRepository() throws BatchConfigurationException {
		JdbcJobRepositoryFactoryBean jobRepositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
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

	@Override
	protected PlatformTransactionManager getTransactionManager() {
		String errorMessage = " To use the default configuration, a PlatformTransactionManager bean named 'transactionManager'"
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
	 * Return the value from {@link Types} class to indicate the type to use for a CLOB
	 * @return the value from {@link Types} class to indicate the type to use for a CLOB
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
