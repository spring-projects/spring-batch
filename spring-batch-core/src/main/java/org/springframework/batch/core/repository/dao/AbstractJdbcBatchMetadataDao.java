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

package org.springframework.batch.core.repository.dao;

import java.sql.Types;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.converter.DateToStringConverter;
import org.springframework.batch.core.converter.LocalDateTimeToStringConverter;
import org.springframework.batch.core.converter.LocalDateToStringConverter;
import org.springframework.batch.core.converter.LocalTimeToStringConverter;
import org.springframework.batch.core.converter.StringToDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateConverter;
import org.springframework.batch.core.converter.StringToLocalDateTimeConverter;
import org.springframework.batch.core.converter.StringToLocalTimeConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulates common functionality needed by JDBC batch metadata DAOs - provides
 * jdbcTemplate for subclasses and handles table prefixes.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public abstract class AbstractJdbcBatchMetadataDao implements InitializingBean {

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "BATCH_";

	public static final int DEFAULT_EXIT_MESSAGE_LENGTH = 2500;

	public static final int DEFAULT_SHORT_CONTEXT_LENGTH = 2500;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private int clobTypeToUse = Types.CLOB;

	private @Nullable JdbcOperations jdbcTemplate;

	private @Nullable ConfigurableConversionService conversionService;

	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	protected String getTablePrefix() {
		return tablePrefix;
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all the table
	 * names before queries are executed. Defaults to {@link #DEFAULT_TABLE_PREFIX}.
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Nullable protected JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public int getClobTypeToUse() {
		return clobTypeToUse;
	}

	public void setClobTypeToUse(int clobTypeToUse) {
		this.clobTypeToUse = clobTypeToUse;
	}

	/**
	 * Set the conversion service to use to convert job parameters from String literals to
	 * typed values and vice versa.
	 */
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "conversionService must not be null");
		this.conversionService = conversionService;
	}

	@Nullable public ConfigurableConversionService getConversionService() {
		return conversionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "JdbcOperations is required");
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
	}

}
