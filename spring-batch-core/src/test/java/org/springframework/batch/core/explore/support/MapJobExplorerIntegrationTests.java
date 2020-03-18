/*
 * Copyright 2006-2019 the original author or authors.
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

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 *
 */
public class MapJobExplorerIntegrationTests {

	private boolean block = true;

	@Test
	public void testRunningJobExecution() throws Exception {

		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		repositoryFactory.afterPropertiesSet();
		JobRepository jobRepository = repositoryFactory.getObject();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
		jobLauncher.afterPropertiesSet();

		SimpleJob job = new SimpleJob("job");
		TaskletStep step = new TaskletStep("step");
		step.setTasklet(new Tasklet() {
			@Nullable
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				while (block) {
					Thread.sleep(100L);
				}
				return RepeatStatus.FINISHED;
			}
		});
		step.setTransactionManager(repositoryFactory.getTransactionManager());
		step.setJobRepository(jobRepository);
		step.afterPropertiesSet();
		job.addStep(step);
		job.setJobRepository(jobRepository);
		job.afterPropertiesSet();

		jobLauncher.run(job, new JobParametersBuilder().addString("test", getClass().getName()).toJobParameters());

		Thread.sleep(500L);
		JobExplorer explorer = new MapJobExplorerFactoryBean(repositoryFactory).getObject();
		Set<JobExecution> executions = explorer.findRunningJobExecutions("job");
		assertEquals(1, executions.size());
		assertEquals(1, executions.iterator().next().getStepExecutions().size());

		block = false;

	}
}
