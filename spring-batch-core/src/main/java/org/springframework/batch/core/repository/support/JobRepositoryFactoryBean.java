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

package org.springframework.batch.core.repository.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

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
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.jdbc.JdbcStepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Types;

import static org.springframework.batch.support.DatabaseType.SYBASE;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobRepository}
 * using JDBC DAO implementations which persist batch metadata in database. Requires the
 * user to describe what kind of database they are using.
 *
 * @author Ben Hale
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0 in favor of {@link JdbcJobRepositoryFactoryBean}. Scheduled for
 * removal in 6.2 or later.
 */
@NullUnmarked
@Deprecated(since = "6.0", forRemoval = true)
public class JobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean implements InitializingBean {

	protected static final Log logger = LogFactory.getLog(JobRepositoryFactoryBean.class);

	protected DataSource dataSource;

	protected JdbcOperations jdbcOperations;

	protected String databaseType;

	protected String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	protected DataFieldMaxValueIncrementerFactory incrementerFactory;

	protected int maxVarCharLengthForExitMessage = AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;

	protected int maxVarCharLengthForShortContext = AbstractJdbcBatchMetadataDao.DEFAULT_SHORT_CONTEXT_LENGTH;

	protected ExecutionContextSerializer serializer;

	protected Integer clobType;

	protected Charset charset = StandardCharsets.UTF_8;

	protected ConfigurableConversionService conversionService;

