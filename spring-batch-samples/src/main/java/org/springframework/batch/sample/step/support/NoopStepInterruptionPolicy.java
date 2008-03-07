package org.springframework.batch.sample.step.support;

import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.execution.step.support.StepInterruptionPolicy;
import org.springframework.batch.repeat.RepeatContext;

public class NoopStepInterruptionPolicy implements StepInterruptionPolicy {

	public void checkInterrupted(RepeatContext context)
			throws JobInterruptedException {
		// no-op
		
	}

}
