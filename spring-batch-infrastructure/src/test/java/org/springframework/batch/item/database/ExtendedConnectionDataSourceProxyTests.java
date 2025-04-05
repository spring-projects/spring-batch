/*
 * Copyright 2009-2023 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SmartDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ExtendedConnectionDataSourceProxyTests {

	@Test
	void testOperationWithDataSourceUtils() throws SQLException {
		Connection con = mock();
		DataSource ds = mock();

		when(ds.getConnection()).thenReturn(con); // con1
		con.close();
		when(ds.getConnection()).thenReturn(con); // con2
		con.close();

		when(ds.getConnection()).thenReturn(con); // con3
		con.close(); // con3
		when(ds.getConnection()).thenReturn(con); // con4
		con.close(); // con4

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy(ds);

		Connection con1 = csds.getConnection();
		Connection con2 = csds.getConnection();
		assertNotSame(con1, con2, "shouldn't be the same connection");

		assertTrue(csds.shouldClose(con1), "should be able to close connection");
		con1.close();
		assertTrue(csds.shouldClose(con2), "should be able to close connection");
		con2.close();

		Connection con3 = csds.getConnection();
		csds.startCloseSuppression(con3);
		Connection con3_1 = csds.getConnection();
		assertSame(con3, con3_1, "should be same connection");
		assertFalse(csds.shouldClose(con3), "should not be able to close connection");
		con3_1.close(); // no mock call for this - should be suppressed
		Connection con3_2 = csds.getConnection();
		assertSame(con3, con3_2, "should be same connection");
		Connection con4 = csds.getConnection();
		assertNotSame(con3, con4, "shouldn't be same connection");
		csds.stopCloseSuppression(con3);
		assertTrue(csds.shouldClose(con3), "should be able to close connection");
		con3_1 = null;
		con3_2 = null;
		con3.close();
		assertTrue(csds.shouldClose(con4), "should be able to close connection");
		con4.close();

	}

	@Test
	void testOperationWithDirectCloseCall() throws SQLException {
		Connection con = mock();
		DataSource ds = mock();

		when(ds.getConnection()).thenReturn(con); // con1
		con.close();
		when(ds.getConnection()).thenReturn(con); // con2
		con.close();

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy(ds);

		Connection con1 = csds.getConnection();
		csds.startCloseSuppression(con1);
		Connection con1_1 = csds.getConnection();
		assertSame(con1, con1_1, "should be same connection");
		con1_1.close(); // no mock call for this - should be suppressed
		Connection con1_2 = csds.getConnection();
		assertSame(con1, con1_2, "should be same connection");
		Connection con2 = csds.getConnection();
		assertNotSame(con1, con2, "shouldn't be same connection");
		csds.stopCloseSuppression(con1);
		assertTrue(csds.shouldClose(con1), "should be able to close connection");
		con1_1 = null;
		con1_2 = null;
		con1.close();
		assertTrue(csds.shouldClose(con2), "should be able to close connection");
		con2.close();

	}

	@Test
	void testSuppressOfCloseWithJdbcTemplate() throws Exception {

		Connection con = mock();
		DataSource ds = mock();
		Statement stmt = mock();
		ResultSet rs = mock();

		// open and start suppressing close
		when(ds.getConnection()).thenReturn(con);

		// transaction 1
		when(con.getAutoCommit()).thenReturn(false);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select baz from bar")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select foo from bar")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		con.commit();

		// transaction 2
		when(con.getAutoCommit()).thenReturn(false);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select ham from foo")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		// REQUIRES_NEW transaction
		when(ds.getConnection()).thenReturn(con);
		when(con.getAutoCommit()).thenReturn(false);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select 1 from eggs")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		con.commit();
		con.close();
		// resume transaction 2
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select more, ham from foo")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		con.commit();

		// transaction 3
		when(con.getAutoCommit()).thenReturn(false);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select spam from ham")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		con.commit();

		// stop suppressing close and close
		con.close();

		// standalone query
		when(ds.getConnection()).thenReturn(con);
		when(con.createStatement()).thenReturn(stmt);
		when(stmt.executeQuery("select egg from bar")).thenReturn(rs);
		when(rs.next()).thenReturn(false);
		con.close();

		final ExtendedConnectionDataSourceProxy csds = new ExtendedConnectionDataSourceProxy();
		csds.setDataSource(ds);
		PlatformTransactionManager tm = new JdbcTransactionManager(csds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final JdbcTemplate template = new JdbcTemplate(csds);

		Connection connection = DataSourceUtils.getConnection(csds);
		csds.startCloseSuppression(connection);
		tt.execute((TransactionCallback<Void>) status -> {
			template.queryForList("select baz from bar");
			template.queryForList("select foo from bar");
			return null;
		});
		tt.execute((TransactionCallback<Void>) status -> {
			template.queryForList("select ham from foo");
			tt2.execute((TransactionCallback<Void>) status1 -> {
				template.queryForList("select 1 from eggs");
				return null;
			});
			template.queryForList("select more, ham from foo");
			return null;
		});
		tt.execute((TransactionCallback<Void>) status -> {
			template.queryForList("select spam from ham");
			return null;
		});
		csds.stopCloseSuppression(connection);
		DataSourceUtils.releaseConnection(connection, csds);
		template.queryForList("select egg from bar");

	}

	@Test
	void delegateIsRequired() {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(null);
		assertThrows(IllegalStateException.class, tested::afterPropertiesSet);
	}

	@Test
	void unwrapForUnsupportedInterface() throws Exception {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(new DataSourceStub());

		assertFalse(tested.isWrapperFor(Unsupported.class));

		Exception expected = assertThrows(SQLException.class, () -> tested.unwrap(Unsupported.class));
		assertEquals("Unsupported class " + Unsupported.class.getSimpleName(), expected.getMessage());
	}

	@Test
	void unwrapForSupportedInterface() throws Exception {

		DataSourceStub ds = new DataSourceStub();
		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(ds);

		assertTrue(tested.isWrapperFor(Supported.class));
		assertEquals(ds, tested.unwrap(Supported.class));
	}

	@Test
	void unwrapForSmartDataSource() throws Exception {

		ExtendedConnectionDataSourceProxy tested = new ExtendedConnectionDataSourceProxy(new DataSourceStub());

		assertTrue(tested.isWrapperFor(DataSource.class));
		assertEquals(tested, tested.unwrap(DataSource.class));

		assertTrue(tested.isWrapperFor(SmartDataSource.class));
		assertEquals(tested, tested.unwrap(SmartDataSource.class));
	}

	/**
	 * Interface implemented by the wrapped DataSource
	 */
	private interface Supported {

	}

	/**
	 * Interface *not* implemented by the wrapped DataSource
	 */
	private interface Unsupported {

	}

	/**
	 * Stub for a wrapped DataSource that implements additional interface. Its purpose is
	 * testing of {@link DataSource#isWrapperFor(Class)} and
	 * {@link DataSource#unwrap(Class)} methods.
	 */
	private static class DataSourceStub implements DataSource, Supported {

		private static final String UNWRAP_ERROR_MESSAGE = "supplied type is not implemented by this class";

		@Override
		public Connection getConnection() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Connection getConnection(String username, String password) {
			throw new UnsupportedOperationException();
		}

		@Override
		public PrintWriter getLogWriter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getLoginTimeout() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLogWriter(PrintWriter out) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLoginTimeout(int seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			if (iface.equals(Supported.class) || iface.equals(DataSource.class)) {
				return true;
			}
			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T unwrap(Class<T> iface) throws SQLException {
			if (iface.equals(Supported.class) || iface.equals(DataSource.class)) {
				return (T) this;
			}
			throw new SQLException(UNWRAP_ERROR_MESSAGE);
		}

		/**
		 * Added due to JDK 7.
		 */
		@Override
		@SuppressWarnings("unused")
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}

	}

}
