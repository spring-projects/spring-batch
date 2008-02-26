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

package org.springframework.batch.io.cursor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.KeyedItemReader;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Simple input source that opens a Sql Cursor and continually retrieves the
 * next row in the ResultSet. It is extremely important to note that the
 * JdbcDriver used must be version 3.0 or higher. This is because earlier
 * versions do not support holding a ResultSet open over commits.
 * </p>
 * 
 * <p>
 * Each call to read() will call the provided RowMapper, (NOTE: Calling read()
 * without setting a RowMapper will result in an IllegalStateException!) passing
 * in the ResultSet. If this is the first call to read(), the provided query
 * will be run in order to open the cursor. There is currently no wrapping of
 * the ResultSet to suppress calls to next(). However, if the RowMapper
 * increments the current row, the next call to read will verify that the
 * current row is at the expected position and throw a DataAccessException if it
 * is not. This means that, in theory, a RowMapper could read ahead, as long as
 * it returns the row back to it's correct position before returning. The reason
 * for such strictness on the ResultSet is due to the need to maintain strict
 * control for Transactions, restartability and skippability. This ensures that
 * each call to read() returns the ResultSet at the correct line, regardless of
 * rollbacks, restarts, or skips.
 * </p>
 * 
 * <p>
 * {@link ExecutionContext}: The current row is returned as restart data,
 * and when restored from that same data, the cursor is opened and the current
 * row set to the value within the restart data. There are also two statistics
 * returned by this input source: the current line being processed and the
 * number of lines that have been skipped.
 * </p>
 * 
 * <p>
 * Transactions: At first glance, it may appear odd that Spring's
 * TransactionSynchronization abstraction is used for something that is reading
 * from the database, however, it is important because the same resultset is
 * held open regardless of commits or roll backs. This means that when a
 * transaction is committed, the input source is notified so that it can save
 * it's current row number. Later, if the transaction is rolled back, the
 * current row can be moved back to the same row number as it was on when commit
 * was called.
 * </p>
 * 
 * <p>
 * Calling skip will indicate to the input source that a record is bad and
 * should not be represented to the user if the transaction is rolled back. For
 * example, if row 2 is read in, and found to be bad, calling skip will inform
 * the Input Source. If reading is then continued, and a rollback is necessary
 * because of an error on output, the input source will be returned to row 1.
 * Calling read while on row 1 will move the current row to 3, not 2, because 2
 * has been marked as skipped.
 * </p>
 * 
 * <p>
 * Calling close on this Input Source will cause all resources it is currently
 * using to be freed. (Connection, resultset, etc). If read() is called on the
 * same instance again, the cursor will simply be reopened starting at row 0.
 * </p>
 * 
 * @author Lucas Ward
 * @author Peter Zozom
 */
