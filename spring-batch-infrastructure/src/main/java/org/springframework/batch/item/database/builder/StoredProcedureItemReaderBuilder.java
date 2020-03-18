/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import javax.sql.DataSource;

import org.springframework.batch.item.database.AbstractCursorItemReader;

import org.springframework.batch.item.database.StoredProcedureItemReader;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A fluent builder API for the configuration of a {@link StoredProcedureItemReader}.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @since 4.0.0
 * @see StoredProcedureItemReader
 */
public class StoredProcedureItemReaderBuilder<T> {

	public static final int VALUE_NOT_SET = -1;

	private int currentItemCount = 0;

	private int maxItemCount = Integer.MAX_VALUE;

	private boolean saveState = true;

	private DataSource dataSource;

	private int fetchSize = VALUE_NOT_SET;

	private int maxRows = VALUE_NOT_SET;

	private int queryTimeout = VALUE_NOT_SET;

	private boolean ignoreWarnings = true;

	private boolean verifyCursorPosition = true;

	private boolean driverSupportsAbsolute = false;

	private boolean useSharedExtendedConnection = false;

	private PreparedStatementSetter preparedStatementSetter;

	private RowMapper<T> rowMapper;

	private String procedureName;

	private SqlParameter[] parameters = new SqlParameter[0];

	private boolean function = false;

	private int refCursorPosition = 0;

	private String name;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public StoredProcedureItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public StoredProcedureItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public StoredProcedureItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public StoredProcedureItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * The {@link DataSource} to read from
	 *
	 * @param dataSource a relational data base
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setDataSource(DataSource)
	 */
	public StoredProcedureItemReaderBuilder<T> dataSource(DataSource dataSource) {
		this.dataSource = dataSource;

		return this;
	}

	/**
	 * A hint to the driver as to how many rows to return with each fetch.
	 *
	 * @param fetchSize the hint
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setFetchSize(int)
	 */
	public StoredProcedureItemReaderBuilder<T> fetchSize(int fetchSize) {
		this.fetchSize = fetchSize;

		return this;
	}

	/**
	 * The max number of rows the {@link java.sql.ResultSet} can contain
	 *
	 * @param maxRows the max
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setMaxRows(int)
	 */
	public StoredProcedureItemReaderBuilder<T> maxRows(int maxRows) {
		this.maxRows = maxRows;

		return this;
	}

	/**
	 * The time in milliseconds for the query to timeout
	 *
	 * @param queryTimeout timeout
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setQueryTimeout(int)
	 */
	public StoredProcedureItemReaderBuilder<T> queryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;

