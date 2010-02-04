package org.springframework.batch.item.database;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.Types;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(JUnit4ClassRunner.class)
public class StoredprocedureItemReaderConfigTests {

	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	public void testUsesCurrentTransaction() throws Exception {
		DataSource ds = createMock(DataSource.class);
		DatabaseMetaData dmd = createNiceMock(DatabaseMetaData.class);
		expect(dmd.getDatabaseProductName()).andReturn("Oracle").times(2);
		Connection con = createMock(Connection.class);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getAutoCommit()).andReturn(false);
		CallableStatement cs = createNiceMock(CallableStatement.class);
		expect(con.prepareCall("{call foo_bar()}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT)).andReturn(cs);
		expect(ds.getConnection()).andReturn(con);
		expect(ds.getConnection()).andReturn(con);
		con.commit();
		replay(con,dmd, ds, cs);
		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final StoredProcedureItemReader<String> reader = new StoredProcedureItemReader<String>();
		reader.setDataSource(new ExtendedConnectionDataSourceProxy(ds));
		reader.setUseSharedExtendedConnection(true);
		reader.setProcedureName("foo_bar");
		final ExecutionContext ec = new ExecutionContext();
		tt.execute(
				new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						reader.open(ec);
						reader.close();
						return null;
					}
				});
		verify(ds);
	}
	
	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	public void testUsesItsOwnTransaction() throws Exception {
		
		DataSource ds = createMock(DataSource.class);
		DatabaseMetaData dmd = createNiceMock(DatabaseMetaData.class);
		expect(dmd.getDatabaseProductName()).andReturn("Oracle").times(2);
		Connection con = createMock(Connection.class);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getAutoCommit()).andReturn(false);
		CallableStatement cs = createNiceMock(CallableStatement.class);
		expect(con.prepareCall("{call foo_bar()}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).andReturn(cs);
		expect(ds.getConnection()).andReturn(con);
		expect(ds.getConnection()).andReturn(con);
		con.commit();
		replay(con,dmd, ds, cs);
		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final StoredProcedureItemReader<String> reader = new StoredProcedureItemReader<String>();
		reader.setDataSource(ds);
		reader.setProcedureName("foo_bar");
		final ExecutionContext ec = new ExecutionContext();
		tt.execute(
				new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						reader.open(ec);
						reader.close();
						return null;
					}
				});
		verify(ds);
	}

	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	public void testHandlesRefCursorPosition() throws Exception {
		
		DataSource ds = createMock(DataSource.class);
		DatabaseMetaData dmd = createNiceMock(DatabaseMetaData.class);
		expect(dmd.getDatabaseProductName()).andReturn("Oracle").times(2);
		Connection con = createMock(Connection.class);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getMetaData()).andReturn(dmd);
		expect(con.getAutoCommit()).andReturn(false);
		CallableStatement cs = createNiceMock(CallableStatement.class);
		expect(con.prepareCall("{call foo_bar(?, ?)}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).andReturn(cs);
		expect(ds.getConnection()).andReturn(con);
		expect(ds.getConnection()).andReturn(con);
		con.commit();
		replay(con,dmd, ds, cs);
		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final StoredProcedureItemReader<String> reader = new StoredProcedureItemReader<String>();
		reader.setDataSource(ds);
		reader.setProcedureName("foo_bar");
		reader.setParameters(new SqlParameter[] {
				new SqlParameter("foo", Types.VARCHAR),
				new SqlParameter("bar", Types.OTHER)});
		reader.setPreparedStatementSetter(
				new PreparedStatementSetter() {
					public void setValues(PreparedStatement ps)
							throws SQLException {
					}
				});
		reader.setRefCursorPosition(3);
		final ExecutionContext ec = new ExecutionContext();
		tt.execute(
				new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						reader.open(ec);
						reader.close();
						return null;
					}
				});
		verify(ds);
	}
}