public class JdbcCursorItemReader implements KeyedItemReader, InitializingBean,
		ItemStream, Skippable {

	private static Log log = LogFactory.getLog(JdbcCursorItemReader.class);

	public static final int VALUE_NOT_SET = -1;

	private static final String CURRENT_PROCESSED_ROW = "sqlCursorInput.lastProcessedRowNum";

	private static final String SKIPPED_ROWS = "sqlCursorInput.skippedRows";

	private static final String SKIP_COUNT = "sqlCursorInput.skippedRrecordCount";

	private Connection con;

	private Statement stmt;

	protected ResultSet rs;

	private DataSource dataSource;

	private String sql;

	private final List skippedRows = new ArrayList();

	private int skipCount = 0;

	private int fetchSize = VALUE_NOT_SET;

	private int maxRows = VALUE_NOT_SET;

	private int queryTimeout = VALUE_NOT_SET;

	private boolean ignoreWarnings = true;

	private boolean verifyCursorPosition = true;

	private SQLExceptionTranslator exceptionTranslator;

	/* Current count of processed records. */
	private long currentProcessedRow = 0;

	private long lastCommittedRow = 0;

	private RowMapper mapper;

	private boolean initialized = false;
	
	private ExecutionContext executionContext = new ExecutionContext();
	
	private boolean saveState = false;
	
	private String name = JdbcCursorItemReader.class.getName();

	/**
	 * Assert that mandatory properties are set.
	 * 
	 * @throws IllegalArgumentException if either data source or sql properties
	 * not set.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource, "DataSOurce must be provided");
		Assert.notNull(sql, "The SQL query must be provided");
	}

	/**
	 * Public setter for the data source for injection purposes.
	 * 
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Increment the cursor to the next row, validating the cursor position and
	 * passing the resultset to the RowMapper. If read has not been called on
	 * this instance before, the cursor will be opened. If there are skipped
	 * records for this commit scope, an internal list of skipped records will
	 * be checked to ensure that only a valid row is given to the mapper.
	 * 
	 * @returns Object returned by RowMapper
	 * @throws DataAccessException
	 * @throws IllegalStateExceptino if mapper is null.
	 */
	public Object read() {

		if (!initialized) {
			open(new ExecutionContext());
		}

		Assert.state(mapper != null, "Mapper must not be null.");

		try {
			if (!rs.next()) {
				return null;
			}
			else {
				currentProcessedRow++;
				if (!skippedRows.isEmpty()) {
					// while is necessary to handle successive skips.
					while (skippedRows.contains(new Long(currentProcessedRow))) {
						if (!rs.next()) {
							return null;
						}
						currentProcessedRow++;
					}
				}

				Object mappedResult = mapper.mapRow(rs, (int) currentProcessedRow);

				verifyCursorPosition(currentProcessedRow);

				return mappedResult;
			}
		}
		catch (SQLException se) {
			throw getExceptionTranslator().translate("Trying to process next row", sql, se);
		}

	}

	public long getCurrentProcessedRow() {
		return currentProcessedRow;
	}

	/**
	 * Mark the current row. Calling reset will cause the result set to be set
	 * to the current row when mark was called.
	 */
	public void mark() {
		lastCommittedRow = currentProcessedRow;
		skippedRows.clear();
	}

	/**
	 * Set the ResultSet's current row to the last marked position.
	 * 
	 * @throws DataAccessException
	 */
	public void reset() throws ResetFailedException {
		try {
			currentProcessedRow = lastCommittedRow;
			if (currentProcessedRow > 0) {
				rs.absolute((int) currentProcessedRow);
			}
			else {
				rs.beforeFirst();
			}

		}
		catch (SQLException se) {
			throw new ResetFailedException(getExceptionTranslator().translate(
					"Attempted to move ResultSet to last committed row", sql, se));
		}
	}

	/**
	 * Close this input source. The ResultSet, Statement and Connection created
	 * will be closed. This must be called or the connection and cursor will be
	 * held open indefinitely!
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {
		initialized = false;
		JdbcUtils.closeResultSet(this.rs);
		JdbcUtils.closeStatement(this.stmt);
		JdbcUtils.closeConnection(this.con);
		this.currentProcessedRow = 0;
		skippedRows.clear();
		skipCount = 0;
	}

	// Check the result set is in synch with the currentRow attribute. This is
	// important
	// to ensure that the user hasn't modified the current row.
	private void verifyCursorPosition(long expectedCurrentRow) throws SQLException {
		if (verifyCursorPosition) {
			if (expectedCurrentRow != this.rs.getRow()) {
				throw new InvalidDataAccessResourceUsageException("Unexpected cursor position change.");
			}
		}
	}

	/*
	 * Executes the provided SQL query. The statement is created with
	 * 'READ_ONLY' and 'HOLD_CUSORS_OVER_COMMIT' set to true. This is extremely
	 * important, since a non read-only cursor may lock tables that shouldn't be
	 * locked, and not holding the cursor open over a commit would require it to
	 * be reopened after each commit, which would destroy performance.
	 */
	private void executeQuery() {

		Assert.state(dataSource != null, "DataSource must not be null.");

		try {
			this.con = dataSource.getConnection();
			this.stmt = this.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
					ResultSet.HOLD_CURSORS_OVER_COMMIT);
			applyStatementSettings(this.stmt);
			this.rs = this.stmt.executeQuery(sql);
			handleWarnings(this.stmt.getWarnings());
		}
		catch (SQLException se) {
			close();
			throw getExceptionTranslator().translate("Executing query", sql, se);
		}

	}

	/*
	 * Prepare the given JDBC Statement (or PreparedStatement or
	 * CallableStatement), applying statement settings such as fetch size, max
	 * rows, and query timeout. @param stmt the JDBC Statement to prepare
	 * @throws SQLException
	 * 
	 * @see #setFetchSize
	 * @see #setMaxRows
	 * @see #setQueryTimeout
	 */
	private void applyStatementSettings(Statement stmt) throws SQLException {
		if (fetchSize != VALUE_NOT_SET) {
			stmt.setFetchSize(fetchSize);
			stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
		}
		if (maxRows != VALUE_NOT_SET) {
			stmt.setMaxRows(maxRows);
		}
		if (queryTimeout != VALUE_NOT_SET) {
			stmt.setQueryTimeout(queryTimeout);
		}
	}

	/*
	 * Return the exception translator for this instance. <p>Creates a default
	 * SQLErrorCodeSQLExceptionTranslator for the specified DataSource if none
	 * is set.
	 */
	protected SQLExceptionTranslator getExceptionTranslator() {
		if (exceptionTranslator == null) {
			if (dataSource != null) {
				exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
			}
			else {
				exceptionTranslator = new SQLStateSQLExceptionTranslator();
			}
		}
		return exceptionTranslator;
	}

	/*
	 * Throw a SQLWarningException if we're not ignoring warnings, else log the
	 * warnings (at debug level).
	 * 
	 * @param warning the warnings object from the current statement. May be
	 * <code>null</code>, in which case this method does nothing.
	 * 
	 * @see org.springframework.jdbc.SQLWarningException
	 */
	private void handleWarnings(SQLWarning warnings) throws SQLWarningException {
		if (ignoreWarnings) {
			SQLWarning warningToLog = warnings;
			while (warningToLog != null) {
				log.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '"
						+ warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
				warningToLog = warningToLog.getNextWarning();
			}
		}
		else if (warnings != null) {
			throw new SQLWarningException("Warning not ignored", warnings);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#getExecutionContext()
	 */
	public void beforeSave() {
		if(saveState){
			String skipped = skippedRows.toString();
			executionContext.putString(addName(SKIPPED_ROWS), skipped.substring(1, skipped.length() - 1));
			executionContext.putLong(addName(CURRENT_PROCESSED_ROW), currentProcessedRow);
			executionContext.putLong(addName(SKIP_COUNT), skipCount);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#restoreFrom(org.springframework.batch.item.ExecutionContext)
	 */
	public void open(ExecutionContext context) {
		Assert.state(!initialized, "Stream is already initialized.  Close before re-opening.");
		Assert.isNull(rs);
		Assert.notNull(context, "ExecutionContext must not be null");
		executeQuery();
		initialized = true;
		this.executionContext = context;

		// Properties restartProperties = data.getProperties();
		if (!context.containsKey(addName(CURRENT_PROCESSED_ROW))) {
			return;
		}

		try {
			this.currentProcessedRow = context.getLong(addName(CURRENT_PROCESSED_ROW));
			rs.absolute((int) currentProcessedRow);
		}
		catch (SQLException se) {
			throw getExceptionTranslator().translate("Attempted to move ResultSet to last committed row", sql, se);
		}

		if (!context.containsKey(addName(SKIPPED_ROWS))) {
			return;
		}

		String[] skipped = StringUtils.commaDelimitedListToStringArray(context.getString(addName(SKIPPED_ROWS)));
		for (int i = 0; i < skipped.length; i++) {
			this.skippedRows.add(new Long(skipped[i]));
		}
	}

	/**
	 * Skip the current row. If the transaction is rolled back, this row will
	 * not be represented to the RowMapper when read() is called. For example,
	 * if you read in row 2, find the data to be bad, and call skip(), then
	 * continue processing and find
	 */
	public void skip() {
		skippedRows.add(new Long(currentProcessedRow));
		skipCount++;
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be
	 * fetched from the database when more rows are needed for this
	 * <code>ResultSet</code> object. If the fetch size specified is zero, the
	 * JDBC driver ignores the value.
	 * 
	 * @param fetchSize the number of rows to fetch
	 * @see ResultSet#setFetchSize(int)
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Sets the limit for the maximum number of rows that any
	 * <code>ResultSet</code> object can contain to the given number.
	 * 
	 * @param maxRows the new max rows limit; zero means there is no limit
	 * @see Statement#setMaxRows(int)
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * Sets the number of seconds the driver will wait for a
	 * <code>Statement</code> object to execute to the given number of
	 * seconds. If the limit is exceeded, an <code>SQLException</code> is
	 * thrown.
	 * 
	 * @param queryTimeout seconds the new query timeout limit in seconds; zero
	 * means there is no limit
	 * @see Statement#setQueryTimeout(int)
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * Set whether SQLWarnings should be ignored (only logged) or exception
	 * should be thrown.
	 * 
	 * @param ignoreWarnings if TRUE, warnings are ignored
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * Allow verification of cursor position after current row is processed by
	 * RowMapper or RowCallbackHandler. Default value is TRUE.
	 * 
	 * @param verifyCursorPosition if true, cursor position is verified
	 */
	public void setVerifyCursorPosition(boolean verifyCursorPosition) {
		this.verifyCursorPosition = verifyCursorPosition;
	}

	/**
	 * Set the RowMapper to be used for all calls to read().
	 * 
	 * @param mapper
	 */
	public void setMapper(RowMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Set the sql statement to be used when creating the cursor. This statement
	 * should be a complete and valid Sql statement, as it will be run directly
	 * without any modification.
	 * 
	 * @param sql
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * Return the item itself (which is already a key).
	 * @see org.springframework.batch.item.ItemReader#getKey(java.lang.Object)
	 */
	public Object getKey(Object item) {
		return item;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String addName(String key){
		return name + "." + key;
	}
	
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}
