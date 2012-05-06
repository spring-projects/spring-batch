package org.springframework.batch.item.database;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SmartDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(JUnit4ClassRunner.class)
public class ExtendedConnectionDataSourceProxyTests {

	@Test
	public void testOperationWithDataSourceUtils() throws SQLException {
		Connection con = createMock(Connection.class);
		DataSource ds = createMock(DataSource.class);

		expect(ds.getConnection()).andReturn(con); // con1
		con.close();
		expect(ds.getConnection()).andReturn(con); // con2
		con.close();

		expect(ds.getConnection()).andReturn(con); // con3
		con.close(); // con3
		expect(ds.getConnection()).andReturn(con); // con4
		con.close(); // con4

		replay(ds);
		replay(con);

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy(ds);

		Connection con1 = csds.getConnection();
		Connection con2 = csds.getConnection();
		assertNotSame("shouldn't be the same connection", con1, con2);

		assertTrue("should be able to close connection", csds.shouldClose(con1));
		con1.close();
		assertTrue("should be able to close connection", csds.shouldClose(con2));
		con2.close();

		Connection con3 = csds.getConnection();
		csds.startCloseSuppression(con3);
		Connection con3_1 = csds.getConnection();
		assertSame("should be same connection", con3_1, con3);
		assertFalse("should not be able to close connection", csds.shouldClose(con3));
		con3_1.close(); // no mock call for this - should be suppressed
		Connection con3_2 = csds.getConnection();
		assertSame("should be same connection", con3_2, con3);
		Connection con4 = csds.getConnection();
		assertNotSame("shouldn't be same connection", con4, con3);
		csds.stopCloseSuppression(con3);
		assertTrue("should be able to close connection", csds.shouldClose(con3));
		con3_1 = null;
		con3_2 = null;
		con3.close();
		assertTrue("should be able to close connection", csds.shouldClose(con4));
		con4.close();

		verify(ds);
		verify(con);

	}

	@Test
	public void testOperationWithDirectCloseCall() throws SQLException {
		Connection con = createMock(Connection.class);
		DataSource ds = createMock(DataSource.class);

		expect(ds.getConnection()).andReturn(con); // con1
		con.close();
		expect(ds.getConnection()).andReturn(con); // con2
		con.close();

		replay(ds);
		replay(con);

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy(ds);

		Connection con1 = csds.getConnection();
		csds.startCloseSuppression(con1);
		Connection con1_1 = csds.getConnection();
		assertSame("should be same connection", con1_1, con1);
		con1_1.close(); // no mock call for this - should be suppressed
		Connection con1_2 = csds.getConnection();
		assertSame("should be same connection", con1_2, con1);
		Connection con2 = csds.getConnection();
		assertNotSame("shouldn't be same connection", con2, con1);
		csds.stopCloseSuppression(con1);
		assertTrue("should be able to close connection", csds.shouldClose(con1));
		con1_1 = null;
		con1_2 = null;
		con1.close();
		assertTrue("should be able to close connection", csds.shouldClose(con2));
		con2.close();

		verify(ds);
		verify(con);

	}

	@Test
	public void testSupressOfCloseWithJdbcTemplate() throws Exception {

		Connection con = createMock(Connection.class);
		DataSource ds = createMock(DataSource.class);
		Statement stmt = createMock(Statement.class);
		ResultSet rs = createMock(ResultSet.class);

		// open and start suppressing close
		expect(ds.getConnection()).andReturn(con);

		// transaction 1
		expect(con.getAutoCommit()).andReturn(false);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select baz from bar")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select foo from bar")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		con.commit();

		// transaction 2
		expect(con.getAutoCommit()).andReturn(false);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select ham from foo")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		// REQUIRES_NEW transaction
		expect(ds.getConnection()).andReturn(con);
		expect(con.getAutoCommit()).andReturn(false);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select 1 from eggs")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		con.commit();
		con.close();
		// resume transaction 2
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select more, ham from foo")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		con.commit();

		// transaction 3
		expect(con.getAutoCommit()).andReturn(false);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select spam from ham")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		con.commit();

		// stop suppressing close and close
		con.close();

		// standalone query
		expect(ds.getConnection()).andReturn(con);
		expect(con.createStatement()).andReturn(stmt);
		expect(stmt.executeQuery("select egg from bar")).andReturn(rs);
		expect(rs.next()).andReturn(false);
		con.close();

