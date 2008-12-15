package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.easymock.MockControl;
import junit.framework.TestCase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JdbcCursorItemReaderConfigTests extends TestCase {

    /*
     * Should fail if trying to call getConnection() twice
     */
    public void testUsesCurrentTransaction() throws Exception {
        //TODO:
    }

	/*
	 * Should not fail if trying to call getConnection() twice
	 */
	public void testUsesItsOwnTransaction() throws Exception {
		MockControl ctrlDataSource;
		DataSource mockDataSource;
		MockControl ctrlConnection;
		Connection mockConnection;
		MockControl ctrlPreparedStatement;
		PreparedStatement mockPreparedStatement;
		MockControl ctrlResultSet;
		ResultSet mockResultSet;

		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();

		ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		mockPreparedStatement.getWarnings();
		ctrlPreparedStatement.setDefaultReturnValue(null);

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(null);
		mockConnection.getAutoCommit();
		ctrlConnection.setDefaultReturnValue(false);
		mockConnection.prepareStatement("select foo from bar", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.HOLD_CURSORS_OVER_COMMIT);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.commit();
		ctrlConnection.setDefaultVoidCallable();
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setReturnValue(mockConnection);
        mockDataSource.getConnection();
        ctrlDataSource.setReturnValue(mockConnection);

		ctrlResultSet.replay();
		ctrlDataSource.replay();
		ctrlConnection.replay();
		ctrlPreparedStatement.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(mockDataSource);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final JdbcCursorItemReader reader = new JdbcCursorItemReader();
		reader.setDataSource(mockDataSource);
		reader.setSql("select foo from bar");
		final ExecutionContext ec = new ExecutionContext();
		tt.execute(
				new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						reader.open(ec);
						reader.close(ec);
						return null;
					}
				});

		 ctrlDataSource.verify();
	}

}
