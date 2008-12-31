package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.batch.core.BatchStatus.STARTED;
import static org.springframework.batch.core.BatchStatus.STOPPED;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JobOperatorFunctionalTests {

	private static final Log logger = LogFactory.getLog(JobOperatorFunctionalTests.class);

	@Autowired
	private JobOperator tested;

	@Autowired
	private Job job;

	@Autowired
	private ListableJobRegistry jobRegistry;

	@Before
	public void setUp() throws Exception {
		if (!jobRegistry.getJobNames().contains(job.getName()))
			jobRegistry.register(new ReferenceJobFactory(job));
	}

	@Test
	public void testStartStopResumeJob() throws Exception {

		String params = new JobParametersBuilder().addLong("jobOperatorTestParam", 7L).toJobParameters().toString();

		long executionId = tested.start(job.getName(), params);
		assertEquals(params, tested.getParameters(executionId));
		stopAndCheckStatus(executionId);

		long resumedExecutionId = tested.resume(executionId);
		assertEquals(params, tested.getParameters(resumedExecutionId));
		stopAndCheckStatus(resumedExecutionId);

		List<Long> instances = tested.getLastInstances(job.getName(), 1);
		assertEquals(1, instances.size());
		long instanceId = instances.get(0);

		List<Long> executions = tested.getExecutions(instanceId);
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

		assertTrue(tested.getRunningExecutions(job.getName()).contains(executionId));
		assertTrue(tested.getSummary(executionId).contains(STARTED.toString()));

		tested.stop(executionId);

		int count = 0;
		while (tested.getRunningExecutions(job.getName()).contains(executionId) && count <= 10) {
			logger.info("Checking for running JobExecution: count=" + count);
			Thread.sleep(100);
			count++;
		}

		assertFalse(tested.getRunningExecutions(job.getName()).contains(executionId));
		assertTrue(tested.getSummary(executionId).contains(STOPPED.toString()));

		// there is just a single step in the test job
		Map<Long, String> summaries = tested.getStepExecutionSummaries(executionId);
		assertEquals(1, summaries.size());
		assertTrue(summaries.values().toString().contains(STOPPED.toString()));
	}

	@Test
	public void testMultipleSimultaneousInstances() throws Exception {
		String jobName = job.getName();

		Set<String> names = tested.getJobNames();
		assertEquals(1, names.size());
		assertTrue(names.contains(jobName));

		long exec1 = tested.startNextInstance(jobName);
		long exec2 = tested.startNextInstance(jobName);

		assertTrue(exec1 != exec2);
		assertTrue(tested.getParameters(exec1) != tested.getParameters(exec2));

		Set<Long> executions = tested.getRunningExecutions(jobName);
		assertTrue(executions.contains(exec1));
		assertTrue(executions.contains(exec2));

		tested.stop(exec1);
		tested.stop(exec2);

	}

}
