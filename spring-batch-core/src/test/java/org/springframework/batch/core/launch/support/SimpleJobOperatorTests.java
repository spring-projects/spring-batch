/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class SimpleJobOperatorTests {

	private SimpleJobOperator jobOperator;

	protected Job job;

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private JobParameters jobParameters;

	@BeforeEach
	void setUp() throws Exception {

		job = new JobSupport("foo") {
			@Nullable
			@Override
			public JobParametersIncrementer getJobParametersIncrementer() {
				return parameters -> jobParameters;
			}
		};

		jobOperator = new SimpleJobOperator();

		jobOperator.setJobRegistry(new MapJobRegistry() {
			@Override
			public Job getJob(@Nullable String name) throws NoSuchJobException {
				if (name.equals("foo")) {
					return job;
				}
				throw new NoSuchJobException("foo");
			}

			@Override
			public Set<String> getJobNames() {
				return new HashSet<>(Arrays.asList(new String[] { "foo", "bar" }));
			}
		});

		jobOperator.setJobLauncher(
				(job, jobParameters) -> new JobExecution(new JobInstance(123L, job.getName()), 999L, jobParameters));

		jobExplorer = mock(JobExplorer.class);

		jobOperator.setJobExplorer(jobExplorer);

		jobRepository = mock(JobRepository.class);
		jobOperator.setJobRepository(jobRepository);

		jobOperator.setJobParametersConverter(new DefaultJobParametersConverter() {
			@Override
			public JobParameters getJobParameters(@Nullable Properties props) {
				assertTrue(props.containsKey("a"), "Wrong properties");
				return jobParameters;
			}

			@Override
			public Properties getProperties(@Nullable JobParameters params) {
				return PropertiesConverter.stringToProperties("a=b");
			}
		});

		jobOperator.afterPropertiesSet();

	}

	@Test
	void testMandatoryProperties() {
		jobOperator = new SimpleJobOperator();
		assertThrows(IllegalArgumentException.class, jobOperator::afterPropertiesSet);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleJobOperator#startNextInstance(java.lang.String)}
	 * .
	 */
	@Test
	void testStartNextInstanceSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		JobInstance jobInstance = new JobInstance(321L, "foo");
		when(jobExplorer.getJobInstances("foo", 0, 1)).thenReturn(Collections.singletonList(jobInstance));
		when(jobExplorer.getJobExecutions(jobInstance))
				.thenReturn(Collections.singletonList(new JobExecution(jobInstance, new JobParameters())));
		Long value = jobOperator.startNextInstance("foo");
		assertEquals(999, value.longValue());
	}

	@Test
	void testStartNewInstanceSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		jobRepository.isJobInstanceExists("foo", jobParameters);
		Long value = jobOperator.start("foo", "a=b");
		assertEquals(999, value.longValue());
	}

	@Test
	void testStartNewInstanceAlreadyExists() {
		jobParameters = new JobParameters();
		when(jobRepository.isJobInstanceExists("foo", jobParameters)).thenReturn(true);
		jobRepository.isJobInstanceExists("foo", jobParameters);
		assertThrows(JobInstanceAlreadyExistsException.class, () -> jobOperator.start("foo", "a=b"));
	}

	@Test
	void testResumeSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		when(jobExplorer.getJobExecution(111L))
				.thenReturn(new JobExecution(new JobInstance(123L, job.getName()), 111L, jobParameters));
		jobExplorer.getJobExecution(111L);
		Long value = jobOperator.restart(111L);
		assertEquals(999, value.longValue());
	}

	@Test
	void testGetSummarySunnyDay() throws Exception {
		jobParameters = new JobParameters();
		JobExecution jobExecution = new JobExecution(new JobInstance(123L, job.getName()), 111L, jobParameters);
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);
		jobExplorer.getJobExecution(111L);
		String value = jobOperator.getSummary(111L);
		assertEquals(jobExecution.toString(), value);
	}

	@Test
	void testGetSummaryNoSuchExecution() {
		jobParameters = new JobParameters();
		jobExplorer.getJobExecution(111L);
		assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getSummary(111L));
	}

	@Test
	void testGetStepExecutionSummariesSunnyDay() throws Exception {
		jobParameters = new JobParameters();

		JobExecution jobExecution = new JobExecution(new JobInstance(123L, job.getName()), 111L, jobParameters);
		jobExecution.createStepExecution("step1");
		jobExecution.createStepExecution("step2");
		jobExecution.getStepExecutions().iterator().next().setId(21L);
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);
		Map<Long, String> value = jobOperator.getStepExecutionSummaries(111L);
		assertEquals(2, value.size());
	}

	@Test
	void testGetStepExecutionSummariesNoSuchExecution() {
		jobParameters = new JobParameters();
		jobExplorer.getJobExecution(111L);
		assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getStepExecutionSummaries(111L));
	}

	@Test
	void testFindRunningExecutionsSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		JobExecution jobExecution = new JobExecution(new JobInstance(123L, job.getName()), 111L, jobParameters);
		when(jobExplorer.findRunningJobExecutions("foo")).thenReturn(Collections.singleton(jobExecution));
		Set<Long> value = jobOperator.getRunningExecutions("foo");
		assertEquals(111L, value.iterator().next().longValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testFindRunningExecutionsNoSuchJob() {
		jobParameters = new JobParameters();
		when(jobExplorer.findRunningJobExecutions("no-such-job")).thenReturn(Collections.EMPTY_SET);
		assertThrows(NoSuchJobException.class, () -> jobOperator.getRunningExecutions("no-such-job"));
	}

	@Test
	void testGetJobParametersSunnyDay() throws Exception {
		final JobParameters jobParameters = new JobParameters();
		when(jobExplorer.getJobExecution(111L))
				.thenReturn(new JobExecution(new JobInstance(123L, job.getName()), 111L, jobParameters));
		String value = jobOperator.getParameters(111L);
		assertEquals("a=b", value);
	}

	@Test
	void testGetJobParametersNoSuchExecution() {
		jobExplorer.getJobExecution(111L);
		assertThrows(NoSuchJobExecutionException.class, () -> jobOperator.getParameters(111L));
	}

	@Test
	void testGetLastInstancesSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		when(jobExplorer.getJobInstances("foo", 0, 2)).thenReturn(Collections.singletonList(jobInstance));
		jobExplorer.getJobInstances("foo", 0, 2);
		List<Long> value = jobOperator.getJobInstances("foo", 0, 2);
		assertEquals(123L, value.get(0).longValue());
	}

	@Test
	void testGetLastInstancesNoSuchJob() {
		jobParameters = new JobParameters();
		jobExplorer.getJobInstances("no-such-job", 0, 2);
		assertThrows(NoSuchJobException.class, () -> jobOperator.getJobInstances("no-such-job", 0, 2));
	}

	@Test
	void testGetJobNames() {
		Set<String> names = jobOperator.getJobNames();
		assertEquals(2, names.size());
		assertTrue(names.contains("foo"), "Wrong names: " + names);
	}

	@Test
	void testGetExecutionsSunnyDay() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		when(jobExplorer.getJobInstance(123L)).thenReturn(jobInstance);

		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(Collections.singletonList(jobExecution));
		List<Long> value = jobOperator.getExecutions(123L);
		assertEquals(111L, value.iterator().next().longValue());
	}

	@Test
	void testGetExecutionsNoSuchInstance() {
		jobExplorer.getJobInstance(123L);
		assertThrows(NoSuchJobInstanceException.class, () -> jobOperator.getExecutions(123L));
	}

	@Test
	void testStop() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);
		jobExplorer.getJobExecution(111L);
		jobRepository.update(jobExecution);
		jobOperator.stop(111L);
		assertEquals(BatchStatus.STOPPING, jobExecution.getStatus());
	}

	@Test
	void testStopTasklet() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		StoppableTasklet tasklet = mock(StoppableTasklet.class);
		TaskletStep taskletStep = new TaskletStep();
		taskletStep.setTasklet(tasklet);
		MockJob job = new MockJob();
		job.taskletStep = taskletStep;

		JobRegistry jobRegistry = mock(JobRegistry.class);
		TaskletStep step = mock(TaskletStep.class);

		when(step.getTasklet()).thenReturn(tasklet);
		when(step.getName()).thenReturn("test_job.step1");
		when(jobRegistry.getJob(any(String.class))).thenReturn(job);
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);

		jobOperator.setJobRegistry(jobRegistry);
		jobExplorer.getJobExecution(111L);
		jobRepository.update(jobExecution);
		jobOperator.stop(111L);
		assertEquals(BatchStatus.STOPPING, jobExecution.getStatus());
	}

	@Test
	void testStopTaskletWhenJobNotRegistered() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		StoppableTasklet tasklet = mock(StoppableTasklet.class);
		JobRegistry jobRegistry = mock(JobRegistry.class);
		TaskletStep step = mock(TaskletStep.class);

		when(step.getTasklet()).thenReturn(tasklet);
		when(jobRegistry.getJob(job.getName())).thenThrow(new NoSuchJobException("Unable to find job"));
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);

		jobOperator.setJobRegistry(jobRegistry);
		jobOperator.stop(111L);
		assertEquals(BatchStatus.STOPPING, jobExecution.getStatus());
		verify(tasklet, never()).stop();
	}

	@Test
	void testStopTaskletException() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		StoppableTasklet tasklet = new StoppableTasklet() {

			@Nullable
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				return null;
			}

			@Override
			public void stop() {
				throw new IllegalStateException();
			}
		};
		TaskletStep taskletStep = new TaskletStep();
		taskletStep.setTasklet(tasklet);
		MockJob job = new MockJob();
		job.taskletStep = taskletStep;

		JobRegistry jobRegistry = mock(JobRegistry.class);
		TaskletStep step = mock(TaskletStep.class);

		when(step.getTasklet()).thenReturn(tasklet);
		when(step.getName()).thenReturn("test_job.step1");
		when(jobRegistry.getJob(any(String.class))).thenReturn(job);
		when(jobExplorer.getJobExecution(111L)).thenReturn(jobExecution);

		jobOperator.setJobRegistry(jobRegistry);
		jobExplorer.getJobExecution(111L);
		jobRepository.update(jobExecution);
		jobOperator.stop(111L);
		assertEquals(BatchStatus.STOPPING, jobExecution.getStatus());
	}

	@Test
	void testAbort() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		jobExecution.setStatus(BatchStatus.STOPPING);
		when(jobExplorer.getJobExecution(123L)).thenReturn(jobExecution);
		jobRepository.update(jobExecution);
		jobOperator.abandon(123L);
		assertEquals(BatchStatus.ABANDONED, jobExecution.getStatus());
		assertNotNull(jobExecution.getEndTime());
	}

	@Test
	void testAbortNonStopping() {
		JobInstance jobInstance = new JobInstance(123L, job.getName());
		JobExecution jobExecution = new JobExecution(jobInstance, 111L, jobParameters);
		jobExecution.setStatus(BatchStatus.STARTED);
		when(jobExplorer.getJobExecution(123L)).thenReturn(jobExecution);
		jobRepository.update(jobExecution);
		assertThrows(JobExecutionAlreadyRunningException.class, () -> jobOperator.abandon(123L));
	}

	static class MockJob extends AbstractJob {

		private TaskletStep taskletStep;

		@Override
		public Step getStep(String stepName) {
			return taskletStep;
		}

		@Override
		public Collection<String> getStepNames() {
			return Collections.singletonList("test_job.step1");
		}

		@Override
		protected void doExecute(JobExecution execution) throws JobExecutionException {

		}

	}

}
