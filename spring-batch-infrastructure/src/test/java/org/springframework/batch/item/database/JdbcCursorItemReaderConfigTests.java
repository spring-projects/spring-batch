package org.springframework.batch.item.database;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(JUnit4ClassRunner.class)
public class JdbcCursorItemReaderConfigTests {

	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	public void testUsesCurrentTransaction() throws Exception {
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		expect(con.getAutoCommit()).andReturn(false);
		PreparedStatement ps = createNiceMock(PreparedStatement.class);
		expect(con.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT)).andReturn(ps);
		expect(ds.getConnection()).andReturn(con);
		expect(ds.getConnection()).andReturn(con);
		con.commit();
		replay(con, ds, ps);
		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<String>();
		reader.setDataSource(new ExtendedConnectionDataSourceProxy(ds));
		reader.setUseSharedExtendedConnection(true);
		reader.setSql("select foo from bar");
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
		Connection con = createMock(Connection.class);
		expect(con.getAutoCommit()).andReturn(false);
		PreparedStatement ps = createNiceMock(PreparedStatement.class);
		expect(con.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).andReturn(ps);
		expect(ds.getConnection()).andReturn(con);
		expect(ds.getConnection()).andReturn(con);
		con.commit();
		replay(con, ds, ps);
		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<String>();
		reader.setDataSource(ds);
		reader.setSql("select foo from bar");
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
