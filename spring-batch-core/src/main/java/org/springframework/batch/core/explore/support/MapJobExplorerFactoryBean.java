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

import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using in-memory DAO implementations.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class MapJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean {

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return new MapJobExecutionDao();
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return new MapJobInstanceDao();
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		return new MapStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		return new MapExecutionContextDao();
	}

	public Object getObject() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}

}
