/*
 * Copyright 2006-2023 the original author or authors.
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
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
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

	private JdbcOperations jdbcTemplate;

	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	protected String getQuery(String base, Map<String, Collection<?>> collectionParams) {
		String query = getQuery(base);
		for (Map.Entry<String, Collection<?>> collectionParam : collectionParams.entrySet()) {
			query = createParameterizedQuery(query, collectionParam.getKey(), collectionParam.getValue());
		}
		return query;
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

	protected JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public int getClobTypeToUse() {
		return clobTypeToUse;
	}

	public void setClobTypeToUse(int clobTypeToUse) {
		this.clobTypeToUse = clobTypeToUse;
	}

	/**
	 * Replaces a given placeholder with a number of parameters (i.e. "?").
	 * @param sqlTemplate given sql template
	 * @param placeholder placeholder that is being used for parameters
	 * @param parameters collection of parameters with variable size
	 * @return sql query replaced with a number of parameters
	 */
	private static String createParameterizedQuery(String sqlTemplate, String placeholder, Collection<?> parameters) {
		String params = parameters.stream().map(p -> "?").collect(Collectors.joining(", "));
		return sqlTemplate.replace(placeholder, params);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "JdbcOperations is required");
	}

}
