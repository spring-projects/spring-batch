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

package org.springframework.batch.execution.repository;

import javax.sql.DataSource;

import org.springframework.batch.execution.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.execution.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.execution.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.execution.repository.dao.JobExecutionDao;
import org.springframework.batch.execution.repository.dao.JobInstanceDao;
import org.springframework.batch.execution.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DB2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobRepository}.  Requires the user
 * to describe what kind of database they are using.  Valid values are:
 * 
 * <ul>
 * <li>db2</li>
 * <li>derby</li>
 * <li>hsql</li>
 * <li>mysql</li>
 * <li>oracle</li>
 * <li>postgres</li>
 * </ul>
 * 
 * @author Ben Hale
 */
public class SimpleJobRepositoryFactoryBean implements FactoryBean, InitializingBean {

	private static final String DB_TYPE_DB2 = "db2";

	private static final String DB_TYPE_DERBY = "derby";

	private static final String DB_TYPE_HSQL = "hsql";

	private static final String DB_TYPE_MYSQL = "mysql";

	private static final String DB_TYPE_ORACLE = "oracle";

	private static final String DB_TYPE_POSTGRES = "postgres";

	private DataSource dataSource;

	private String databaseType;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setDatabaseType(String dbType) {
		this.databaseType = dbType;
	}

	public void afterPropertiesSet() throws Exception {
		if (!DB_TYPE_DB2.equals(databaseType) && !DB_TYPE_DERBY.equals(databaseType)
		        && !DB_TYPE_HSQL.equals(databaseType) && !DB_TYPE_MYSQL.equals(databaseType)
		        && !DB_TYPE_ORACLE.equals(databaseType) && !DB_TYPE_POSTGRES.equals(databaseType)) {
			throw new BeanCreationException(
			        "Database type must be one of: 'db2', 'derby', 'hsql', 'mysql', 'oracle', 'postgres'");
		}
	}

	public Object getObject() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		JobInstanceDao jobInstanceDao = createJobInstanceDao(jdbcTemplate);
		JobExecutionDao jobExecutionDao = createJobExecutionDao(jdbcTemplate);
		StepExecutionDao stepExecutionDao = createStepExecutionDao(jdbcTemplate);
		return new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao);
	}

	public Class getObjectType() {
		return SimpleJobRepository.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private JobInstanceDao createJobInstanceDao(JdbcTemplate jdbcTemplate) throws Exception {
		JdbcJobInstanceDao dao = new JdbcJobInstanceDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setJobIncrementer(getIncrementer(dataSource, "BATCH_JOB_SEQ"));
		dao.afterPropertiesSet();
		return dao;
	}

	private JobExecutionDao createJobExecutionDao(JdbcTemplate jdbcTemplate) throws Exception {
		JdbcJobExecutionDao dao = new JdbcJobExecutionDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setJobExecutionIncrementer(getIncrementer(dataSource, "BATCH_JOB_EXECUTION_SEQ"));
		dao.afterPropertiesSet();
		return dao;
	}

	private StepExecutionDao createStepExecutionDao(JdbcTemplate jdbcTemplate) throws Exception {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setStepExecutionIncrementer(getIncrementer(dataSource, "BATCH_STEP_EXECUTION_SEQ"));
		dao.afterPropertiesSet();
		return dao;
	}

	private DataFieldMaxValueIncrementer getIncrementer(DataSource dataSource, String incrementerName) {
		if (DB_TYPE_DB2.equals(databaseType)) {
			return new DB2SequenceMaxValueIncrementer(dataSource, incrementerName);
		} else if (DB_TYPE_DERBY.equals(databaseType)) {
			return new DerbyMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_HSQL.equals(databaseType)) {
			return new HsqlMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_MYSQL.equals(databaseType)) {
			return new MySQLMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_ORACLE.equals(databaseType)) {
			return new OracleSequenceMaxValueIncrementer(dataSource, incrementerName);
		} else if (DB_TYPE_POSTGRES.equals(databaseType)) {
			return new PostgreSQLSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		throw new IllegalArgumentException("databaseType argument was not on the approved list");
	}

}
