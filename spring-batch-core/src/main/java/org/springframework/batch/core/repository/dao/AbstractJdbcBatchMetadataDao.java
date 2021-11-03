/*
 * Copyright 2006-2013 the original author or authors.
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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulates common functionality needed by JDBC batch metadata DAOs -
 * provides jdbcTemplate for subclasses and handles table prefixes.
 *
 * @author Robert Kasanicky
 */
public abstract class AbstractJdbcBatchMetadataDao implements InitializingBean {

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "BATCH_";

	/**
	 * The default exit message length for the DAO.
	 */
	public static final int DEFAULT_EXIT_MESSAGE_LENGTH = 2500;

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private int clobTypeToUse = Types.CLOB;

	private JdbcOperations jdbcTemplate;

	/**
	 * @return {@link String} containing the current query.
	 */
	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/**
	 * @return {@link String} containing the current table prefix.
	 */
	protected String getTablePrefix() {
		return tablePrefix;
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@link #DEFAULT_TABLE_PREFIX}.
	 *
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Establish the {@link JdbcOperations} used by the dAo.
	 * @param jdbcTemplate the {@link JdbcOperations} to be used.
	 */
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @return {@link JdbcOperations} used by the DAO.
	 */
	protected JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	/**
	 * Retrieve the type of clob as defined {@link Types}
	 * @return clob type.
	 */
	public int getClobTypeToUse() {
		return clobTypeToUse;
	}


	/**
	 * Set the type of clob to use based on the constants in {@link Types}.
	 * @param clobTypeToUse the type of CLOB to use.  Defaults to {@link Types#CLOB}.
	 */
	public void setClobTypeToUse(int clobTypeToUse) {
		this.clobTypeToUse = clobTypeToUse;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "JdbcOperations is required");
	}

}