		return this;
	}

	/**
	 * Indicates if SQL warnings should be ignored or if an exception should be thrown.
	 *
	 * @param ignoreWarnings indicator. Defaults to true
	 * @return this instance for method chaining
	 * @see AbstractCursorItemReader#setIgnoreWarnings(boolean)
	 */
	public StoredProcedureItemReaderBuilder<T> ignoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;

		return this;
	}

	/**
	 * Indicates if the reader should verify the current position of the
	 * {@link java.sql.ResultSet} after being passed to the {@link RowMapper}.  Defaults
	 * to true.
	 *
	 * @param verifyCursorPosition indicator
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setVerifyCursorPosition(boolean)
	 */
	public StoredProcedureItemReaderBuilder<T> verifyCursorPosition(boolean verifyCursorPosition) {
		this.verifyCursorPosition = verifyCursorPosition;

		return this;
	}

	/**
	 * Indicates if the JDBC driver supports setting the absolute row on the
	 * {@link java.sql.ResultSet}.
	 *
	 * @param driverSupportsAbsolute indicator
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setDriverSupportsAbsolute(boolean)
	 */
	public StoredProcedureItemReaderBuilder<T> driverSupportsAbsolute(boolean driverSupportsAbsolute) {
		this.driverSupportsAbsolute = driverSupportsAbsolute;

		return this;
	}

	/**
	 * Indicates that the connection used for the cursor is being used by all other
	 * processing, therefor part of the same transaction.
	 *
	 * @param useSharedExtendedConnection indicator
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setUseSharedExtendedConnection(boolean)
	 */
	public StoredProcedureItemReaderBuilder<T> useSharedExtendedConnection(boolean useSharedExtendedConnection) {
		this.useSharedExtendedConnection = useSharedExtendedConnection;

		return this;
	}

	/**
	 * Configures the provided {@link PreparedStatementSetter} to be used to populate any
	 * arguments in the SQL query to be executed for the reader.
	 *
	 * @param preparedStatementSetter setter
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setPreparedStatementSetter(PreparedStatementSetter)
	 */
	public StoredProcedureItemReaderBuilder<T> preparedStatementSetter(PreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;

		return this;
	}

	/**
	 * The {@link RowMapper} used to map the results of the cursor to each item.
	 *
	 * @param rowMapper {@link RowMapper}
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setRowMapper(RowMapper)
	 */
	public StoredProcedureItemReaderBuilder<T> rowMapper(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;

		return this;
	}

	/**
	 * The name of the stored procedure to execute
	 *
	 * @param procedureName name of the procedure
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setProcedureName(String)
	 */
	public StoredProcedureItemReaderBuilder<T> procedureName(String procedureName) {
		this.procedureName = procedureName;

		return this;
	}

	/**
	 * SQL parameters to be set when executing the stored procedure
	 *
	 * @param parameters parameters to be set
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setParameters(SqlParameter[])
	 */
	public StoredProcedureItemReaderBuilder<T> parameters(SqlParameter... parameters) {
		this.parameters = parameters;

		return this;
	}

	/**
	 * Indicates the stored procedure is a function
	 *
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setFunction(boolean)
	 */
	public StoredProcedureItemReaderBuilder<T> function() {
		this.function = true;

		return this;
	}

	/**
	 * The parameter position of the REF CURSOR.  Only used for Oracle and PostgreSQL that
	 * use REF CURSORs.  For any other database, this should remain as the default (0).
	 *
	 * @param refCursorPosition the parameter position
	 * @return this instance for method chaining
	 * @see StoredProcedureItemReader#setRefCursorPosition(int)
	 */
	public StoredProcedureItemReaderBuilder<T> refCursorPosition(int refCursorPosition) {
		this.refCursorPosition = refCursorPosition;

		return this;
	}

	/**
	 * Validates configuration and builds a new reader instance
	 *
	 * @return a fully constructed {@link StoredProcedureItemReader}
	 */
	public StoredProcedureItemReader<T> build() {
		if(this.saveState) {
			Assert.hasText(this.name,
					"A name is required when saveSate is set to true");
		}

		Assert.notNull(this.procedureName, "The name of the stored procedure must be provided");
		Assert.notNull(this.dataSource, "A datasource is required");
		Assert.notNull(this.rowMapper, "A rowmapper is required");

		StoredProcedureItemReader<T> itemReader = new StoredProcedureItemReader<>();

		if(StringUtils.hasText(this.name)) {
			itemReader.setName(this.name);
		}

		itemReader.setProcedureName(this.procedureName);
		itemReader.setRowMapper(this.rowMapper);
		itemReader.setParameters(this.parameters);
		itemReader.setPreparedStatementSetter(this.preparedStatementSetter);
		itemReader.setFunction(this.function);
		itemReader.setRefCursorPosition(this.refCursorPosition);
		itemReader.setCurrentItemCount(this.currentItemCount);
		itemReader.setDataSource(this.dataSource);
		itemReader.setDriverSupportsAbsolute(this.driverSupportsAbsolute);
		itemReader.setFetchSize(this.fetchSize);
		itemReader.setIgnoreWarnings(this.ignoreWarnings);
		itemReader.setMaxItemCount(this.maxItemCount);
		itemReader.setMaxRows(this.maxRows);
		itemReader.setQueryTimeout(this.queryTimeout);
		itemReader.setSaveState(this.saveState);
		itemReader.setUseSharedExtendedConnection(this.useSharedExtendedConnection);
		itemReader.setVerifyCursorPosition(this.verifyCursorPosition);

		return itemReader;
	}

}
