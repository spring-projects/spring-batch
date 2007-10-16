/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.io.sql;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.io.support.AbstractDrivingQueryInputSource;
import org.springframework.batch.restart.RestartData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>Sql implementation of the DrivingQueryInputSource that works for composite keys.
 * (i.e. keys represented by multiple columns)  A sql query to be used to return the keys and
 * a RowMapper to map each row in the resultset to an Object must be set in order for the
 * InputSource to work correctly.
 * </p>
 *
 * @author Lucas Ward
 * @see AbstractDrivingQueryInputSource
 */
public class CompositeKeySqlDrivingQueryInputSource extends
		AbstractDrivingQueryInputSource {

	public static final String RESTART_KEY = "CompositeKeySqlDrivingQueryInputSource.key";

	private JdbcTemplate jdbcTemplate;

	private RowMapper keyMapper;

	private String drivingQuery;

	private String restartQuery;

	private CompositeKeyRestartDataConverter restartDataConverter;

	public CompositeKeySqlDrivingQueryInputSource() {
		super();
	}

	/**
	 * Construct a new InputSource.
	 *
	 * @param jdbcTemplate
	 * @param drivingQuery - Sql statement that returns all keys to process.
	 * @param keyMapper - RowMapper that maps each row of the ResultSet to an object.
	 */
	public CompositeKeySqlDrivingQueryInputSource(JdbcTemplate jdbcTemplate,
			String drivingQuery, RowMapper keyMapper){
		this();
		Assert.notNull(jdbcTemplate, "The JdbcTemplate must not be null.");
		Assert.hasText(drivingQuery, "The DrivingQuery must not be null or empty.");
		Assert.notNull(keyMapper, "The key RowMapper must not be null.");
		this.jdbcTemplate = jdbcTemplate;
		this.drivingQuery = drivingQuery;
		this.keyMapper = keyMapper;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.io.sql.scratch.AbstractDrivingQueryInputSource#retrieveKeys()
	 */
	protected List retrieveKeys() {
		return jdbcTemplate.query(drivingQuery, keyMapper);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.io.sql.scratch.AbstractDrivingQueryInputSource#restoreKeys(org.springframework.batch.restart.RestartData)
	 */
	protected List restoreKeys(RestartData restartData) {

		Assert.state(restartDataConverter != null, "RestartDataConverter must not be null.");
		Assert.state(StringUtils.hasText(restartQuery), "The RestartQuery must not be null or empty" +
		" in order to restart.");

		if (restartData.getProperties() != null) {
			return jdbcTemplate.query(restartQuery, restartDataConverter.createArguments(restartData), keyMapper);
		}

		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.restart.Restartable#getRestartData()
	 */
	public RestartData getRestartData() {
		Assert.state(restartDataConverter != null, "RestartDataConverter must not be null.");
		return restartDataConverter.createRestartData(getCurrentKey());
	}

	/**
	 * Set the {@link RestartDataConverter} used to convert a composite key to
	 * RestartData and back again.
	 *
	 * @param restartDataConverter
	 */
	public void setRestartDataConverter(
			CompositeKeyRestartDataConverter restartDataConverter) {
		this.restartDataConverter = restartDataConverter;
	}

	/**
	 * Set the query to use to retrieve keys in order to restore the previous
	 * state for restart.
	 *
	 * @param restartQuery
	 */
	public void setRestartQuery(String restartQuery) {
		this.restartQuery = restartQuery;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "The JdbcTemplate must not be null.");
		Assert.hasText(drivingQuery, "The DrivingQuery must not be null or empty.");
		Assert.notNull(keyMapper, "The key RowMapper must not be null.");
	}
}
