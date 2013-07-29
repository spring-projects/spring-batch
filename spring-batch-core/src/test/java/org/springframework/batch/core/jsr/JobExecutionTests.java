package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class JobExecutionTests {

	private JobExecution adapter;

	@Before
	public void setUp() throws Exception {
		JobInstance instance = new JobInstance(2L, "job name");

		JobParameters params = new JobParametersBuilder().addString("key1", "value1").toJobParameters();

		org.springframework.batch.core.JobExecution execution = new org.springframework.batch.core.JobExecution(instance, params);

		execution.setId(5L);
		execution.setCreateTime(new Date(0));
		execution.setEndTime(new Date(999999999l));
		execution.setExitStatus(new ExitStatus("exit status"));
		execution.setLastUpdated(new Date(12345));
		execution.setStartTime(new Date(98765));
		execution.setStatus(BatchStatus.FAILED);
		execution.setVersion(21);

		adapter = new JobExecution(execution);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new JobExecution(null);
	}

	@Test
	public void testGetBasicValues() {
		assertEquals(javax.batch.runtime.BatchStatus.FAILED, adapter.getBatchStatus());
		assertEquals(new Date(0), adapter.getCreateTime());
		assertEquals(new Date(999999999l), adapter.getEndTime());
		assertEquals(5L, adapter.getExecutionId());
		assertEquals("exit status", adapter.getExitStatus());
		assertEquals("job name", adapter.getJobName());
		assertEquals(new Date(12345), adapter.getLastUpdatedTime());
		assertEquals(new Date(98765), adapter.getStartTime());

		Properties props = adapter.getJobParameters();

		assertEquals("value1", props.get("key1"));
	}
}
