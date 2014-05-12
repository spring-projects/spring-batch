/*
 * Copyright 2006-2014 the original author or authors.
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

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.BatchResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link ItemWriter} that uses the batching features from
 * SqlMapClient to execute a batch of statements for all items
 * provided.<br>
 *
 * The user must provide an iBATIS statement id that points to the SQL statement defined
 * in the iBATIS SqlMap configuration.<br>
 *
 * It is expected that {@link #write(List)} is called inside a transaction.<br>
 *
 * The writer is thread-safe after its properties are set (normal singleton
 * behavior), so it can be used to write in multiple concurrent transactions.<br>
 *
 * <em>Note:</em> This reader was refactored as part of Spring Batch 3.0 to use the iBatis
 * APIs directly instead of using Spring's SqlMapClientTemplate as part of the upgrade to
 * support Spring 4.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 * @deprecated as of Spring Batch 3.0, in favor of the native Spring Batch support
 * in the MyBatis follow-up project (http://mybatis.github.io/spring/)
 */
@Deprecated
public class IbatisBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(IbatisBatchItemWriter.class);

	private String statementId;

	private boolean assertUpdates = true;

	private SqlMapClient sqlMapClient;

	private DataSource dataSource;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Public setter for the flag that determines whether an assertion is made
	 * that all items cause at least one row to be updated.
	 *
	 * @param assertUpdates the flag to set. Defaults to true;
	 */
	public void setAssertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;
	}

	/**
	 * Public setter for {@link SqlMapClient} for injection purposes.
	 *
	 * @param sqlMapClient the SqlMapClient
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClient = sqlMapClient;
	}

	/**
	 * Public setter for the statement id identifying the statement in the SqlMap
	 * configuration file.
	 *
	 * @param statementId the id for the statement
	 */
	public void setStatementId(String statementId) {
		this.statementId = statementId;
	}

	/**
	 * Check mandatory properties - there must be an SqlMapClient and a statementId.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(sqlMapClient, "A SqlMapClient is required.");
		Assert.notNull(statementId, "A statementId is required.");
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(final List<? extends T> items) {

		if (!items.isEmpty()) {

			if (logger.isDebugEnabled()) {
				logger.debug("Executing batch with " + items.size() + " items.");
			}

			List<BatchResult> results = execute(items);

			if (assertUpdates) {
				if (results.size() != 1) {
					throw new InvalidDataAccessResourceUsageException("Batch execution returned invalid results. " +
							"Expected 1 but number of BatchResult objects returned was " + results.size());
				}

				int[] updateCounts = results.get(0).getUpdateCounts();

				for (int i = 0; i < updateCounts.length; i++) {
					int value = updateCounts[i];
					if (value == 0) {
						throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
								+ " did not update any rows: [" + items.get(i) + "]", 1);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<BatchResult> execute(final List<? extends T> items) {
		// We always need to use a SqlMapSession, as we need to pass a Spring-managed
		// Connection (potentially transactional) in. This shouldn't be necessary if
		// we run against a TransactionAwareDataSourceProxy underneath, but unfortunately
		// we still need it to make iBATIS batch execution work properly: If iBATIS
		// doesn't recognize an existing transaction, it automatically executes the
		// batch for every single statement...

		SqlMapSession session = this.sqlMapClient.openSession();
		if (logger.isDebugEnabled()) {
			logger.debug("Opened SqlMapSession [" + session + "] for iBATIS operation");
		}
		Connection ibatisCon = null;

		try {
			Connection springCon = null;
			boolean transactionAware = (dataSource instanceof TransactionAwareDataSourceProxy);

			// Obtain JDBC Connection to operate on...
			try {
				ibatisCon = session.getCurrentConnection();
				if (ibatisCon == null) {
					springCon = (transactionAware ?
							dataSource.getConnection() : DataSourceUtils.doGetConnection(dataSource));
					session.setUserConnection(springCon);
					if (logger.isDebugEnabled()) {
						logger.debug("Obtained JDBC Connection [" + springCon + "] for iBATIS operation");
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Reusing JDBC Connection [" + ibatisCon + "] for iBATIS operation");
					}
				}
			}
			catch (SQLException ex) {
				throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
			}

			// Execute given callback...
			try {
				session.startBatch();
				for (T item : items) {
					session.update(statementId, item);
				}
				try {
					return session.executeBatchDetailed();
				} catch (BatchException e) {
					throw e.getBatchUpdateException();
				}
			}
			catch (SQLException ex) {
				SQLExceptionTranslator sqlStateSQLExceptionTranslator;

				if(dataSource != null) {
					sqlStateSQLExceptionTranslator = new SQLStateSQLExceptionTranslator();
				} else {
					sqlStateSQLExceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
				}

				throw sqlStateSQLExceptionTranslator.translate("SqlMapClient operation", null, ex);
			}

			// Processing finished - potentially session still to be closed.
		}
		finally {
			// Only close SqlMapSession if we know we've actually opened it
			// at the present level.
			if (ibatisCon == null) {
				session.close();
			}
		}
	}
}