		replay(rs);
		replay(stmt);
		replay(con);
		replay(ds);

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy();
		csds.setDataSource(ds);
		PlatformTransactionManager tm = new DataSourceTransactionManager(csds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final JdbcTemplate template = new JdbcTemplate(csds);

		Connection connection = DataSourceUtils.getConnection(csds);
		csds.startCloseSuppression(connection);
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				template.queryForList("select baz from bar");
				template.queryForList("select foo from bar");
				return null;
			}
		});
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				template.queryForList("select ham from foo");
				tt2.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						template.queryForList("select 1 from eggs");
						return null;
					}
				});
				template.queryForList("select more, ham from foo");
				return null;
			}
		});
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				template.queryForList("select spam from ham");
				return null;
			}
		});
		csds.stopCloseSuppression(connection);
		DataSourceUtils.releaseConnection(connection, csds);
		template.queryForList("select egg from bar");

		verify(rs);
		verify(stmt);
		verify(con);
		verify(ds);
	}

	@Test(expected = IllegalArgumentException.class)
	public void delegateIsRequired() throws Exception {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(null);
		tested.afterPropertiesSet();
	}

	@Test
	public void unwrapForUnsupportedInterface() throws Exception {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(new DataSourceStub());

		assertFalse(tested.isWrapperFor(Unsupported.class));

		try {
			tested.unwrap(Unsupported.class);
			fail();
		}
		catch (SQLException expected) {
//			this would be the correct behavior in a Java6-only recursive implementation
//			assertEquals(DataSourceStub.UNWRAP_ERROR_MESSAGE, expected.getMessage());
			assertEquals("Unsupported class " + Unsupported.class.getSimpleName(), expected.getMessage());
		}
	}

	@Test
	public void unwrapForSupportedInterface() throws Exception {

		DataSourceStub ds = new DataSourceStub();
		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(ds);

		assertTrue(tested.isWrapperFor(Supported.class));
		assertEquals(ds, tested.unwrap(Supported.class));
	}

	@Test
	public void unwrapForSmartDataSource() throws Exception {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(new DataSourceStub());

		assertTrue(tested.isWrapperFor(DataSource.class));
		assertEquals(tested, tested.unwrap(DataSource.class));
		
		assertTrue(tested.isWrapperFor(SmartDataSource.class));
		assertEquals(tested, tested.unwrap(SmartDataSource.class));
	}

	/**
	 * Interface implemented by the wrapped DataSource
	 */
	private static interface Supported {
	}

	/**
	 * Interface *not* implemented by the wrapped DataSource
	 */
	private static interface Unsupported {
	}

	/**
	 * Stub for a wrapped DataSource that implements additional interface. Its
	 * purpose is testing of {@link DataSource#isWrapperFor(Class)} and
	 * {@link DataSource#unwrap(Class)} methods.
	 */
	private static class DataSourceStub implements DataSource, Supported {

		private static final String UNWRAP_ERROR_MESSAGE = "supplied type is not implemented by this class";

		public Connection getConnection() throws SQLException {
			throw new UnsupportedOperationException();
		}

		public Connection getConnection(String username, String password) throws SQLException {
			throw new UnsupportedOperationException();
		}

		public PrintWriter getLogWriter() throws SQLException {
			throw new UnsupportedOperationException();
		}

		public int getLoginTimeout() throws SQLException {
			throw new UnsupportedOperationException();
		}

		public void setLogWriter(PrintWriter out) throws SQLException {
			throw new UnsupportedOperationException();
		}

		public void setLoginTimeout(int seconds) throws SQLException {
			throw new UnsupportedOperationException();
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			if (iface.equals(Supported.class) || (iface.equals(DataSource.class))) {
				return true;
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		public <T> T unwrap(Class<T> iface) throws SQLException {
			if (iface.equals(Supported.class) || iface.equals(DataSource.class)) {
				return (T) this;
			}
			throw new SQLException(UNWRAP_ERROR_MESSAGE);
		}

		/**
		 * Added due to JDK 7 compatibility, sadly a proper implementation
		 * that would throw SqlFeatureNotSupportedException is not possible
		 * in Java 5 (the class was added in Java 6).
		 */
		public Logger getParentLogger() {
			throw new UnsupportedOperationException();
		}

	}
}
