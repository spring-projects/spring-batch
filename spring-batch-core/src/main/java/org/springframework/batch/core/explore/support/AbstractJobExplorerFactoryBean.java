/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer}. Declares abstract methods for providing DAO
 * object implementations.
 * 
 * @see JobExplorerFactoryBean
 * @see MapJobExplorerFactoryBean
 * 
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractJobExplorerFactoryBean implements FactoryBean {

	/**
	 * @return fully configured {@link JobInstanceDao} implementation.
	 */
	protected abstract JobInstanceDao createJobInstanceDao() throws Exception;

	/**
	 * @return fully configured {@link JobExecutionDao} implementation.
	 */
	protected abstract JobExecutionDao createJobExecutionDao() throws Exception;
	
	protected abstract StepExecutionDao createStepExecutionDao() throws Exception;

	protected abstract ExecutionContextDao createExecutionContextDao() throws Exception;

	/**
	 * The type of object to be returned from {@link #getObject()}.
	 * 
	 * @return JobExplorer.class
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<JobExplorer> getObjectType() {
		return JobExplorer.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
