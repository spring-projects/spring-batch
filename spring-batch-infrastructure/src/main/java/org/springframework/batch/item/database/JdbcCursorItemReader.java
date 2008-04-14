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

package org.springframework.batch.item.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * Simple input source that opens a JDBC cursor and continually retrieves the next row in the ResultSet. It is extremely
 * important to note that the JdbcDriver used must be version 3.0 or higher. This is because earlier versions do not
 * support holding a ResultSet open over commits.
 * </p>
 * 
 * <p>
 * Each call to {@link #read()} will call the provided RowMapper, passing in the ResultSet. There is currently no
 * wrapping of the ResultSet to suppress calls to next(). However, if the RowMapper (mistakenly) increments the current
 * row, the next call to read will verify that the current row is at the expected position and throw a
 * DataAccessException if it is not. This means that, in theory, a RowMapper could read ahead, as long as it returns the
 * row back to the correct position before returning. The reason for such strictness on the ResultSet is due to the need
 * to maintain control for transactions, restartability and skippability. This ensures that each call to {@link #read()}
 * returns the ResultSet at the correct line, regardless of rollbacks, restarts, or skips.
 * </p>
 * 
 * <p>
 * {@link ExecutionContext}: The current row is returned as restart data, and when restored from that same data, the
 * cursor is opened and the current row set to the value within the restart data. Two values are stored: the current
 * line being processed and the number of lines that have been skipped.
 * </p>
 * 
 * <p>
 * Transactions: The same ResultSet is held open regardless of commits or roll backs in a surrounding transaction. This
 * means that when such a transaction is committed, the input source is notified through the {@link #mark()} and
 * {@link #reset()} so that it can save it's current row number. Later, if the transaction is rolled back, the current
 * row can be moved back to the same row number as it was on when commit was called.
 * </p>
 * 
 * <p>
 * Calling skip will indicate that a record is bad and should not be re-presented to the user if the transaction is
 * rolled back. For example, if row 2 is read in, and found to be bad, calling skip will inform the {@link ItemReader}.
 * If reading is then continued, and a rollback is necessary because of an error on output, the input source will be
 * returned to row 1. Calling read while on row 1 will move the current row to 3, not 2, because 2 has been marked as
 * skipped.
 * </p>
 * 
 * <p>
 * Calling close on this {@link ItemStream} will cause all resources it is currently using to be freed. (Connection,
 * ResultSet, etc). It is then illegal to call {@link #read()} again until it has been opened.
 * </p>
 * 
 * @author Lucas Ward
 * @author Peter Zozom
 */
public class JdbcCursorItemReader extends ExecutionContextUserSupport implements ItemReader, InitializingBean,
        ItemStream {

	private static Log log = LogFactory.getLog(JdbcCursorItemReader.class);

	public static final int VALUE_NOT_SET = -1;

	private static final String CURRENT_PROCESSED_ROW = "last.processed.row.number";

	private Connection con;

    private PreparedStatement preparedStatement;
    
    private PreparedStatementSetter preparedStatementSetter;

	protected ResultSet rs;

	private DataSource dataSource;

	private String sql;

	private int fetchSize = VALUE_NOT_SET;

	private int maxRows = VALUE_NOT_SET;

	private int queryTimeout = VALUE_NOT_SET;

	private boolean ignoreWarnings = true;

	private boolean verifyCursorPosition = true;

	private SQLExceptionTranslator exceptionTranslator;


	private RowMapper mapper;

	private boolean initialized = false;

	private boolean saveState = false;
	
	private BufferredResultSetReader bufferredReader;

	public JdbcCursorItemReader() {
		setName(ClassUtils.getShortName(JdbcCursorItemReader.class));
	}

	/**
	 * Assert that mandatory properties are set.
	 * 
	 * @throws IllegalArgumentException if either data source or sql properties not set.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource, "DataSOurce must be provided");
		Assert.notNull(sql, "The SQL query must be provided");
		Assert.notNull(mapper, "RowMapper must be provided");
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
	 * Increment the cursor to the next row, validating the cursor position and passing the resultset to the RowMapper.
	 * If read has not been called on this instance before, the cursor will be opened. If there are skipped records for
	 * this commit scope, an internal list of skipped records will be checked to ensure that only a valid row is given
	 * to the mapper.
	 * 
	 * @returns Object returned by RowMapper
	 * @throws DataAccessException
	 * @throws IllegalStateExceptino if mapper is null.
	 */
	public Object read() throws Exception{

		return bufferredReader.read();
	}

	public long getCurrentProcessedRow() {
		return bufferredReader.getProcessedRowCount();
	}

	/**
	 * Mark the current row. Calling reset will cause the result set to be set to the current row when mark was called.
	 */
	public void mark() {
		bufferredReader.mark();
	}

	/**
	 * Set the ResultSet's current row to the last marked position.
	 * 
	 * @throws DataAccessException
	 */
	public void reset() throws ResetFailedException {
		bufferredReader.reset();
	}

	/**
	 * Close this input source. The ResultSet, Statement and Connection created will be closed. This must be called or
	 * the connection and cursor will be held open indefinitely!
	 * 
	 * @see org.springframework.batch.item.ResourceLifecycle#close(ExecutionContext)
	 */
	public void close(ExecutionContext executionContext) {
		initialized = false;
		JdbcUtils.closeResultSet(this.rs);
		JdbcUtils.closeStatement(this.preparedStatement);
		JdbcUtils.closeConnection(this.con);
		bufferredReader = null;
	}


	/*
	 * Executes the provided SQL query. The statement is created with 'READ_ONLY' and 'HOLD_CUSORS_OVER_COMMIT' set to
	 * true. This is extremely important, since a non read-only cursor may lock tables that shouldn't be locked, and not
	 * holding the cursor open over a commit would require it to be reopened after each commit, which would destroy
	 * performance.
	 */
	private void executeQuery() {

		Assert.state(dataSource != null, "DataSource must not be null.");

		try {
			this.con = dataSource.getConnection();
			preparedStatement = this.con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
			        ResultSet.HOLD_CURSORS_OVER_COMMIT);
			applyStatementSettings(preparedStatement);
			if(this.preparedStatementSetter != null){
				preparedStatementSetter.setValues(preparedStatement);
			}
			this.rs = preparedStatement.executeQuery();
			handleWarnings(preparedStatement.getWarnings());
		} catch (SQLException se) {
			close(null);
			throw getExceptionTranslator().translate("Executing query", sql, se);
		}

	}

	/*
	 * Prepare the given JDBC Statement (or PreparedStatement or CallableStatement), applying statement settings such as
	 * fetch size, max rows, and query timeout. @param stmt the JDBC Statement to prepare @throws SQLException
	 * 
	 * @see #setFetchSize
	 * @see #setMaxRows
	 * @see #setQueryTimeout
	 */
	private void applyStatementSettings(PreparedStatement stmt) throws SQLException {
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
	 * Return the exception translator for this instance. <p>Creates a default SQLErrorCodeSQLExceptionTranslator for
	 * the specified DataSource if none is set.
	 */
	protected SQLExceptionTranslator getExceptionTranslator() {
		if (exceptionTranslator == null) {
			if (dataSource != null) {
				exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
			} else {
				exceptionTranslator = new SQLStateSQLExceptionTranslator();
			}
		}
		return exceptionTranslator;
	}

	/*
	 * Throw a SQLWarningException if we're not ignoring warnings, else log the warnings (at debug level).
	 * 
	 * @param warning the warnings object from the current statement. May be <code>null</code>, in which case this
	 * method does nothing.
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
		} else if (warnings != null) {
			throw new SQLWarningException("Warning not ignored", warnings);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#getExecutionContext()
	 */
	public void update(ExecutionContext executionContext) {
		if (saveState && initialized) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putLong(getKey(CURRENT_PROCESSED_ROW), bufferredReader.getProcessedRowCount());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#restoreFrom(org.springframework.batch.item.ExecutionContext)
	 */
	public void open(ExecutionContext context) {
		Assert.state(!initialized, "Stream is already initialized.  Close before re-opening.");
		Assert.isNull(rs);
		Assert.notNull(context, "ExecutionContext must not be null");
		executeQuery();
		initialized = true;
		long processedRowCount = 0;

		if (context.containsKey(getKey(CURRENT_PROCESSED_ROW))) {
			try {
				processedRowCount = context.getLong(getKey(CURRENT_PROCESSED_ROW));
				while(rs.next()){
					if(rs.getRow() == processedRowCount){
						break;
					}
				}
			} catch (SQLException se) {
				throw getExceptionTranslator().translate("Attempted to move ResultSet to last committed row", sql, se);
			}
		}
		
		bufferredReader = new BufferredResultSetReader(rs, mapper, processedRowCount);
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when more rows are
	 * needed for this <code>ResultSet</code> object. If the fetch size specified is zero, the JDBC driver ignores the
	 * value.
	 * 
	 * @param fetchSize the number of rows to fetch
	 * @see ResultSet#setFetchSize(int)
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * Sets the limit for the maximum number of rows that any <code>ResultSet</code> object can contain to the given
	 * number.
	 * 
	 * @param maxRows the new max rows limit; zero means there is no limit
	 * @see Statement#setMaxRows(int)
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * Sets the number of seconds the driver will wait for a <code>Statement</code> object to execute to the given
	 * number of seconds. If the limit is exceeded, an <code>SQLException</code> is thrown.
	 * 
	 * @param queryTimeout seconds the new query timeout limit in seconds; zero means there is no limit
	 * @see Statement#setQueryTimeout(int)
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * Set whether SQLWarnings should be ignored (only logged) or exception should be thrown.
	 * 
	 * @param ignoreWarnings if TRUE, warnings are ignored
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * Allow verification of cursor position after current row is processed by RowMapper or RowCallbackHandler. Default
	 * value is TRUE.
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
	 * Set the sql statement to be used when creating the cursor. This statement should be a complete and valid Sql
	 * statement, as it will be run directly without any modification.
	 * 
	 * @param sql
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}
	
	/**
	 * Set the PreparedStatementSetter to use if any parameter values that need to be set in the supplied
	 * query.
	 * 
	 * @param preparedStatementSetter
	 */
	public void setPreparedStatementSetter(
			PreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Set whether this {@link ItemReader} should save it's state in the
	 * {@link ExecutionContext} or not
	 * 
	 * @param saveState
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
	
	

	
	private class BufferredResultSetReader implements ItemReader{
		
		private ResultSet rs;
		private RowMapper rowMapper;
		private List buffer;
		private int currentIndex;
		private long processedRowCount;
		private int INITIAL_POSITION = -1;
		
		public BufferredResultSetReader(ResultSet rs, RowMapper rowMapper, long processedRowCount) {
			Assert.notNull(rs, "The ResultSet must not be null");
			Assert.notNull(rowMapper, "The RowMapper must not be null");
			this.rs = rs;
			this.rowMapper = rowMapper;
			buffer = new ArrayList();
			currentIndex = INITIAL_POSITION;
			this.processedRowCount = processedRowCount;
		}
		
		public BufferredResultSetReader(ResultSet rs, RowMapper rowMapper){
			this(rs, rowMapper, 0);
		}

		public Object read() throws Exception, UnexpectedInputException,
			NoWorkFoundException, ParseException {
			
			
			if(buffer.size() > currentIndex){
				currentIndex++;
				try{
					if(!rs.next()){
						return null;
					}
					int currentRow = rs.getRow();
					buffer.add(rowMapper.mapRow(rs, currentRow));
					verifyCursorPosition(currentRow);
				}
				catch(SQLException se){
					throw getExceptionTranslator().translate("Attempt to process next row failed", sql, se);
				}
			}
			
			processedRowCount++;
			return buffer.get(currentIndex);
		}
		
		public void mark() throws MarkFailedException {
			buffer.clear();
			currentIndex = INITIAL_POSITION;
		}

		public void reset() throws ResetFailedException {
			processedRowCount -= buffer.size();
			currentIndex = INITIAL_POSITION;
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
		
		public long getProcessedRowCount() {
			return processedRowCount;
		}

	}
}
