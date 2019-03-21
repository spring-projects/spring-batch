/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.item.database.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.incrementer.AbstractColumnMaxValueIncrementer;

/**
 * Implemented as a package-private class since it is required for SQLite support, but
 * should ideally be in Spring JDBC.
 *
 * @author Luke Taylor
 * @since 3.0
 */
class SqliteMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	public SqliteMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer#getNextKey()
	 */
	@Override
	protected long getNextKey() {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
			stmt.executeUpdate("insert into " + getIncrementerName() + " values(null)");
			ResultSet rs = stmt.executeQuery("select max(rowid) from " + getIncrementerName());
			if (!rs.next()) {
				throw new DataAccessResourceFailureException("rowid query failed after executing an update");
			}
			long nextKey = rs.getLong(1);
			stmt.executeUpdate("delete from " + getIncrementerName() + " where " + getColumnName() + " < " + nextKey);
			return nextKey;
		}
		catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain rowid", ex);
		}
		finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}
}
