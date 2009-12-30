/*
 * Copyright 2006-2008 the original author or authors.
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

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.util.Assert;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.engine.execution.BatchException;
import com.ibatis.sqlmap.engine.execution.BatchResult;

/**
 * {@link ItemWriter} that uses the batching features from 
 * {@link SqlMapClientTemplate} to execute a batch of statements for all items 
 * provided.<br/>
 * 
 * The user must provide an iBATIS statement id that points to the SQL statement defined
 * in the iBATIS SqlMap configuration.<br/>
 * 
 * It is expected that {@link #write(List)} is called inside a transaction.<br/>
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behavior), so it can be used to write in multiple concurrent transactions.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class IbatisBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(IbatisBatchItemWriter.class);

	private SqlMapClientTemplate sqlMapClientTemplate;

	private String statementId;

	private boolean assertUpdates = true;

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
		if (sqlMapClientTemplate == null) {
			this.sqlMapClientTemplate = new SqlMapClientTemplate(sqlMapClient);
		}
	}

	/**
	 * Public setter for the {@link SqlMapClientTemplate}.
	 * 
	 * @param sqlMapClientTemplate the SqlMapClientTemplate
	 */
	public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate) {
		this.sqlMapClientTemplate = sqlMapClientTemplate;
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
	public void afterPropertiesSet() {
		Assert.notNull(sqlMapClientTemplate, "A SqlMapClient or a SqlMapClientTemplate is required.");
		Assert.notNull(statementId, "A statementId is required.");
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(final List<? extends T> items) {

		if (!items.isEmpty()) {

			if (logger.isDebugEnabled()) {
				logger.debug("Executing batch with " + items.size() + " items.");
			}
			
			@SuppressWarnings("unchecked")
			List<BatchResult> results = (List<BatchResult>) sqlMapClientTemplate.execute(
					new SqlMapClientCallback() {
						public Object doInSqlMapClient(SqlMapExecutor executor)
								throws SQLException {
							executor.startBatch();
							for (T item : items) {
								executor.update(statementId, item);
							}
							try {
								return executor.executeBatchDetailed();
							} catch (BatchException e) {
								throw e.getBatchUpdateException();
							}
						}
					});
			
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

}
