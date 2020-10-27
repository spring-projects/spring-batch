/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class FlowJobBuilderTests {

	private JobRepository jobRepository;

	private JobExecution execution;

	private StepSupport step1 = new StepSupport("step1") {
		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
		UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	private StepSupport fails = new StepSupport("fails") {
		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
		UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.FAILED);
			stepExecution.setExitStatus(ExitStatus.FAILED);
			jobRepository.update(stepExecution);
		}
	};

	private StepSupport step2 = new StepSupport("step2") {
		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
		UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	private StepSupport step3 = new StepSupport("step3") {
		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
		UnexpectedJobExecutionException {
			stepExecution.upgradeStatus(BatchStatus.COMPLETED);
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			jobRepository.update(stepExecution);
		}
	};

	@Before
	public void init() throws Exception {
		jobRepository = new MapJobRepositoryFactoryBean().getObject();
		execution = jobRepository.createJobExecution("flow", new JobParameters());
	}

	@Test
	public void testBuildOnOneLine() throws Exception {
		FlowJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1).on("COMPLETED")
				.to(step2).end().preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildSingleFlow() throws Exception {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).next(step2).build();
		FlowJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(flow).end().preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildOverTwoLines() throws Exception {
		FlowJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1).on("COMPLETED")
				.to(step2).end();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildSubflow() throws Exception {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
		JobFlowBuilder builder = new JobBuilder("flow").repository(jobRepository).start(flow);
		builder.on("COMPLETED").to(step2);
		builder.end().preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildSplit() throws Exception {
		Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step2);
		builder.split(new SimpleAsyncTaskExecutor()).add(flow).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildSplitUsingStartAndAdd_BATCH_2346() throws Exception {
		Flow subflow1 = new FlowBuilder<Flow>("subflow1").from(step2).end();
		Flow subflow2 = new FlowBuilder<Flow>("subflow2").from(step3).end();
		Flow splitflow = new FlowBuilder<Flow>("splitflow").start(subflow1).split(new SimpleAsyncTaskExecutor())
				.add(subflow2).build();

		FlowJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(splitflow).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

    @Test
    public void testBuildSplit_BATCH_2282() throws Exception {
        Flow flow1 = new FlowBuilder<Flow>("subflow1").from(step1).end();
        Flow flow2 = new FlowBuilder<Flow>("subflow2").from(step2).end();
        Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(new SimpleAsyncTaskExecutor()).add(flow1, flow2).build();
        FlowJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(splitFlow).end();
        builder.preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

	@Test
	public void testBuildDecision() throws Exception {
		JobExecutionDecider decider = new JobExecutionDecider() {
			private int count = 0;
			@Override
			public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
				count++;
				return count<2 ? new FlowExecutionStatus("ONGOING") : FlowExecutionStatus.COMPLETED;
			}
		};
		step1.setAllowStartIfComplete(true);
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1);
		builder.next(decider).on("COMPLETED").end().from(decider).on("*").to(step1).end();
		builder.preventRestart().build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildWithIntermediateSimpleJob() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1);
		builder.on("COMPLETED").to(step2).end();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildWithIntermediateSimpleJobTwoSteps() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1).next(step2);
		builder.on("FAILED").to(step3).end();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildWithCustomEndState() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1);
		builder.on("COMPLETED").end("FOO");
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals("FOO", execution.getExitStatus().getExitCode());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildWithStop() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(step1);
		builder.on("COMPLETED").stop();
		builder.preventRestart();
		builder.build().execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		assertEquals("STOPPED", execution.getExitStatus().getExitCode());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	public void testBuildWithStopAndRestart() throws Exception {
		SimpleJobBuilder builder = new JobBuilder("flow").repository(jobRepository).start(fails);
		builder.on("FAILED").stopAndRestart(step2);
		Job job = builder.build();
		job.execute(execution);
		assertEquals(BatchStatus.STOPPED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
		execution = jobRepository.createJobExecution("flow", new JobParameters());
		job.execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
		assertEquals("step2", execution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	public void testBuildWithJobScopedStep() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("chunkSize", 2L)
				.toJobParameters();

		// when
		JobExecution jobExecution = jobLauncher.run(job, jobParameters);

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@EnableBatchProcessing
	@Configuration
	static class JobConfiguration {

		@Bean
		@JobScope
		public Step step(StepBuilderFactory stepBuilderFactory,
						 @Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
			return stepBuilderFactory.get("step")
					.<Integer, Integer>chunk(chunkSize)
					.reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4)))
					.writer(items -> {})
					.build();
		}

		@Bean
		public Job job(JobBuilderFactory jobBuilderFactory) {
			return jobBuilderFactory.get("job")
					.flow(step(null, null))
					.build()
					.build();
		}
	}

}
