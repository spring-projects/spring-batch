package org.springframework.batch.item.database.support;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.database.DrivingQueryItemReader;
import org.springframework.batch.item.database.KeyGenerator;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.util.Assert;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * {@link KeyGenerator} based on iBATIS ORM framework. It is functionally
 * similar to {@link SingleColumnJdbcKeyGenerator} but does not make assumptions
 * about the primary key structure.
 * 
 * @author Robert Kasanicky
 * @author Lucas Ward
 * @see DrivingQueryItemReader
 */
public class IbatisKeyGenerator extends ExecutionContextUserSupport implements KeyGenerator {

	private static final String RESTART_KEY = "key.index";

	private SqlMapClientTemplate sqlMapClientTemplate;

	private String drivingQuery;

	private String restartQueryId;

	public IbatisKeyGenerator() {
		setName(IbatisKeyGenerator.class.getSimpleName());
	}

	/*
	 * Retrieve the keys using the provided driving query id.
	 * 
	 * @see org.springframework.batch.io.support.AbstractDrivingQueryItemReader#retrieveKeys()
	 */
	public List retrieveKeys(ExecutionContext executionContext) {
		if (executionContext.containsKey(getKey(RESTART_KEY))) {
			Object key = executionContext.getString(getKey(RESTART_KEY));
			return sqlMapClientTemplate.queryForList(restartQueryId, key);
		}
		else {
			return sqlMapClientTemplate.queryForList(drivingQuery);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.io.driving.KeyGenerator#getKeyAsExecutionContext(java.lang.Object)
	 */
	public void saveState(Object key, ExecutionContext executionContext) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(executionContext, "ExecutionContext must be null");
		executionContext.putString(getKey(RESTART_KEY), key.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sqlMapClientTemplate, "SqlMaperClientTemplate must not be null.");
		Assert.hasText(drivingQuery, "The DrivingQuery must not be null or empty.");
	}

	/**
	 * @param sqlMapClient configured iBATIS client
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClientTemplate = new SqlMapClientTemplate();
		this.sqlMapClientTemplate.setSqlMapClient(sqlMapClient);
	}

	/**
	 * @param drivingQueryId id of the iBATIS select statement that will be used
	 * to retrieve the list of primary keys
	 */
	public void setDrivingQueryId(String drivingQueryId) {
		this.drivingQuery = drivingQueryId;
	}

	/**
	 * Set the id of the restart query.
	 * 
	 * @param restartQueryId id of the iBatis select statement that will be used
	 * to retrieve the list of primary keys after a restart.
	 */
	public void setRestartQueryId(String restartQueryId) {
		this.restartQueryId = restartQueryId;
	}

	public final SqlMapClientTemplate getSqlMapClientTemplate() {
		return sqlMapClientTemplate;
	}

}
