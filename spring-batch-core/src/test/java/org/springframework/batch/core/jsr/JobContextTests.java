package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class JobContextTests {

	private JobContext context;
	@Mock
	private JobExecution execution;
	@Mock
	private JobInstance instance;
	@Mock
	private ParametersConverter converter;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		context = new JobContext(execution, converter);
		when(execution.getJobInstance()).thenReturn(instance);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		context = new JobContext(null, null);
	}

	@Test
	public void testGetJobName() {
		when(instance.getJobName()).thenReturn("jobName");

		assertEquals("jobName", context.getJobName());
	}

	@Test
	public void testTransientUserData() {
		context.setTransientUserData("This is my data");
		assertEquals("This is my data", context.getTransientUserData());
	}

	@Test
	public void testGetInstanceId() {
		when(instance.getId()).thenReturn(5L);

		assertEquals(5L, context.getInstanceId());
	}

	@Test
	public void testGetExecutionId() {
		when(execution.getId()).thenReturn(5L);

		assertEquals(5L, context.getExecutionId());
	}

	@Test
	public void testGetProperties() {
		JobParameters params = new JobParametersBuilder()
		.addString("key1", "value1")
		.toJobParameters();
		Properties results = new Properties();
		results.put("key1", "value1");

		when(execution.getJobParameters()).thenReturn(params);
		when(converter.convert(params)).thenReturn(results);

		Properties props = context.getProperties();

		assertEquals("value1", props.get("key1"));
	}

	@Test
	public void testGetBatchStatus() {
		when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);

		assertEquals(javax.batch.runtime.BatchStatus.COMPLETED, context.getBatchStatus());
	}

	@Test
	public void testExitStatus() {
		when(execution.getExitStatus()).thenReturn(new ExitStatus("exit"));

		assertEquals("exit", context.getExitStatus());

		context.setExitStatus("my exit status");

		verify(execution).setExitStatus(new ExitStatus("my exit status"));
	}
}
