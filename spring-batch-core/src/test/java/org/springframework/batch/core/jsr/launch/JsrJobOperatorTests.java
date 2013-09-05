package org.springframework.batch.core.jsr.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverterSupport;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.jsr.JsrJobParametersConverter;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.JobRepositorySupport;

public class JsrJobOperatorTests {

	private JobOperator jsrJobOperator;
	@Mock
	private org.springframework.batch.core.launch.JobOperator jobOperator;
	@Mock
	private JobExplorer jobExplorer;
	@Mock
	private JobRepository jobRepository;
	private JobParametersConverter parameterConverter;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		parameterConverter = new JobParametersConverterSupport();
		jsrJobOperator = new JsrJobOperator(jobExplorer, jobRepository, jobOperator, parameterConverter);
	}

	@Test
	public void testLoadingWithBatchRuntime() {
		jsrJobOperator = BatchRuntime.getJobOperator();
		assertNotNull(jsrJobOperator);
	}

	@Test
	public void testNullsInConstructor() {
		try {
			new JsrJobOperator(null, new JobRepositorySupport(), new SimpleJobOperator(), parameterConverter);
			fail("JobExplorer should be required");
		} catch (IllegalArgumentException correct) {
		}

		try {
			new JsrJobOperator(new SimpleJobExplorer(null, null, null, null), null, new SimpleJobOperator(), parameterConverter);
			fail("JobRepository should be required");
		} catch (IllegalArgumentException correct) {
		}

		try {
			new JsrJobOperator(new SimpleJobExplorer(null, null, null, null), new JobRepositorySupport(), null, parameterConverter);
			fail("JobOperator should be required");
		} catch (IllegalArgumentException correct) {
		}

		try {
			new JsrJobOperator(new SimpleJobExplorer(null, null, null, null), new JobRepositorySupport(), new SimpleJobOperator(), null);
			fail("ParameterConverter should be required");
		} catch (IllegalArgumentException correct) {
		}

		new JsrJobOperator(new SimpleJobExplorer(null, null, null, null), new JobRepositorySupport(), new SimpleJobOperator(), parameterConverter);
	}

	@Test
	public void testAbandonRoseyScenario() throws Exception {
		JobExecution jobExecution = new JobExecution(5l);
		jobExecution.setEndTime(new Date());
		when(jobExplorer.getJobExecution(5l)).thenReturn(jobExecution);

		jsrJobOperator.abandon(5l);

		ArgumentCaptor<JobExecution> executionCaptor = ArgumentCaptor.forClass(JobExecution.class);
		verify(jobRepository).update(executionCaptor.capture());
		assertEquals(org.springframework.batch.core.BatchStatus.ABANDONED, executionCaptor.getValue().getStatus());

	}

	@Test(expected=NoSuchJobExecutionException.class)
	public void testAbandonNoSuchJob() throws Exception {
		jsrJobOperator.abandon(5l);
	}

	@Test(expected=JobExecutionIsRunningException.class)
	public void testAbandonJobRunning() throws Exception {
		JobExecution jobExecution = new JobExecution(5l);
		when(jobExplorer.getJobExecution(5l)).thenReturn(jobExecution);

		jsrJobOperator.abandon(5l);
	}

	@Test
	public void testGetJobExecutionRoseyScenario() {
		when(jobExplorer.getJobExecution(5l)).thenReturn(new JobExecution(5l));

		assertEquals(5l, jsrJobOperator.getJobExecution(5l).getExecutionId());
	}

	@Test(expected=NoSuchJobExecutionException.class)
	public void testGetJobExecutionNoExecutionFound() {
		jsrJobOperator.getJobExecution(5l);
	}

	@Test
	public void testGetJobExecutionsRoseyScenario() {
		org.springframework.batch.core.JobInstance jobInstance = new org.springframework.batch.core.JobInstance(5l, "my job");
		List<JobExecution> executions = new ArrayList<JobExecution>();
		executions.add(new JobExecution(2l));

		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(executions);

		List<javax.batch.runtime.JobExecution> jobExecutions = jsrJobOperator.getJobExecutions(jobInstance);
		assertEquals(1, jobExecutions.size());
		assertEquals(2l, executions.get(0).getId().longValue());
	}

	@Test(expected=NoSuchJobInstanceException.class)
	public void testGetJobExecutionsNullJobInstance() {
		jsrJobOperator.getJobExecutions(null);
	}

	@Test(expected=NoSuchJobInstanceException.class)
	public void testGetJobExecutionsNullReturned() {
		org.springframework.batch.core.JobInstance jobInstance = new org.springframework.batch.core.JobInstance(5l, "my job");

		jsrJobOperator.getJobExecutions(jobInstance);
	}

	@Test(expected=NoSuchJobInstanceException.class)
	public void testGetJobExecutionsNoneReturned() {
		org.springframework.batch.core.JobInstance jobInstance = new org.springframework.batch.core.JobInstance(5l, "my job");
		List<JobExecution> executions = new ArrayList<JobExecution>();

		when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(executions);

		jsrJobOperator.getJobExecutions(jobInstance);
	}

	@Test
	public void testGetJobInstanceRoseyScenario() {
		JobInstance instance = new JobInstance(1l, "my job");
		JobExecution execution = new JobExecution(5l);
		execution.setJobInstance(instance);

		when(jobExplorer.getJobExecution(5l)).thenReturn(execution);
		when(jobExplorer.getJobInstance(1l)).thenReturn(instance);

		javax.batch.runtime.JobInstance jobInstance = jsrJobOperator.getJobInstance(5l);

		assertEquals(1l, jobInstance.getInstanceId());
		assertEquals("my job", jobInstance.getJobName());
	}

	@Test(expected=NoSuchJobExecutionException.class)
	public void testGetJobInstanceNoExecution() {
		JobInstance instance = new JobInstance(1l, "my job");
		JobExecution execution = new JobExecution(5l);
		execution.setJobInstance(instance);

		jsrJobOperator.getJobInstance(5l);
	}

	@Test
	public void testGetJobInstanceCount() throws Exception {
		when(jobExplorer.getJobInstanceCount("myJob")).thenReturn(4);

		assertEquals(4, jsrJobOperator.getJobInstanceCount("myJob"));
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetJobInstanceCountNoSuchJob() throws Exception {
		when(jobExplorer.getJobInstanceCount("myJob")).thenThrow(new org.springframework.batch.core.launch.NoSuchJobException("expected"));

		jsrJobOperator.getJobInstanceCount("myJob");
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetJobInstanceCountZeroInstancesReturned() throws Exception {
		when(jobExplorer.getJobInstanceCount("myJob")).thenReturn(0);

		jsrJobOperator.getJobInstanceCount("myJob");
	}

	@Test
	public void testGetJobInstancesRoseyScenario() {
		List<JobInstance> instances = new ArrayList<JobInstance>();
		instances.add(new JobInstance(1l, "myJob"));
		instances.add(new JobInstance(2l, "myJob"));
		instances.add(new JobInstance(3l, "myJob"));

		when(jobExplorer.getJobInstances("myJob", 0, 3)).thenReturn(instances);

		List<javax.batch.runtime.JobInstance> jobInstances = jsrJobOperator.getJobInstances("myJob", 0, 3);

		assertEquals(3, jobInstances.size());
		assertEquals(1l, jobInstances.get(0).getInstanceId());
		assertEquals(2l, jobInstances.get(1).getInstanceId());
		assertEquals(3l, jobInstances.get(2).getInstanceId());
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetJobInstancesNullInstancesReturned() {
		jsrJobOperator.getJobInstances("myJob", 0, 3);
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetJobInstancesZeroInstancesReturned() {
		List<JobInstance> instances = new ArrayList<JobInstance>();

		when(jobExplorer.getJobInstances("myJob", 0, 3)).thenReturn(instances);

		jsrJobOperator.getJobInstances("myJob", 0, 3);
	}

	@Test
	public void testGetJobNames() {
		List<String> jobNames = new ArrayList<String>();
		jobNames.add("job1");
		jobNames.add("job2");

		when(jobExplorer.getJobNames()).thenReturn(jobNames);

		Set<String> result = jsrJobOperator.getJobNames();

		assertEquals(2, result.size());
		assertTrue(result.contains("job1"));
		assertTrue(result.contains("job2"));
	}

	@Test
	public void testGetParametersRoseyScenario() {
		JobExecution jobExecution = new JobExecution(5l, new JobParametersBuilder().addString("key1", "value1").addLong(JsrJobParametersConverter.JOB_RUN_ID, 5l).toJobParameters());

		when(jobExplorer.getJobExecution(5l)).thenReturn(jobExecution);

		Properties params = jsrJobOperator.getParameters(5l);

		assertEquals("value1", params.get("key1"));
		assertNull(params.get(JsrJobParametersConverter.JOB_RUN_ID));
	}

	@Test(expected=NoSuchJobExecutionException.class)
	public void testGetParametersNoExecution() {
		jsrJobOperator.getParameters(5l);
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetNoRunningExecutions() {
		Set<JobExecution> executions = new HashSet<JobExecution>();

		when(jobExplorer.findRunningJobExecutions("myJob")).thenReturn(executions);

		jsrJobOperator.getRunningExecutions("myJob");
	}

	@Test
	public void testGetRunningExecutions() {
		Set<JobExecution> executions = new HashSet<JobExecution>();
		executions.add(new JobExecution(5l));

		when(jobExplorer.findRunningJobExecutions("myJob")).thenReturn(executions);

		assertEquals(5l, jsrJobOperator.getRunningExecutions("myJob").get(0).longValue());
	}

	@Test
	public void testGetStepExecutionsRoseyScenario() {
		JobExecution jobExecution = new JobExecution(5l);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
		stepExecutions.add(new StepExecution("step1", jobExecution, 1l));
		stepExecutions.add(new StepExecution("step2", jobExecution, 2l));
		jobExecution.addStepExecutions(stepExecutions);

		when(jobExplorer.getJobExecution(5l)).thenReturn(jobExecution);
		when(jobExplorer.getStepExecution(5l, 1l)).thenReturn(new StepExecution("step1", jobExecution, 1l));
		when(jobExplorer.getStepExecution(5l, 2l)).thenReturn(new StepExecution("step2", jobExecution, 2l));

		List<javax.batch.runtime.StepExecution> results = jsrJobOperator.getStepExecutions(5l);

		assertEquals("step1", results.get(0).getStepName());
		assertEquals("step2", results.get(1).getStepName());
	}

	@Test(expected=NoSuchJobException.class)
	public void testGetStepExecutionsNoExecutionReturned() {
		jsrJobOperator.getStepExecutions(5l);
	}

	@Test
	public void testGetStepExecutionsNoStepExecutions() {
		JobExecution jobExecution = new JobExecution(5l);

		when(jobExplorer.getJobExecution(5l)).thenReturn(jobExecution);

		List<javax.batch.runtime.StepExecution> results = jsrJobOperator.getStepExecutions(5l);

		assertEquals(0, results.size());
	}

	@Test
	public void testStartRoseyScenario() {
		jsrJobOperator = BatchRuntime.getJobOperator();

		long executionId = jsrJobOperator.start("jsrJobOperatorTestJob", null);

		assertEquals(BatchStatus.COMPLETED, jsrJobOperator.getJobExecution(executionId).getBatchStatus());
	}

	@Test
	public void testStartMultipleTimesSameParameters() throws Exception {
		jsrJobOperator = BatchRuntime.getJobOperator();

		int jobInstanceCountBefore = 0;

		try {
			jobInstanceCountBefore = jsrJobOperator.getJobInstanceCount("myJob3");
		} catch (NoSuchJobException ignore) {
		}

		long run1 = jsrJobOperator.start("jsrJobOperatorTestJob", null);
		long run2 = jsrJobOperator.start("jsrJobOperatorTestJob", null);
		long run3 = jsrJobOperator.start("jsrJobOperatorTestJob", null);

		assertEquals(BatchStatus.COMPLETED, jsrJobOperator.getJobExecution(run1).getBatchStatus());
		assertEquals(BatchStatus.COMPLETED, jsrJobOperator.getJobExecution(run2).getBatchStatus());
		assertEquals(BatchStatus.COMPLETED, jsrJobOperator.getJobExecution(run3).getBatchStatus());

		int jobInstanceCountAfter = jsrJobOperator.getJobInstanceCount("myJob3");

		assertTrue((jobInstanceCountAfter - jobInstanceCountBefore) == 3);
	}

	@Test
	public void testRestartRoseyScenario() {
		jsrJobOperator = BatchRuntime.getJobOperator();

		long executionId = jsrJobOperator.start("jsrJobOperatorTestRestartJob", null);

		assertEquals(BatchStatus.FAILED, jsrJobOperator.getJobExecution(executionId).getBatchStatus());

		long finalExecutionId = jsrJobOperator.restart(executionId, null);

		assertEquals(BatchStatus.COMPLETED, jsrJobOperator.getJobExecution(finalExecutionId).getBatchStatus());
	}
}
