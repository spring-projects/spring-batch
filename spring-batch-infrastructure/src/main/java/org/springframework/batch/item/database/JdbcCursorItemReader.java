/*
 * Copyright 2006-2020 the original author or authors.
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

package org.springframework.batch.item.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * Simple item reader implementation that opens a JDBC cursor and continually retrieves the
 * next row in the ResultSet.
 * </p>
 *
 * <p>
 * The statement used to open the cursor is created with the 'READ_ONLY' option since a non read-only
 * cursor may unnecessarily lock tables or rows. It is also opened with 'TYPE_FORWARD_ONLY' option.
 * By default the cursor will be opened using a separate connection which means that it will not participate
 * in any transactions created as part of the step processing.
 * </p>
 *
 * <p>
 * Each call to {@link #read()} will call the provided RowMapper, passing in the
 * ResultSet.
 * </p>
 *
 * @author Lucas Ward
 * @author Peter Zozom
 * @author Robert Kasanicky
 * @author Thomas Risberg
 * @author Mahmoud Ben Hassine
 */
public class JdbcCursorItemReader<T> extends AbstractCursorItemReader<T> {

	private PreparedStatement preparedStatement;

	private PreparedStatementSetter preparedStatementSetter;

	private String sql;

	private RowMapper<T> rowMapper;

	public JdbcCursorItemReader() {
		super();
		setName(ClassUtils.getShortName(JdbcCursorItemReader.class));
	}

	/**
	 * Set the RowMapper to be used for all calls to read().
	 *
	 * @param rowMapper the mapper used to map each item
	 */
	public void setRowMapper(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * Set the SQL statement to be used when creating the cursor. This statement
	 * should be a complete and valid SQL statement, as it will be run directly
	 * without any modification.
	 *
	 * @param sql SQL statement
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * Set the PreparedStatementSetter to use if any parameter values that need
	 * to be set in the supplied query.
	 *
	 * @param preparedStatementSetter PreparedStatementSetter responsible for filling out the statement
	 */
	public void setPreparedStatementSetter(PreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Assert that mandatory properties are set.
	 *
	 * @throws IllegalArgumentException if either data source or SQL properties
	 * not set.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(sql, "The SQL query must be provided");
		Assert.notNull(rowMapper, "RowMapper must be provided");
	}


	@Override
	protected void openCursor(Connection con) {
		try {
			if (isUseSharedExtendedConnection()) {
				preparedStatement = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.HOLD_CURSORS_OVER_COMMIT);
			}
			else {
				preparedStatement = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			}
			applyStatementSettings(preparedStatement);
			if (this.preparedStatementSetter != null) {
				preparedStatementSetter.setValues(preparedStatement);
			}
			this.rs = preparedStatement.executeQuery();
			handleWarnings(preparedStatement);
		}
		catch (SQLException se) {
			close();
			throw getExceptionTranslator().translate("Executing query", getSql(), se);
		}

	}


	@Nullable
	@Override
	protected T readCursor(ResultSet rs, int currentRow) throws SQLException {
		return rowMapper.mapRow(rs, currentRow);
	}

	/**
	 * Close the cursor and database connection.
	 * @deprecated This method is deprecated in favor of
	 * {@link JdbcCursorItemReader#cleanupOnClose(java.sql.Connection)} and will
	 * be removed in a future release
	 */
	@Override
	@Deprecated
	protected void cleanupOnClose() throws Exception {
		JdbcUtils.closeStatement(this.preparedStatement);
	}

	/**
	 * Close the cursor and database connection.
	 * @param connection to the database
	 */
	@Override
	protected void cleanupOnClose(Connection connection) throws Exception {
		JdbcUtils.closeStatement(this.preparedStatement);
		JdbcUtils.closeConnection(connection);
	}

	@Override
	public String getSql() {
		return this.sql;
	}
}
