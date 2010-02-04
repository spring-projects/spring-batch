/*
 * Copyright 2006-2009 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * Item reader implementation that executes a stored procedure and then reads the returned cursor 
 * and continually retrieves the next row in the <code>ResultSet</code>. 
 * </p>
 * 
 * <p>
 * The callable statement used to open the cursor is created with the 'READ_ONLY' option as well as with the 
 * 'TYPE_FORWARD_ONLY' option. By default the cursor will be opened using a separate connection which means 
 * that it will not participate in any transactions created as part of the step processing.
 * </p>
 *  
 * <p>
 * Each call to {@link #read()} will call the provided RowMapper, passing in the
 * ResultSet. 
 * </p>
 * 
 * <p>
 * This class is modeled after the similar <code>JdbcCursorItemReader</code> class. 
 * </p>
 * 
 * @author Thomas Risberg
 */
public class StoredProcedureItemReader<T> extends AbstractCursorItemReader<T> {

	private CallableStatement callableStatement;

	private PreparedStatementSetter preparedStatementSetter;

	private String procedureName;
	
	private String callString;

	private RowMapper rowMapper;

	private SqlParameter[] parameters = new SqlParameter[0];
	
	private boolean function = false;
	
	private int refCursorPosition = 0;

	public StoredProcedureItemReader() {
		super();
		setName(ClassUtils.getShortName(StoredProcedureItemReader.class));
	}

	/**
	 * Set the RowMapper to be used for all calls to read().
	 * 
	 * @param rowMapper
	 */
	public void setRowMapper(RowMapper rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * Set the SQL statement to be used when creating the cursor. This statement
	 * should be a complete and valid SQL statement, as it will be run directly
	 * without any modification.
	 * 
	 * @param sprocedureName
	 */
	public void setProcedureName(String sprocedureName) {
		this.procedureName = sprocedureName;
	}

	/**
	 * Set the PreparedStatementSetter to use if any parameter values that need
	 * to be set in the supplied query.
	 * 
	 * @param preparedStatementSetter
	 */
	public void setPreparedStatementSetter(PreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Add one or more declared parameters. Used for configuring this operation when used in a 
	 * bean factory. Each parameter will specify SQL type and (optionally) the parameter's name. 
	 * 
	 * @param parameters Array containing the declared <code>SqlParameter</code> objects
	 */
	public void setParameters(SqlParameter[] parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * Set whether this stored procedure is a function.
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * Set the parameter position of the REF CURSOR. Only used for Oracle and
	 * PostgreSQL that use REF CURSORs. For any other database this should be 
	 * kept as 0 which is the default.
	 *  
	 * @param refCursorPosition The parameter position of the REF CURSOR
	 */
	public void setRefCursorPosition(int refCursorPosition) {
		this.refCursorPosition = refCursorPosition;
	}

	/**
	 * Assert that mandatory properties are set.
	 * 
	 * @throws IllegalArgumentException if either data source or sql properties
	 * not set.
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(procedureName, "The name of the stored procedure must be provided");
		Assert.notNull(rowMapper, "RowMapper must be provided");
	}

	protected void openCursor(Connection con) {	

		Assert.state(procedureName != null, "Procedure Name must not be null.");
		Assert.state(refCursorPosition >= 0, 
				"invalid refCursorPosition specified as " + refCursorPosition + "; it can't be " +
				"specified as a negative number.");
		Assert.state(refCursorPosition == 0 || refCursorPosition > 0, 
				"invalid refCursorPosition specified as " + refCursorPosition + "; there are " + 
				parameters.length + " parameters defined.");

		CallMetaDataContext callContext = new CallMetaDataContext();
		callContext.setAccessCallParameterMetaData(false);
		callContext.setProcedureName(procedureName);
		callContext.setFunction(function);
		callContext.initializeMetaData(getDataSource());
		callContext.processParameters(Arrays.asList(parameters));
		SqlParameter cursorParameter = callContext.createReturnResultSetParameter("cursor", rowMapper);
		this.callString = callContext.createCallString();

		log.debug("Call string is: " + callString);
		
		int cursorSqlType = Types.OTHER;
		if (function) {
			if (cursorParameter instanceof SqlOutParameter) {
				cursorSqlType = cursorParameter.getSqlType();
			}
		}
		else {
			if (refCursorPosition > 0 && refCursorPosition <= parameters.length) {
				cursorSqlType = parameters[refCursorPosition - 1].getSqlType();
			}
		}
		
		try {
			if (isUseSharedExtendedConnection()) {
				callableStatement = con.prepareCall(callString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
						ResultSet.HOLD_CURSORS_OVER_COMMIT);
			}
			else {
				callableStatement = con.prepareCall(callString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			}
			applyStatementSettings(callableStatement);
			if (this.preparedStatementSetter != null) {
				preparedStatementSetter.setValues(callableStatement);
			}
			
			if (function) {
				callableStatement.registerOutParameter(1, cursorSqlType);
			}
			else {
				if (refCursorPosition > 0) {
					callableStatement.registerOutParameter(refCursorPosition, cursorSqlType);
				}
			}
			boolean results = callableStatement.execute();
			if (results) {
				rs = callableStatement.getResultSet();
			}
			else {
				if (function) {
					rs = (ResultSet) callableStatement.getObject(1);
				}
				else {
					rs = (ResultSet) callableStatement.getObject(refCursorPosition);
				}
			}
			handleWarnings(callableStatement);
		}
		catch (SQLException se) {
			close();
			throw getExceptionTranslator().translate("Executing stored procedure", getSql(), se);
		}

	}

	@SuppressWarnings("unchecked")
	protected T readCursor(ResultSet rs, int currentRow) throws SQLException {
		return (T) rowMapper.mapRow(rs, currentRow);
	}

	/**
	 * Close the cursor and database connection.
	 */
	protected void cleanupOnClose() throws Exception {
		JdbcUtils.closeStatement(this.callableStatement);
	}

	@Override
	public String getSql() {
		if (callString != null) {
			return this.callString;
		}
		else {
			return "PROCEDURE NAME: " + procedureName;
		}
	}

}
