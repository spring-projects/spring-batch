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

package org.springframework.batch.core.repository.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository} using JDBC DAO implementations which persist
 * batch metadata in database. Requires the user to describe what kind of
 * database they are using.
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class JobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean implements InitializingBean {
	
	protected static final Log logger = LogFactory.getLog(JobRepositoryFactoryBean.class);
	
	/**
	 * Default value for isolation level in create* method.
	 */
	private static final String DEFAULT_ISOLATION_LEVEL = "ISOLATION_SERIALIZABLE";

	private ProxyFactory proxyFactory;

	private String isolationLevelForCreate = DEFAULT_ISOLATION_LEVEL;

	private DataSource dataSource;

	private SimpleJdbcOperations jdbcTemplate;

	private String databaseType;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	private DataFieldMaxValueIncrementerFactory incrementerFactory;
	
	private PlatformTransactionManager transactionManager;

	/**
	 * Public setter for the isolation level to be used for the transaction when
	 * job execution entities are initially created. The default is
	 * ISOLATION_SERIALIZABLE, which prevents accidental concurrent execution of
	 * the same job (ISOLATION_REPEATABLE_READ would work as well).
	 * 
	 * @param isolationLevelForCreate the isolation level name to set
	 * 
	 * @see SimpleJobRepository#createJobExecution(String,
	 * org.springframework.batch.core.JobParameters)
	 */
	public void setIsolationLevelForCreate(String isolationLevelForCreate) {
		this.isolationLevelForCreate = isolationLevelForCreate;
	}
	
	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	/**
	 * Public setter for the {@link DataSource}.
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Sets the database type.
	 * @param dbType as specified by {@link DefaultDataFieldMaxValueIncrementerFactory}
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

		Assert.notNull(transactionManager, "TransactionManager must not be null.");
		Assert.notNull(dataSource, "DataSource must not be null.");
		
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);

		if (incrementerFactory == null) {
			incrementerFactory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
		}
		
		if(databaseType == null){
			logger.info("No database type set, using meta data");
			databaseType = DatabaseType.fromMetaData(dataSource).name();
		}

		Assert.isTrue(incrementerFactory.isSupportedIncrementerType(databaseType), "'" + databaseType
				+ "' is an unsupported database type.  The supported database types are "
				+ StringUtils.arrayToCommaDelimitedString(incrementerFactory.getSupportedIncrementerTypes()));
		
		initializeProxy();

	}
	
	protected void initializeProxy() throws Exception {
		proxyFactory = new ProxyFactory();
		TransactionInterceptor advice = new TransactionInterceptor(transactionManager, PropertiesConverter
				.stringToProperties("create*=PROPAGATION_REQUIRES_NEW," + isolationLevelForCreate
						+ "\n*=PROPAGATION_REQUIRED"));
		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(advice);
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.addMethodName("*");
		advisor.setPointcut(pointcut);
		proxyFactory.addAdvisor(advisor);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.addInterface(JobRepository.class);
		proxyFactory.setTarget(getTarget());
	}
	
	private Object getTarget() throws Exception {
		return new SimpleJobRepository(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(), createExecutionContextDao());
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
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		JdbcStepExecutionDao dao = new JdbcStepExecutionDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setStepExecutionIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix
				+ "STEP_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	public Object getObject() throws Exception {
		return proxyFactory.getProxy();
	}
}
