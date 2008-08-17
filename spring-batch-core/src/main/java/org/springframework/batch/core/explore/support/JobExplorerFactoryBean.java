/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.core.explore.support;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using JDBC DAO implementations. Requires the user
 * to describe what kind of database they are using.
 * 
 * @author Dave Syer
 */
public class JobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private DataSource dataSource;

	private SimpleJdbcOperations jdbcTemplate;

	private String databaseType;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	/**
	 * Public setter for the {@link DataSource}.
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Sets the database type.
	 * @param dbType as specified by
	 * {@link DefaultDataFieldMaxValueIncrementerFactory}
	 */
	public void setDatabaseType(String dbType) {
		this.databaseType = dbType;
	}

	/**
	 * Sets the table prefix for all the batch meta-data tables.
	 * @param tablePrefix
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setIncrementerFactory(DataFieldMaxValueIncrementerFactory incrementerFactory) {
		this.incrementerFactory = incrementerFactory;
	}

	public void afterPropertiesSet() throws Exception {

		Assert.notNull(dataSource, "DataSource must not be null.");

		jdbcTemplate = new SimpleJdbcTemplate(dataSource);

		if (incrementerFactory == null) {
			incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
		}

		Assert.isTrue(incrementerFactory.isSupportedIncrementerType(databaseType), "'" + databaseType
				+ "' is an unsupported database type.  The supported database types are "
				+ StringUtils.arrayToCommaDelimitedString(incrementerFactory.getSupportedIncrementerTypes()));

	}

	private Object getTarget() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao());
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		JdbcJobInstanceDao dao = new JdbcJobInstanceDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setJobIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setJobExecutionIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix
				+ "JOB_EXECUTION_SEQ"));
		dao.setJobInstanceDao(createJobInstanceDao());
		dao.setStepExecutionDao(createStepExecutionDao());
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	protected StepExecutionDao createStepExecutionDao() throws Exception {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setStepExecutionIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix
				+ "STEP_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	public Object getObject() throws Exception {
		return getTarget();
	}
}
