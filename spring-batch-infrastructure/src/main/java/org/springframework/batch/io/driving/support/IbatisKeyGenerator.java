package org.springframework.batch.io.driving.support;

import java.util.List;
import java.util.Properties;

import org.springframework.batch.io.driving.DrivingQueryItemReader;
import org.springframework.batch.io.driving.KeyGenerator;
import org.springframework.batch.stream.GenericStreamContext;
import org.springframework.batch.stream.StreamContext;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.util.Assert;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * {@link KeyGenerator} based on iBATIS ORM framework. It is functionally similar to
 * {@link SingleColumnJdbcKeyGenerator} but does not make assumptions about the primary key
 * structure.
 *
 * @author Robert Kasanicky
 * @author Lucas Ward
 * @see DrivingQueryItemReader
 */
public class IbatisKeyGenerator implements KeyGenerator {

	public static final String RESTART_KEY = "IbatisDrivingQueryItemReader.keyIndex";

	private SqlMapClientTemplate sqlMapClientTemplate;

	private String drivingQuery;

	private String restartQueryId;

	/*
	 * Retrieve the keys using the provided driving query id.
	 *
	 * @see org.springframework.batch.io.support.AbstractDrivingQueryItemReader#retrieveKeys()
	 */
	public List retrieveKeys() {
		return sqlMapClientTemplate.queryForList(drivingQuery);
	}

	/*
	 *
	 * @see org.springframework.batch.restart.Restartable#getRestartData()
	 */
	public StreamContext getKeyAsRestartData(Object key) {
		Properties props = new Properties();
		props.setProperty(RESTART_KEY, key.toString());

		return new GenericStreamContext(props);
	}

	/**
	 * Restore the keys list given the provided restart data.
	 *
	 * @see org.springframework.batch.io.driving.DrivingQueryItemReader#restoreKeys(org.springframework.batch.stream.StreamContext)
	 */
	public List restoreKeys(StreamContext data) {

		Properties props = data.getProperties();
		Object key = props.getProperty(RESTART_KEY);
		return sqlMapClientTemplate.queryForList(restartQueryId, key);
	}

	/* (non-Javadoc)
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
