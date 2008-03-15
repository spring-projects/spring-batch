package org.springframework.batch.sample.step.support;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.StepInterruptionPolicy;

public class NoopStepInterruptionPolicy implements StepInterruptionPolicy {

	public void checkInterrupted(StepExecution stepExecution)
			throws JobInterruptedException {
		// no-op
		
	}

}
