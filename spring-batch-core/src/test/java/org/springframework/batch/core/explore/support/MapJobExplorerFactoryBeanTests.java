/*
 * Copyright 2010-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;

import java.util.Date;

/**
 * Tests for {@link MapJobExplorerFactoryBean}.
 */
public class MapJobExplorerFactoryBeanTests {

	/**
	 * Use the factory to create repository and check the explorer remembers
	 * created executions.
	 */
	@Test
	public void testCreateExplorer() throws Exception {

		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		JobRepository jobRepository = repositoryFactory.getObject();
		JobExecution jobExecution = jobRepository.createJobExecution("foo", new JobParameters());

		//simulating a running job execution
		jobExecution.setStartTime(new Date());
		jobRepository.update(jobExecution);

		MapJobExplorerFactoryBean tested = new MapJobExplorerFactoryBean(repositoryFactory);
		tested.afterPropertiesSet();

		JobExplorer explorer = tested.getObject();

		assertEquals(1, explorer.findRunningJobExecutions("foo").size());

	}

}
