package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/infiniteLoopJob.xml" })
public class JobOperatorFunctionalTests {

	private static final Log logger = LogFactory.getLog(JobOperatorFunctionalTests.class);

	@Autowired
	private JobOperator operator;

	@Autowired
	private Job job;

	@Autowired
	private JobRegistry jobRegistry;

	@Before
	public void setUp() throws Exception {
		if (!jobRegistry.getJobNames().contains(job.getName())) {
			jobRegistry.register(new ReferenceJobFactory(job));
		}
	}

	@Test
	public void testStartStopResumeJob() throws Exception {

		String params = new JobParametersBuilder().addLong("jobOperatorTestParam", 7L).toJobParameters().toString();

		long executionId = operator.start(job.getName(), params);
		assertEquals(params, operator.getParameters(executionId));
		stopAndCheckStatus(executionId);

		long resumedExecutionId = operator.restart(executionId);
		assertEquals(params, operator.getParameters(resumedExecutionId));
		stopAndCheckStatus(resumedExecutionId);

		List<Long> instances = operator.getJobInstances(job.getName(), 0, 1);
		assertEquals(1, instances.size());
		long instanceId = instances.get(0);

		List<Long> executions = operator.getExecutions(instanceId);
		assertEquals(2, executions.size());
		// latest execution is the first in the returned list
		assertEquals(resumedExecutionId, executions.get(0).longValue());
		assertEquals(executionId, executions.get(1).longValue());

	}

	/**
	 * @param executionId id of running job execution
	 */
	private void stopAndCheckStatus(long executionId) throws Exception {

		// wait to the job to get up and running
		Thread.sleep(1000);

		Set<Long> runningExecutions = operator.getRunningExecutions(job.getName());
		assertTrue("Wrong executions: " + runningExecutions + " expected: " + executionId, runningExecutions
				.contains(executionId));
		assertTrue("Wrong summary: " + operator.getSummary(executionId), operator.getSummary(executionId).contains(
				BatchStatus.STARTED.toString()));

		operator.stop(executionId);

		int count = 0;
		while (operator.getRunningExecutions(job.getName()).contains(executionId) && count <= 10) {
			logger.info("Checking for running JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		runningExecutions = operator.getRunningExecutions(job.getName());
		assertFalse("Wrong executions: " + runningExecutions + " expected: " + executionId, runningExecutions
				.contains(executionId));
		assertTrue("Wrong summary: " + operator.getSummary(executionId), operator.getSummary(executionId).contains(
				BatchStatus.STOPPED.toString()));

		// there is just a single step in the test job
		Map<Long, String> summaries = operator.getStepExecutionSummaries(executionId);
		System.err.println(summaries);
		assertTrue(summaries.values().toString().contains(BatchStatus.STOPPED.toString()));
	}

	@Test
	public void testMultipleSimultaneousInstances() throws Exception {
		String jobName = job.getName();

		Set<String> names = operator.getJobNames();
		assertEquals(1, names.size());
		assertTrue(names.contains(jobName));

		long exec1 = operator.startNextInstance(jobName);
		long exec2 = operator.startNextInstance(jobName);

		assertTrue(exec1 != exec2);
		assertTrue(operator.getParameters(exec1) != operator.getParameters(exec2));

		Set<Long> executions = operator.getRunningExecutions(jobName);
		assertTrue(executions.contains(exec1));
		assertTrue(executions.contains(exec2));
		int count = 0;
		boolean running = operator.getSummary(exec1).contains("STARTED")
				&& operator.getSummary(exec2).contains("STARTED");
		while (count++ < 10 && !running) {
			Thread.sleep(100L);
			running = operator.getSummary(exec1).contains("STARTED") && operator.getSummary(exec2).contains("STARTED");
		}
		assertTrue(String.format("Jobs not started: [%s] and [%s]", operator.getSummary(exec1), operator
				.getSummary(exec1)), running);

		operator.stop(exec1);
		operator.stop(exec2);

	}

}
