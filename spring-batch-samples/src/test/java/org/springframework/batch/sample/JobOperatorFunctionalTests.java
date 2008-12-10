package org.springframework.batch.sample;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.springframework.batch.core.BatchStatus.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.ListableJobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
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

}
