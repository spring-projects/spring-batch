package org.springframework.batch.sample.advice;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.sample.tasklet.JobSupport;

/**
 * Tests for {@link DefaultJobInterruptionAdvice}.
 * 
 * @author Robert Kasanicky
 */
public class DefaultJobInterruptionAdviceTests extends TestCase {

	private DefaultJobInterruptionAdvice tested = new DefaultJobInterruptionAdvice();

	private JobExecution jobExecution = new JobExecution(new JobInstance(new Long(1), new JobParameters(),
			new JobSupport("interruptJob")));

	protected void setUp() throws Exception {
		tested.setJobExecution(jobExecution);
	}

	/**
	 * Scenario when JobExecution is requested to stop explicitly.
	 */
	public void testStop() throws JobInterruptedException {

		jobExecution.stop();
		try {
			tested.checkInterrupt();
			fail();
		}
		catch (JobInterruptedException e) {
			// expected
		}
	}

	/**
	 * Scenario when executing thread is interrupted. Needs to be run in
	 * separate victim thread - interrupting current thread would affect other
	 * tests.
	 */
	public void testThreadInterrupt() throws Exception {

		class TestJob implements Runnable {

			volatile boolean interrupted = false;

			public void run() {
				while (true) {
					try {
						tested.checkInterrupt();
					}
					catch (JobInterruptedException expected) {
						interrupted = true;
						return;
					}
				}
			}
		}

		TestJob job = new TestJob();
		Thread victim = new Thread(job);

		victim.start();
		victim.interrupt();

		Thread.sleep(1000);
		assertTrue(job.interrupted);

	}

	/**
	 * No exception raised when JobExecution has non-terminating status.
	 */
	public void testNoInterrupt() throws JobInterruptedException {

		BatchStatus[] notInterruptedValues = { BatchStatus.STARTED, BatchStatus.STARTING, BatchStatus.STOPPED,
				BatchStatus.UNKNOWN, BatchStatus.COMPLETED, BatchStatus.FAILED };

		for (int i = 0; i < notInterruptedValues.length; i++) {
			jobExecution.setStatus(notInterruptedValues[i]);
			tested.checkInterrupt();
			assertTrue(true);
		}
	}
}
