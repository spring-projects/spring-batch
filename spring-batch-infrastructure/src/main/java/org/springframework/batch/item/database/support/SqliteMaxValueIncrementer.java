package org.springframework.batch.item.database.support;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.incrementer.AbstractColumnMaxValueIncrementer;

/**
 * Implemented as a package-private class since it is required for SQLite support, but
 * should ideally be in Spring JDBC.
 *
 * @author Luke Taylor
 */
class SqliteMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	public SqliteMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}

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
