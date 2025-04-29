/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.batch.core.explore.support;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.converter.DateToStringConverter;
import org.springframework.batch.core.converter.LocalDateTimeToStringConverter;
import org.springframework.batch.core.converter.LocalDateToStringConverter;
import org.springframework.batch.core.converter.LocalTimeToStringConverter;
import org.springframework.batch.core.converter.StringToDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateTimeConverter;
import org.springframework.batch.core.converter.StringToLocalTimeConverter;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobExplorer} by
 * using JDBC DAO implementations. Requires the user to describe what kind of database
 * they use.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class JobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private DataSource dataSource;

	private JdbcOperations jdbcOperations;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	private final DataFieldMaxValueIncrementer incrementer = new AbstractDataFieldMaxValueIncrementer() {
		@Override
		protected long getNextKey() {
			throw new IllegalStateException("JobExplorer is read only.");
		}
	};

	private JobKeyGenerator jobKeyGenerator;

	private ExecutionContextSerializer serializer;

	private Charset charset = StandardCharsets.UTF_8;

	private ConfigurableConversionService conversionService;

	/**
	 * A custom implementation of {@link ExecutionContextSerializer}. The default, if not
	 * injected, is the {@link DefaultExecutionContextSerializer}.
	 * @param serializer The serializer used to serialize or deserialize an
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 * @see ExecutionContextSerializer
	 */
	public void setSerializer(ExecutionContextSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Sets the data source.
	 * <p>
	 * Public setter for the {@link DataSource}.
	 * @param dataSource A {@code DataSource}.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Public setter for the {@link JdbcOperations}. If this property is not explicitly
	 * set, a new {@link JdbcTemplate} is created, by default, for the configured
	 * {@link DataSource}.
	 * @param jdbcOperations a {@link JdbcOperations}
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	/**
	 * Sets the table prefix for all the batch metadata tables.
	 * @param tablePrefix The table prefix for the batch metadata tables.
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
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

	/**
	 * Sets the {@link Charset} to use when deserializing the execution context. Defaults
	 * to "UTF-8". Must not be {@code null}.
	 * @param charset The character set to use when deserializing the execution context.
	 * @see JdbcExecutionContextDao#setCharset(Charset)
	 * @since 5.0
	 */
	public void setCharset(@NonNull Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	/**
	 * Set the conversion service to use in the job explorer. This service is used to
	 * convert job parameters from String literal to typed values and vice versa.
	 * @param conversionService the conversion service to use
	 * @since 5.0
	 */
	public void setConversionService(@NonNull ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.state(dataSource != null, "DataSource must not be null.");

		if (jdbcOperations == null) {
			jdbcOperations = new JdbcTemplate(dataSource);
		}

		if (jobKeyGenerator == null) {
			jobKeyGenerator = new DefaultJobKeyGenerator();
		}

		if (serializer == null) {
			serializer = new DefaultExecutionContextSerializer();
		}

		if (this.conversionService == null) {
			DefaultConversionService conversionService = new DefaultConversionService();
			conversionService.addConverter(new DateToStringConverter());
			conversionService.addConverter(new StringToDateConverter());
			conversionService.addConverter(new LocalDateToStringConverter());
			conversionService.addConverter(new StringToLocalDateConverter());
			conversionService.addConverter(new LocalTimeToStringConverter());
			conversionService.addConverter(new StringToLocalTimeConverter());
			conversionService.addConverter(new LocalDateTimeToStringConverter());
			conversionService.addConverter(new StringToLocalDateTimeConverter());
			this.conversionService = conversionService;
		}

		super.afterPropertiesSet();
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setTablePrefix(tablePrefix);
		dao.setSerializer(serializer);
		dao.setCharset(charset);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		JdbcJobInstanceDao dao = new JdbcJobInstanceDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobInstanceIncrementer(incrementer);
		dao.setJobKeyGenerator(jobKeyGenerator);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobExecutionIncrementer(incrementer);
		dao.setTablePrefix(tablePrefix);
		dao.setConversionService(this.conversionService);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setStepExecutionIncrementer(incrementer);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

}
