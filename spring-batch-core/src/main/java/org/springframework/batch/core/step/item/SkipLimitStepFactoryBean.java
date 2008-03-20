package org.springframework.batch.core.step.item;

import java.util.Arrays;

import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;

/**
 * Factory bean for step that provides options for configuring skip behavior.
 * User can set {@link #skipLimit} to set how many exceptions of
 * {@link #skippableExceptionClasses} types are tolerated.
 * {@link #fatalExceptionClasses} will cause immediate termination of job - they
 * are treated as higher priority than {@link #skippableExceptionClasses}, so
 * the two lists don't need to be exclusive.
 * 
 * @see SimpleStepFactoryBean
 * @see StatefulRetryStepFactoryBean
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class SkipLimitStepFactoryBean extends SimpleStepFactoryBean {

	private int skipLimit = 0;

	private Class[] skippableExceptionClasses = new Class[] { Exception.class };

	private Class[] fatalExceptionClasses = new Class[] { Error.class };

	/**
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for exception classes that when raised won't crash the job
	 * but will result in transaction rollback and the item which handling
	 * caused the exception will be skipped.
	 * 
	 * @param skippableExceptionClasses defaults to <code>Exception</code>
	 */
	public void setSkippableExceptionClasses(Class[] exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that should cause immediate failure.
	 * 
	 * @param fatalExceptionClasses {@link Error} by default
	 */
	public void setFatalExceptionClasses(Class[] fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
	}

	/**
	 * Uses the {@link #skipLimit} value to configure item handler and and
	 * exception handler.
	 */
	protected void applyConfiguration(ItemOrientedStep step) {
		super.applyConfiguration(step);

		ItemSkipPolicyItemHandler itemHandler = new ItemSkipPolicyItemHandler(getItemReader(), getItemWriter());

		if (skipLimit > 0) {
			/*
			 * If there is a skip limit (not the default) then we are prepared
			 * to absorb exceptions at the step level because the failed items
			 * will never re-appear after a rollback.
			 */
			itemHandler.setItemSkipPolicy(new LimitCheckingItemSkipPolicy(skipLimit, Arrays
					.asList(skippableExceptionClasses), Arrays.asList(fatalExceptionClasses)));
			SimpleLimitExceptionHandler exceptionHandler = new SimpleLimitExceptionHandler();
			exceptionHandler.setLimit(skipLimit);
			exceptionHandler.setExceptionClasses(skippableExceptionClasses);
			exceptionHandler.setFatalExceptionClasses(fatalExceptionClasses);
			setExceptionHandler(exceptionHandler);
			getStepOperations().setExceptionHandler(getExceptionHandler());
		}
		else {
			// This is the default in ItemOrientedStep anyway...
			itemHandler.setItemSkipPolicy(new NeverSkipItemSkipPolicy());
		}

		step.setItemHandler(itemHandler);
	}

}
