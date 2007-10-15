package org.springframework.batch.io.orm.ibatis;

import java.util.List;
import java.util.Properties;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.support.AbstractDrivingQueryInputSource;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.util.Assert;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Driving query {@link InputSource} based on iBATIS ORM framework. It is functionally similar to
 * {@link SingleKeySqlDrivingQueryInputSource} but does not make assumptions about the primary key
 * structure.
 *
 * @see SingleKeySqlDrivingQueryInputSource
 *
 * @author Robert Kasanicky
 * @author Lucas Ward
 */
public class IbatisDrivingQueryInputSource extends AbstractDrivingQueryInputSource implements Restartable {

	public static final String RESTART_KEY = "IbatisDrivingQueryInputSource.keyIndex";

	private SqlMapClientTemplate sqlMapClientTemplate;

	private String drivingQuery;

	private String restartQueryId;

	protected List retrieveKeys() {
		return sqlMapClientTemplate.queryForList(drivingQuery);
	}


	public RestartData getRestartData() {
		Properties props = new Properties();
		props.setProperty(RESTART_KEY, getCurrentKey().toString());

		return new GenericRestartData(props);
	}

	/**
	 * Restore the keys list given the provided restart data.
	 *
	 * @see org.springframework.batch.io.support.AbstractDrivingQueryInputSource#restoreKeys(org.springframework.batch.restart.RestartData)
	 */
	public List restoreKeys(RestartData data) {

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
