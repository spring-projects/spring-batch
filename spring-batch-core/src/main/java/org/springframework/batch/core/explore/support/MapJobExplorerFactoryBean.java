/*
 * Copyright 2006-2020 the original author or authors.
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

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using in-memory DAO implementations.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 * 
 * @deprecated as of v4.3 in favor of using the {@link JobExplorerFactoryBean}
 * with an in-memory database. Scheduled for removal in v5.0.
 */
@Deprecated
public class MapJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private MapJobRepositoryFactoryBean repositoryFactory;

	/**
	 * Create an instance with the provided {@link MapJobRepositoryFactoryBean}
	 * as a source of Dao instances.
	 * @param repositoryFactory provides the used {@link org.springframework.batch.core.repository.JobRepository}
	 */
	public MapJobExplorerFactoryBean(MapJobRepositoryFactoryBean repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	/**
	 * Create a factory with no {@link MapJobRepositoryFactoryBean}. It must be
	 * injected as a property.
	 */
	public MapJobExplorerFactoryBean() {
	}

	/**
	 * The repository factory that can be used to create daos for the explorer.
	 *
	 * @param repositoryFactory a {@link MapJobExplorerFactoryBean}
	 */
	public void setRepositoryFactory(MapJobRepositoryFactoryBean repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	/**
	 * @throws Exception thrown if error occurs.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(repositoryFactory != null, "A MapJobRepositoryFactoryBean must be provided");
		repositoryFactory.afterPropertiesSet();
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return repositoryFactory.getJobExecutionDao();
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return repositoryFactory.getJobInstanceDao();
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		return repositoryFactory.getStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		return repositoryFactory.getExecutionContextDao();
	}

	@Override
	public JobExplorer getObject() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

}