	protected Object getTarget() throws Exception {
		JdbcJobInstanceDao jobInstanceDao = createJobInstanceDao();
		JdbcJobExecutionDao jobExecutionDao = createJobExecutionDao();
		jobExecutionDao.setJobInstanceDao(jobInstanceDao);
		JdbcStepExecutionDao stepExecutionDao = createStepExecutionDao();
		stepExecutionDao.setJobExecutionDao(jobExecutionDao);
		JdbcExecutionContextDao executionContextDao = createExecutionContextDao();
		return new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao, executionContextDao);
	}

	/**
	 * @param type a value from the {@link java.sql.Types} class to indicate the type to
	 * use for a CLOB
	 */
	public void setClobType(int type) {
		this.clobType = type;
	}

	/**
	 * A custom implementation of the {@link ExecutionContextSerializer}. The default, if
	 * not injected, is the {@link DefaultExecutionContextSerializer}.
	 * @param serializer used to serialize/deserialize
	 * {@link org.springframework.batch.item.ExecutionContext}
	 * @see ExecutionContextSerializer
	 */
	public void setSerializer(ExecutionContextSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Public setter for the length of long string columns in database. Do not set this if
	 * you haven't modified the schema. Note this value will be used for the exit message
	 * in both {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao} and also the
	 * short version of the execution context in {@link JdbcExecutionContextDao} . If you
	 * want to use separate values for exit message and short context, then use
	 * {@link #setMaxVarCharLengthForExitMessage(int)} and
	 * {@link #setMaxVarCharLengthForShortContext(int)}. For databases with multi-byte
	 * character sets this number can be smaller (by up to a factor of 2 for 2-byte
	 * characters) than the declaration of the column length in the DDL for the tables.
	 * @param maxVarCharLength the exitMessageLength to set
	 */
	public void setMaxVarCharLength(int maxVarCharLength) {
		this.maxVarCharLengthForExitMessage = maxVarCharLength;
		this.maxVarCharLengthForShortContext = maxVarCharLength;
	}

	/**
	 * Public setter for the length of short context string column in database. Do not set
	 * this if you haven't modified the schema. For databases with multi-byte character
	 * sets this number can be smaller (by up to a factor of 2 for 2-byte characters) than
	 * the declaration of the column length in the DDL for the tables. Defaults to
	 * {@link AbstractJdbcBatchMetadataDao#DEFAULT_SHORT_CONTEXT_LENGTH}
	 * @param maxVarCharLengthForShortContext the short context length to set
	 * @since 5.1
	 */
	public void setMaxVarCharLengthForShortContext(int maxVarCharLengthForShortContext) {
		this.maxVarCharLengthForShortContext = maxVarCharLengthForShortContext;
	}

	/**
	 * Public setter for the length of the exit message in both
	 * {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao}. Do not set this if
	 * you haven't modified the schema. For databases with multi-byte character sets this
	 * number can be smaller (by up to a factor of 2 for 2-byte characters) than the
	 * declaration of the column length in the DDL for the tables. Defaults to
	 * {@link AbstractJdbcBatchMetadataDao#DEFAULT_EXIT_MESSAGE_LENGTH}.
	 * @param maxVarCharLengthForExitMessage the exitMessageLength to set
	 * @since 5.1
	 */
	public void setMaxVarCharLengthForExitMessage(int maxVarCharLengthForExitMessage) {
		this.maxVarCharLengthForExitMessage = maxVarCharLengthForExitMessage;
	}

	/**
	 * Public setter for the {@link DataSource}.
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Public setter for the {@link JdbcOperations}. If this property is not set
	 * explicitly, a new {@link JdbcTemplate} will be created for the configured
	 * DataSource by default.
	 * @param jdbcOperations a {@link JdbcOperations}
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	/**
	 * Sets the database type.
	 * @param dbType as specified by {@link DefaultDataFieldMaxValueIncrementerFactory}
	 */
	public void setDatabaseType(String dbType) {
		this.databaseType = dbType;
	}

	/**
	 * Sets the table prefix for all the batch meta-data tables.
	 * @param tablePrefix prefix prepended to batch meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setIncrementerFactory(DataFieldMaxValueIncrementerFactory incrementerFactory) {
		this.incrementerFactory = incrementerFactory;
	}

	/**
	 * Set the {@link Charset} to use when serializing/deserializing the execution
	 * context. Defaults to "UTF-8". Must not be {@code null}.
	 * @param charset to use when serializing/deserializing the execution context.
	 * @see JdbcExecutionContextDao#setCharset(Charset)
	 * @since 5.0
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	/**
	 * Set the conversion service to use in the job repository. This service is used to
	 * convert job parameters from String literal to typed values and vice versa.
	 * @param conversionService the conversion service to use
	 * @since 5.0
	 */
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.state(dataSource != null, "DataSource must not be null.");

		if (jdbcOperations == null) {
			jdbcOperations = new JdbcTemplate(dataSource);
		}

		if (incrementerFactory == null) {
			incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
		}

		if (databaseType == null) {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
			if (logger.isInfoEnabled()) {
				logger.info("No database type set, using meta data indicating: " + databaseType);
			}
		}

		if (serializer == null) {
			serializer = new DefaultExecutionContextSerializer();
		}

		Assert.state(incrementerFactory.isSupportedIncrementerType(databaseType),
				() -> "'" + databaseType + "' is an unsupported database type.  The supported database types are "
						+ StringUtils.arrayToCommaDelimitedString(incrementerFactory.getSupportedIncrementerTypes()));

		if (clobType != null) {
			Assert.state(isValidTypes(clobType), "lobType must be a value from the java.sql.Types class");
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
	protected JdbcJobInstanceDao createJobInstanceDao() {
		JdbcJobInstanceDao dao = new JdbcJobInstanceDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobInstanceIncrementer(
				incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_INSTANCE_SEQ"));
		dao.setJobKeyGenerator(jobKeyGenerator);
		dao.setTablePrefix(tablePrefix);
		return dao;
	}

	@Override
	protected JdbcJobExecutionDao createJobExecutionDao() {
		JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobExecutionIncrementer(
				incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		dao.setExitMessageLength(this.maxVarCharLengthForExitMessage);
		dao.setConversionService(this.conversionService);
		return dao;
	}

	@Override
	protected JdbcStepExecutionDao createStepExecutionDao() {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setStepExecutionIncrementer(
				incrementerFactory.getIncrementer(databaseType, tablePrefix + "STEP_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		dao.setExitMessageLength(this.maxVarCharLengthForExitMessage);
		return dao;
	}

	@Override
	protected JdbcExecutionContextDao createExecutionContextDao() {
		JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setTablePrefix(tablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		dao.setSerializer(serializer);
		dao.setCharset(charset);
		dao.setShortContextLength(this.maxVarCharLengthForShortContext);
		return dao;
	}

	private int determineClobTypeToUse(String databaseType) {
		if (clobType != null) {
			return clobType;
		}
		else {
			if (SYBASE == DatabaseType.valueOf(databaseType.toUpperCase())) {
				return Types.LONGVARCHAR;
			}
			else {
				return Types.CLOB;
			}
		}
	}

	private boolean isValidTypes(int value) throws Exception {
		boolean result = false;

		for (Field field : Types.class.getFields()) {
			int curValue = field.getInt(null);
			if (curValue == value) {
				result = true;
				break;
			}
		}

		return result;
	}

}
