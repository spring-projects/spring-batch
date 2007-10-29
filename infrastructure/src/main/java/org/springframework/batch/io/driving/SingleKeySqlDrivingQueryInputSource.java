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
package org.springframework.batch.io.driving;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.batch.io.support.AbstractDrivingQueryInputSource;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>Sql implementation of the DrivingQueryInputSource that only works for a single key.  A sql
 * query must be passed in which will be used to return a list of keys that will be iterated
 * over according to the {@link InputSource} interface.  Each key will be mapped by a
 * {@link SingleColumnRowMapper} that guarantees a single column will be returned as the key.</p>
 *
 * <p>Restartability: Because the key is only one column, restart is made much more simple.  Before
 * each commit, the last processed key is returned to be stored as restart data.  Upon restart, that
 * same key is given back to restore from, using a separate 'RestartQuery'.  This means that only the
 * keys remaining to be processed are returned, rather than returning the original list of keys and
 * iterating forward to that last committed point.
 * </p>
 *
 * @author Lucas Ward
 * @see AbstractDrivingQueryInputSource
 */
public class SingleKeySqlDrivingQueryInputSource extends AbstractDrivingQueryInputSource {

	public static final String RESTART_KEY = "SingleKeySqlDrivingQueryInputSource.key";

	private JdbcTemplate jdbcTemplate;

	private String drivingQuery;

	private String restartQuery;

	private SingleColumnRowMapper keyMapper = new SingleColumnRowMapper();

	public SingleKeySqlDrivingQueryInputSource() {
		super();
	}

	public SingleKeySqlDrivingQueryInputSource(JdbcTemplate jdbcTemplate, String drivingQuery) {
		this();
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null.");
		Assert.hasLength(drivingQuery, "The DrivingQuery must not be empty.");
		this.jdbcTemplate = jdbcTemplate;
		this.drivingQuery = drivingQuery;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.io.sql.scratch.AbstractDrivingQueryInputSource#retrieveKeys()
	 */
	protected List retrieveKeys() {
		return jdbcTemplate.query(drivingQuery, keyMapper);
	}

	/**
	 * Set the {@link SingleColumnRowMapper} to be used to map each key to an object.
	 *
	 * @param keyMapper
	 */
	public void setKeyMapper(SingleColumnRowMapper keyMapper) {
		this.keyMapper = keyMapper;
	}

	/**
	 * Set the SQL query to be used to return the remaining keys to be processed.
	 *
	 * @param restartQuery
	 */
	public void setRestartQuery(String restartQuery) {
		this.restartQuery = restartQuery;
	}

	/**
	 * Get the restart data representing the last processed key.
	 *
	 * @see org.springframework.batch.restart.Restartable#getRestartData()
	 * @throws IllegalStateException if the RestartQuery is null or empty.
	 */
	public RestartData getRestartData() {
		Properties props = new Properties();
		props.setProperty(RESTART_KEY, getCurrentKey().toString());
		return new GenericRestartData(props);
	}

	/**
	 * Restore input source to previous state.  The RestartData attempting to be restored from
	 * must have been obtained from the <strong>same input source as the one
	 * being restored from</strong> otherwise it is invalid.
	 *
	 * @param RestartData obtained by calling getRestartData during a previous
	 * run.
	 * @throws IllegalStateException if input source has already been read from.
	 * @see org.springframework.batch.io.support.AbstractDrivingQueryInputSource#restoreKeys(org.springframework.batch.restart.RestartData)
	 */
	protected List restoreKeys(RestartData restartData) {

		Assert.state(StringUtils.hasText(restartQuery), "The RestartQuery must not be null or empty" +
		" in order to restart.");

		String lastProcessedKey = restartData.getProperties().getProperty(RESTART_KEY);

		if (lastProcessedKey != null) {
			return jdbcTemplate.query(restartQuery, new Object[] { lastProcessedKey }, keyMapper);
		}

		return new ArrayList();
	}


	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null.");
		Assert.hasText(drivingQuery, "The DrivingQuery must not be null or empty.");
	}
}
