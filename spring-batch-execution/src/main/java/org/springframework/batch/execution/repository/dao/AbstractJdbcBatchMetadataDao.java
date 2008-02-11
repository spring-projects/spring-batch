package org.springframework.batch.execution.repository.dao;

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

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private JdbcOperations jdbcTemplate;

	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@value #DEFAULT_TABLE_PREFIX}.
	 * 
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	protected String getTablePrefix() {
		return tablePrefix;
	}

	protected JdbcOperations getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate);
	}

}
