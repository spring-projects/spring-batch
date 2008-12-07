package org.springframework.batch.core.scope.context;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * Convenient aspect to wrap a single threaded step execution, where the
 * implementation of the {@link Step} is not step scope aware (i.e. not the ones
 * provided by the framework).
 * 
 * @author Dave Syer
 * 
 */
@Aspect
public class StepScopeManager {

	@Around("execution(void org.springframework.batch.core.Step+.execute(*)) && target(step) && args(stepExecution)")
	public void execute(Step step, StepExecution stepExecution) throws JobInterruptedException {
		StepSynchronizationManager.register(stepExecution);
		try {
			step.execute(stepExecution);
		}
		finally {
			StepSynchronizationManager.release();
		}
	}

}
