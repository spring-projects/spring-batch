package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Properties;

import javax.batch.runtime.Metric;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

public class StepContextTests {

	private StepExecution stepExecution;
	private StepContext stepContext;

	@Before
	public void setUp() throws Exception {
		JobExecution jobExecution = new JobExecution(1l, new JobParametersBuilder().addString("key", "value").toJobParameters());

		stepExecution = new StepExecution("testStep", jobExecution);
		stepExecution.setId(5l);
		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setExitStatus(new ExitStatus("customExitStatus"));
		stepExecution.setCommitCount(1);
		stepExecution.setFilterCount(2);
		stepExecution.setProcessSkipCount(3);
		stepExecution.setReadCount(4);
		stepExecution.setReadSkipCount(5);
		stepExecution.setRollbackCount(6);
		stepExecution.setWriteCount(7);
		stepExecution.setWriteSkipCount(8);

		stepContext = new StepContext(stepExecution, new ParametersConverterSupport());
		stepContext.setTransientUserData("This is my transient data");
	}

	@Test
	public void testBasicProperties() {
		assertEquals(javax.batch.runtime.BatchStatus.STARTED, stepContext.getBatchStatus());
		assertEquals("customExitStatus", stepContext.getExitStatus());
		assertEquals(5l, stepContext.getStepExecutionId());
		assertEquals("testStep", stepContext.getStepName());
		assertEquals("This is my transient data", stepContext.getTransientUserData());

		Properties params = stepContext.getProperties();
		assertEquals("value", params.get("key"));

		Metric[] metrics = stepContext.getMetrics();

		for (Metric metric : metrics) {
			switch (metric.getType()) {
			case COMMIT_COUNT:
				assertEquals(1, metric.getValue());
				break;
			case FILTER_COUNT:
				assertEquals(2, metric.getValue());
				break;
			case PROCESS_SKIP_COUNT:
				assertEquals(3, metric.getValue());
				break;
			case READ_COUNT:
				assertEquals(4, metric.getValue());
				break;
			case READ_SKIP_COUNT:
				assertEquals(5, metric.getValue());
				break;
			case ROLLBACK_COUNT:
				assertEquals(6, metric.getValue());
				break;
			case WRITE_COUNT:
				assertEquals(7, metric.getValue());
				break;
			case WRITE_SKIP_COUNT:
				assertEquals(8, metric.getValue());
				break;
			default:
				fail("Invalid metric type");
			}
		}
	}

	@Test
	public void testSetExitStatus() {
		stepContext.setExitStatus("new Exit Status");
		assertEquals("new Exit Status", stepExecution.getExitStatus().getExitCode());
	}
}
