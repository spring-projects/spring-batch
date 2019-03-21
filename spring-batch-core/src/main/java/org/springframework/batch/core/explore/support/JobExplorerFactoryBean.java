/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.core.explore.support;

import javax.sql.DataSource;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using JDBC DAO implementations. Requires the user
 * to describe what kind of database they are using.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class JobExplorerFactoryBean extends AbstractJobExplorerFactoryBean
implements InitializingBean {

	private DataSource dataSource;

	private JdbcOperations jdbcOperations;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	private DataFieldMaxValueIncrementer incrementer = new AbstractDataFieldMaxValueIncrementer() {
		@Override
		protected long getNextKey() {
			throw new IllegalStateException("JobExplorer is read only.");
		}
	};

	private LobHandler lobHandler;

	private ExecutionContextSerializer serializer;

	/**
	 * A custom implementation of the {@link ExecutionContextSerializer}.
	 * The default, if not injected, is the {@link Jackson2ExecutionContextStringSerializer}.
	 *
	 * @param serializer used to serialize/deserialize an {@link org.springframework.batch.item.ExecutionContext}
	 * @see ExecutionContextSerializer
	 */
	public void setSerializer(ExecutionContextSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Public setter for the {@link DataSource}.
	 *
	 * @param dataSource
	 *            a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	/**
	 * Public setter for the {@link JdbcOperations}. If this property is not set explicitly,
	 * a new {@link JdbcTemplate} will be created for the configured DataSource by default.
	 * @param jdbcOperations a {@link JdbcOperations}
	 */
	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}	

	/**
	 * Sets the table prefix for all the batch meta-data tables.
	 *
	 * @param tablePrefix prefix for the batch meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * The lob handler to use when saving {@link ExecutionContext} instances.
	 * Defaults to null which works for most databases.
	 *
	 * @param lobHandler Large object handler for saving {@link org.springframework.batch.item.ExecutionContext}
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(dataSource, "DataSource must not be null.");

		if (jdbcOperations == null) {
			jdbcOperations = new JdbcTemplate(dataSource);
		}	

		if(serializer == null) {
			serializer = new Jackson2ExecutionContextStringSerializer();
		}
	}

	private JobExplorer getTarget() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(),
				createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setLobHandler(lobHandler);
		dao.setTablePrefix(tablePrefix);
		dao.setSerializer(serializer);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		JdbcJobInstanceDao dao = new JdbcJobInstanceDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobIncrementer(incrementer);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setJobExecutionIncrementer(incrementer);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcOperations);
		dao.setStepExecutionIncrementer(incrementer);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	public JobExplorer getObject() throws Exception {
		return getTarget();
	}
}
