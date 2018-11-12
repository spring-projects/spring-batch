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
import java.sql.SQLWarning;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * <p>
 * Abstract base class for any simple item reader that opens a database cursor and continually retrieves
 * the next row in the ResultSet.
 * </p>
 *
 * <p>
 * By default the cursor will be opened using a separate connection. The ResultSet for the cursor
 * is held open regardless of commits or roll backs in a surrounding transaction. Clients of this
 * reader are responsible for buffering the items in the case that they need to be re-presented on a
 * rollback. This buffering is handled by the step implementations provided and is only a concern for
 * anyone writing their own step implementations.
 * </p>
 *
 * <p>
 * There is an option ({@link #setUseSharedExtendedConnection(boolean)} that will share the connection
 * used for the cursor with the rest of the step processing. If you set this flag to <code>true</code>
 * then you must wrap the DataSource in a {@link ExtendedConnectionDataSourceProxy} to prevent the
 * connection from being closed and released after each commit performed as part of the step processing.
 * You must also use a JDBC driver supporting JDBC 3.0 or later since the cursor will be opened with the
 * additional option of 'HOLD_CURSORS_OVER_COMMIT' enabled.
 * </p>
 *
 * <p>
 * Each call to {@link #read()} will attempt to map the row at the current position in the
 * ResultSet. There is currently no wrapping of the ResultSet to suppress calls
 * to next(). However, if the RowMapper (mistakenly) increments the current row,
 * the next call to read will verify that the current row is at the expected
 * position and throw a DataAccessException if it is not. The reason for such strictness on the
 * ResultSet is due to the need to maintain control for transactions and
 * restartability. This ensures that each call to {@link #read()} returns the
 * ResultSet at the correct row, regardless of rollbacks or restarts.
 * </p>
 *
 * <p>
 * {@link ExecutionContext}: The current row is returned as restart data, and
 * when restored from that same data, the cursor is opened and the current row
 * set to the value within the restart data. See
 * {@link #setDriverSupportsAbsolute(boolean)} for improving restart
 * performance.
 * </p>
 *
 * <p>
 * Calling close on this {@link ItemStream} will cause all resources it is
 * currently using to be freed. (Connection, ResultSet, etc). It is then illegal
 * to call {@link #read()} again until it has been re-opened.
 * </p>
 *
 * <p>
 * Known limitation: when used with Derby
 * {@link #setVerifyCursorPosition(boolean)} needs to be <code>false</code>
 * because {@link ResultSet#getRow()} call used for cursor position verification
 * is not available for 'TYPE_FORWARD_ONLY' result sets.
 * </p>
 *
 * @author Lucas Ward
 * @author Peter Zozom
 * @author Robert Kasanicky
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public abstract class AbstractCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T>
implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log log = LogFactory.getLog(getClass());

	public static final int VALUE_NOT_SET = -1;
	private Connection con;

	protected ResultSet rs;

	private DataSource dataSource;

	private int fetchSize = VALUE_NOT_SET;

	private int maxRows = VALUE_NOT_SET;

	private int queryTimeout = VALUE_NOT_SET;

	private boolean ignoreWarnings = true;

	private boolean verifyCursorPosition = true;

	private SQLExceptionTranslator exceptionTranslator;

	private boolean initialized = false;

	private boolean driverSupportsAbsolute = false;

	private boolean useSharedExtendedConnection = false;

	private Boolean connectionAutoCommit;

	private boolean initialConnectionAutoCommit;

	public AbstractCursorItemReader() {
		super();
	}

	/**
	 * Assert that mandatory properties are set.
	 *
	 * @throws IllegalArgumentException if either data source or SQL properties
	 * not set.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource, "DataSource must be provided");
	}

	/**
	 * Public setter for the data source for injection purposes.
	 *
	 * @param dataSource {@link javax.sql.DataSource} to be used
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Public getter for the data source.
	 *
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Prepare the given JDBC Statement (or PreparedStatement or
	 * CallableStatement), applying statement settings such as fetch size, max
	 * rows, and query timeout. @param stmt the JDBC Statement to prepare
	 *
	 * @param stmt {@link java.sql.PreparedStatement} to be configured
	 *
	 * @throws SQLException if interactions with provided stmt fail
	 *
	 * @see #setFetchSize
	 * @see #setMaxRows
	 * @see #setQueryTimeout
	 */
	protected void applyStatementSettings(PreparedStatement stmt) throws SQLException {
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

	/**
	 * Creates a default SQLErrorCodeSQLExceptionTranslator for the specified
	 * DataSource if none is set.
	 *
	 * @return the exception translator for this instance.
	 */
	protected SQLExceptionTranslator getExceptionTranslator() {
		synchronized(this) {
			if (exceptionTranslator == null) {
				if (dataSource != null) {
					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
				}
				else {
					exceptionTranslator = new SQLStateSQLExceptionTranslator();
				}
			}
		}
		return exceptionTranslator;
	}

	/**
	 * Throw a SQLWarningException if we're not ignoring warnings, else log the
	 * warnings (at debug level).
	 *
	 * @param statement the current statement to obtain the warnings from, if there are any.
	 * @throws SQLException if interaction with provided statement fails.
	 *
	 * @see org.springframework.jdbc.SQLWarningException
	 */
	protected void handleWarnings(Statement statement) throws SQLWarningException,
	SQLException {
		if (ignoreWarnings) {
			if (log.isDebugEnabled()) {
				SQLWarning warningToLog = statement.getWarnings();
				while (warningToLog != null) {
					log.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '"
							+ warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		}
		else {
			SQLWarning warnings = statement.getWarnings();
			if (warnings != null) {
				throw new SQLWarningException("Warning not ignored", warnings);
			}
		}
	}

	/**
	 * Moves the cursor in the ResultSet to the position specified by the row
	 * parameter by traversing the ResultSet.
	 * @param row The index of the row to move to
	 */
	private void moveCursorToRow(int row) {
		try {
			int count = 0;
			while (row != count && rs.next()) {
				count++;
			}
		}
		catch (SQLException se) {
			throw getExceptionTranslator().translate("Attempted to move ResultSet to last committed row", getSql(), se);
		}
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
	 * <code>Statement</code> object to execute to the given number of seconds.
	 * If the limit is exceeded, an <code>SQLException</code> is thrown.
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
	 * Indicate whether the JDBC driver supports setting the absolute row on a
	 * {@link ResultSet}. It is recommended that this is set to
	 * <code>true</code> for JDBC drivers that supports ResultSet.absolute() as
	 * it may improve performance, especially if a step fails while working with
	 * a large data set.
	 *
	 * @see ResultSet#absolute(int)
	 *
	 * @param driverSupportsAbsolute <code>false</code> by default
	 */
	public void setDriverSupportsAbsolute(boolean driverSupportsAbsolute) {
		this.driverSupportsAbsolute = driverSupportsAbsolute;
	}

	/**
	 * Indicate whether the connection used for the cursor should be used by all other processing
	 * thus sharing the same transaction. If this is set to false, which is the default, then the
	 * cursor will be opened using in its connection and will not participate in any transactions
	 * started for the rest of the step processing. If you set this flag to true then you must
	 * wrap the DataSource in a {@link ExtendedConnectionDataSourceProxy} to prevent the
	 * connection from being closed and released after each commit.
	 *
	 * When you set this option to <code>true</code> then the statement used to open the cursor
	 * will be created with both 'READ_ONLY' and 'HOLD_CURSORS_OVER_COMMIT' options. This allows
	 * holding the cursor open over transaction start and commits performed in the step processing.
	 * To use this feature you need a database that supports this and a JDBC driver supporting
	 * JDBC 3.0 or later.
	 *
	 * @param useSharedExtendedConnection <code>false</code> by default
	 */
	public void setUseSharedExtendedConnection(boolean useSharedExtendedConnection) {
		this.useSharedExtendedConnection = useSharedExtendedConnection;
	}

	public boolean isUseSharedExtendedConnection() {
		return useSharedExtendedConnection;
	}

	/**
	 * Set whether "autoCommit" should be overridden for the connection used by the cursor. If not set, defaults to
	 * Connection / Datasource default configuration.
	 *
	 * @param autoCommit value used for {@link Connection#setAutoCommit(boolean)}.
	 * @since 4.0
	 */
	public void setConnectionAutoCommit(boolean autoCommit) {
		this.connectionAutoCommit = autoCommit;
	}

	public abstract String getSql();

	/**
	 * Check the result set is in sync with the currentRow attribute. This is
	 * important to ensure that the user hasn't modified the current row.
	 */
	private void verifyCursorPosition(long expectedCurrentRow) throws SQLException {
		if (verifyCursorPosition) {
			if (expectedCurrentRow != this.rs.getRow()) {
				throw new InvalidDataAccessResourceUsageException("Unexpected cursor position change.");
			}
		}
	}

	/**
	 * Close the cursor and database connection. Make call to cleanupOnClose so sub classes can cleanup
	 * any resources they have allocated.
	 */
	@Override
	protected void doClose() throws Exception {
		initialized = false;
		JdbcUtils.closeResultSet(this.rs);
		rs = null;
		cleanupOnClose(con);

		if(this.con != null && !this.con.isClosed()) {
			this.con.setAutoCommit(this.initialConnectionAutoCommit);
		}

		if (useSharedExtendedConnection && dataSource instanceof ExtendedConnectionDataSourceProxy) {
			((ExtendedConnectionDataSourceProxy)dataSource).stopCloseSuppression(this.con);
			if (!TransactionSynchronizationManager.isActualTransactionActive()) {
				DataSourceUtils.releaseConnection(con, dataSource);
			}
		}
		else {
			JdbcUtils.closeConnection(this.con);
		}
	}

	/**
	 * Clean up resources.
	 * @throws Exception If unable to clean up resources
	 * @deprecated This method is deprecated in favor of
	 * {@link AbstractCursorItemReader#cleanupOnClose(java.sql.Connection)} and
	 * will be removed in a future release
	 */
	@Deprecated
	protected abstract void cleanupOnClose()  throws Exception;

	/**
	 * Clean up resources.
	 * @param connection to the database
	 * @throws Exception If unable to clean up resources
	 */
	protected abstract void cleanupOnClose(Connection connection)  throws Exception;

	/**
	 * Execute the statement to open the cursor.
	 */
	@Override
	protected void doOpen() throws Exception {

		Assert.state(!initialized, "Stream is already initialized.  Close before re-opening.");
		Assert.isNull(rs, "ResultSet still open!  Close before re-opening.");

		initializeConnection();
		openCursor(con);
		initialized = true;

	}

	protected void initializeConnection() {
		Assert.state(getDataSource() != null, "DataSource must not be null.");

		try {
			if (useSharedExtendedConnection) {
				if (!(getDataSource() instanceof ExtendedConnectionDataSourceProxy)) {
					throw new InvalidDataAccessApiUsageException(
							"You must use a ExtendedConnectionDataSourceProxy for the dataSource when " +
							"useSharedExtendedConnection is set to true.");
				}
				this.con = DataSourceUtils.getConnection(dataSource);
				((ExtendedConnectionDataSourceProxy)dataSource).startCloseSuppression(this.con);
			}
			else {
				this.con = dataSource.getConnection();
			}

			this.initialConnectionAutoCommit = this.con.getAutoCommit();

			if (this.connectionAutoCommit != null && this.con.getAutoCommit() != this.connectionAutoCommit) {
				this.con.setAutoCommit(this.connectionAutoCommit);
			}
		}
		catch (SQLException se) {
			close();
			throw getExceptionTranslator().translate("Executing query", getSql(), se);
		}
	}

	protected abstract void openCursor(Connection con);

	/**
	 * Read next row and map it to item, verify cursor position if
	 * {@link #setVerifyCursorPosition(boolean)} is true.
	 */
	@Nullable
	@Override
	protected T doRead() throws Exception {
		if (rs == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}

		try {
			if (!rs.next()) {
				return null;
			}
			int currentRow = getCurrentItemCount();
			T item = readCursor(rs, currentRow);
			verifyCursorPosition(currentRow);
			return item;
		}
		catch (SQLException se) {
			throw getExceptionTranslator().translate("Attempt to process next row failed", getSql(), se);
		}
	}

	/**
	 * Read the cursor and map to the type of object this reader should return. This method must be
	 * overridden by subclasses.
	 *
	 * @param rs The current result set
	 * @param currentRow Current position of the result set
	 * @return the mapped object at the cursor position
	 * @throws SQLException if interactions with the current result set fail
	 */
	@Nullable
	protected abstract T readCursor(ResultSet rs, int currentRow) throws SQLException;

	/**
	 * Use {@link ResultSet#absolute(int)} if possible, otherwise scroll by
	 * calling {@link ResultSet#next()}.
	 */
	@Override
	protected void jumpToItem(int itemIndex) throws Exception {
		if (driverSupportsAbsolute) {
			try {
				rs.absolute(itemIndex);
			}
			catch (SQLException e) {
				// Driver does not support rs.absolute(int) revert to
				// traversing ResultSet
				log.warn("The JDBC driver does not appear to support ResultSet.absolute(). Consider"
						+ " reverting to the default behavior setting the driverSupportsAbsolute to false", e);

				moveCursorToRow(itemIndex);
			}
		}
		else {
			moveCursorToRow(itemIndex);
		}
	}

}
