package org.springframework.batch.core.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
	 * CONDITION: ExecutionContext contains {key, key2}. keys = {key}. statuses
	 * is not set (defaults to {COMPLETED}).
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntryNullStatuses() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(ExitStatus.COMPLETED);

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(new String[] { key });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: ExecutionContext contains {key, key2}. keys = {key, key2}.
	 * statuses = {status}. ExitStatus = status
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntryStatusFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
		listener.setStrict(true);

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(new String[] { key });
		listener.setStatuses(new String[] { status });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: ExecutionContext contains {key, key2}. keys = {key, key2}.
	 * statuses = {status}. ExitStatus = status2
	 * 
	 * EXPECTED: no promotions.
	 */
	@Test
	public void promoteEntryStatusNotFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status2));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(new String[] { key });
		listener.setStatuses(new String[] { status });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertFalse(jobExecution.getExecutionContext().containsKey(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key, key2}. statuses = {statusWildcard}. ExitStatus =
	 * status
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntryStatusWildcardFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(new ExitStatus(status));

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);
		stepExecution.getExecutionContext().putString(key2, value2);

		listener.setKeys(new String[] { key });
		listener.setStatuses(new String[] { statusWildcard });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key, key2}. Only {key} exists in the ExecutionContext.
	 * 
	 * EXPECTED: key is promoted. key2 is not.
	 */
	@Test
	public void promoteEntriesKeyNotFound() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(ExitStatus.COMPLETED);

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);

		listener.setKeys(new String[] { key, key2 });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
		assertFalse(jobExecution.getExecutionContext().containsKey(key2));
	}

	/**
	 * CONDITION: keys = {key}. key is already in job but not in step.
	 * 
	 * EXPECTED: key is not erased.
	 */
	@Test
	public void promoteEntriesKeyNotFoundInStep() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(ExitStatus.COMPLETED);

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		jobExecution.getExecutionContext().putString(key, value);

		listener.setKeys(new String[] { key });
		listener.afterPropertiesSet();

		listener.afterStep(stepExecution);

		assertEquals(value, jobExecution.getExecutionContext().getString(key));
	}

	/**
	 * CONDITION: strict = true. keys = {key, key2}. Only {key} exists in the
	 * ExecutionContext.
	 * 
	 * EXPECTED: IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void promoteEntriesKeyNotFoundStrict() throws Exception {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
		listener.setStrict(true);

		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = jobExecution.createStepExecution("step1");
		stepExecution.setExitStatus(ExitStatus.COMPLETED);

		Assert.state(jobExecution.getExecutionContext().isEmpty());
		Assert.state(stepExecution.getExecutionContext().isEmpty());

		stepExecution.getExecutionContext().putString(key, value);

		listener.setKeys(new String[] { key, key2 });
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
