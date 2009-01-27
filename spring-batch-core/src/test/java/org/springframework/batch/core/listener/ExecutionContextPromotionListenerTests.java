package org.springframework.batch.core.listener;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.Assert;

/**
 * Tests for {@link ExecutionContextPromotionListener}.
 */
public class ExecutionContextPromotionListenerTests {

	private static final String key = "testKey";
	private static final String value = "testValue";
	private static final String key2 = "testKey2";
	private static final String value2 = "testValue2";
	private static final String status = "COMPLETED WITH SKIPS";
	private static final String status2 = "FAILURE";
	private static final String statusWildcard = "COMPL*SKIPS";

	/**
	 * CONDITION: keys = {key1, key2}. statuses is not set (defaults to
	 * {COMPLETED}).
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntry_nullStatuses() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(ExitStatus.COMPLETED);

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(Collections.singletonList(key));
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key1, key2}. statuses = {status}. ExitStatus = status
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntry_statusFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(Collections.singletonList(key));
		listener.setStatuses(Collections.singletonList(status));
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key1, key2}. statuses = {status}. ExitStatus = status2
	 * 
	 * EXPECTED: no promotions.
	 */
	@Test
	public void promoteEntry_statusNotFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status2));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(Collections.singletonList(key));
		listener.setStatuses(Collections.singletonList(status));
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertFalse(jobExecution.getExecutionContext().containsKey(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key1, key2}. statuses = {statusWildcard}. ExitStatus =
	 * status
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntry_statusWildcardFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(Collections.singletonList(key));
		listener.setStatuses(Collections.singletonList(statusWildcard));
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = NULL
	 * 
	 * EXPECTED: IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void keysMustBeSet() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
		// didn't set the keys, same as listener.setKeys(null);
		listener.afterPropertiesSet();
	}
}
