package org.springframework.batch.core.listener;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.Assert;

/**
 * Tests for {@link ExecutionContextPromotionListener}.
 */
public class ExecutionContextPromotionListenerTests {

	private ExecutionContextPromotionListener tested = new ExecutionContextPromotionListener();

	private static final String key = "testKey";

	private static final String value = "testValue";

	@Test
	public void promoteEntryFromStepToJobExectutionContext() throws Exception {

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);

		tested.setKeys(Collections.singletonList(key));
		tested.afterPropertiesSet();

		tested.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
	}

	@Test(expected = IllegalArgumentException.class)
	public void keysMustBeSet() throws Exception {
		// didn't set the keys, same as tested.setKeys(null);
		tested.afterPropertiesSet();
	}
}
