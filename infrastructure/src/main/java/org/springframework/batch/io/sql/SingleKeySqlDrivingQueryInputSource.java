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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * <p>
 * DrivingQuery based input source for input data that can be uniquely
 * identified by a single primary key. The current implementation is
 * forward-only, and requires a transactional buffer to ensure rollbacks are
 * handled correctly.
 * </p>
 * 
 * <p>
 * Users of this input source must provide a 'Driving Query' that returns only
 * one column. (If more than one is returned, the first column will be used) A
 * 'Details Query' must then be provided that requires only one parameter.
 * (question mark) Invalid queries will throw SqlExceptions from JdbcTemplate.
 * <p>
 * 
 * 
 * @author Lucas Ward
 * 
 */
public class SingleKeySqlDrivingQueryInputSource implements ResourceLifecycle, InputSource, Restartable {

	private static final String RESTART_KEY = "SingleKeySqlDrivingQueryInputSource.lastProcessedKey";

	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private String drivingQuery;

	private String detailsQuery;

	private String restartQuery;

	private Object[] detailArgs = new Object[1];

	private List keys;

	private Iterator keysIterator;

	private RowMapper mapper;

	/**
	 * Read one record by passing in the current key to the details query.
	 * 
	 */
	public Object read() {
		if (keys == null) {
			retrieveKeys();
		}

		if (keysIterator.hasNext()) {
			detailArgs[0] = keysIterator.next();

			return jdbcTemplate.queryForObject(detailsQuery, detailArgs, mapper);
		}

		return null;
	}

	/*
	 * Retrieve the keys by calling the DrivingQuery.
	 */
	private void retrieveKeys() {

		jdbcTemplate = new JdbcTemplate(dataSource);

		keys = jdbcTemplate.query(drivingQuery, new SingleColumnRowMapper());

		keysIterator = keys.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {
		keys = null;
		keysIterator = null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public void open() {

	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the query to be used to obtain the list of keys at
	 * initialization.  Each key returned will be fed into the
	 * details query.
	 * 
	 * @param drivingQuery
	 */
	public void setDrivingQuery(String drivingQuery) {
		this.drivingQuery = drivingQuery;
	}

	/**
	 * Set the query to be used for each 'detail' record.  Meaning,
	 * the query each key (row returned from the driving query) will
	 * be fed into in order to return a row to be mapped. 
	 * 
	 * @param detailsQuery
	 */
	public void setDetailsQuery(String detailsQuery) {
		this.detailsQuery = detailsQuery;
	}

	/**
	 * Set the query to be used in the case of a restart.  The current
	 * key at the time restart data is requested will be fed into this
	 * query as a parameter upon restart, allowing for only the remaining
	 * keys to be returned.
	 * 
	 * @param restartQuery
	 */
	public void setRestartQuery(String restartQuery) {
		this.restartQuery = restartQuery;
	}

	/**
	 * Set RowMapper to be used for each call to the provided details
	 * query.
	 * 
	 * @param mapper
	 */
	public void setMapper(RowMapper mapper) {
		this.mapper = mapper;
	}

	// Required because JdbcTemplate.queryForList returns a list
	// of maps based on metadata.
	private class SingleColumnRowMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getObject(1);
		}

	}

	public RestartData getRestartData() {
		Properties props = new Properties();
		props.setProperty(RESTART_KEY, detailArgs[0].toString());
		return new GenericRestartData(props);
	}

	/**
	 * Restore input source to previous state. If the input source has already
	 * been initialized before calling restore (meaning, read has been called)
	 * then an IllegalStateException will be thrown, since all input sources
	 * should be restored before being read from, otherwise already processed
	 * data could be returned. The RestartData attempting to be restored from
	 * must have been obtained from the <strong>same input source as the one
	 * being restored from</strong> otherwise it is invalid.
	 * 
	 * @param RestartData obtained by calling getRestartData during a previous
	 * run.
	 * @throws IllegalStateException if input source has already been read from.
	 */
	public void restoreFrom(RestartData data) {

		Assert.notNull(data, "RestartData must not be null.");
		
		if (keys != null) {
			throw new IllegalStateException("Cannot restore when already intialized.  Call"
					+ " close() first before restore()");
		}

		Properties restartData = data.getProperties();
		String lastProcessedKey = restartData.getProperty(RESTART_KEY);
		if (lastProcessedKey != null) {
			jdbcTemplate = new JdbcTemplate(dataSource);

			keys = jdbcTemplate.query(restartQuery, new Object[] { lastProcessedKey }, new SingleColumnRowMapper());

			keysIterator = keys.iterator();
		}
	}
}
