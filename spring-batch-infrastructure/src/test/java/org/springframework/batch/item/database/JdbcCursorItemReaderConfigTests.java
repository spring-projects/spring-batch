/*
 * Copyright 2008-2023 the original author or authors.
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
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCursorItemReaderConfigTests {

	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	void testUsesCurrentTransaction() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(con.getAutoCommit()).thenReturn(false);
		PreparedStatement ps = mock(PreparedStatement.class);
		when(con.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT))
			.thenReturn(ps);
		when(ds.getConnection()).thenReturn(con);
		when(ds.getConnection()).thenReturn(con);
		con.commit();
		PlatformTransactionManager tm = new JdbcTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
		reader.setDataSource(new ExtendedConnectionDataSourceProxy(ds));
		reader.setUseSharedExtendedConnection(true);
		reader.setSql("select foo from bar");
		final ExecutionContext ec = new ExecutionContext();
		tt.execute((TransactionCallback<Void>) status -> {
			reader.open(ec);
			reader.close();
			return null;
		});
	}

	/*
	 * Should fail if trying to call getConnection() twice
	 */
	@Test
	void testUsesItsOwnTransaction() throws Exception {

		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(con.getAutoCommit()).thenReturn(false);
		PreparedStatement ps = mock(PreparedStatement.class);
		when(con.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
			.thenReturn(ps);
		when(ds.getConnection()).thenReturn(con);
		when(ds.getConnection()).thenReturn(con);
		con.commit();
		PlatformTransactionManager tm = new JdbcTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
		reader.setDataSource(ds);
		reader.setSql("select foo from bar");
		final ExecutionContext ec = new ExecutionContext();
		tt.execute((TransactionCallback<Void>) status -> {
			reader.open(ec);
			reader.close();
			return null;
		});
	}

	@Test
	void testOverrideConnectionAutoCommit() throws Exception {
		boolean initialAutoCommit = false;
		boolean neededAutoCommit = true;

		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		when(con.getAutoCommit()).thenReturn(initialAutoCommit);
		PreparedStatement ps = mock(PreparedStatement.class);
		when(con.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
			.thenReturn(ps);
		when(ds.getConnection()).thenReturn(con);

		final JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
		reader.setDataSource(ds);
		reader.setSql("select foo from bar");
		reader.setConnectionAutoCommit(neededAutoCommit);

		// Check "open" outside of a transaction (see AbstractStep#execute())
		final ExecutionContext ec = new ExecutionContext();
		reader.open(ec);

		verify(con).setAutoCommit(eq(neededAutoCommit));

		reset(con);
		reader.close();

		// Check restored autocommit value
		verify(con).setAutoCommit(eq(initialAutoCommit));
	}

}
