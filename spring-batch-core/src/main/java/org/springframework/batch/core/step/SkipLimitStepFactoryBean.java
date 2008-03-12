package org.springframework.batch.core.step;

import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;

/**
 * Factory bean for step that allows configuring skip limit.
 * 
 */
public class SkipLimitStepFactoryBean extends DefaultStepFactoryBean {

	private int skipLimit = 0;
	
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
	 * Uses the {@link #skipLimit} value to configure item handler and
	 * and exception handler.
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
			itemHandler.setItemSkipPolicy(new LimitCheckingItemSkipPolicy(skipLimit));
			setExceptionHandler(new SimpleLimitExceptionHandler(skipLimit));
			getStepOperations().setExceptionHandler(getExceptionHandler());
		}
		else {
			// This is the default in ItemOrientedStep anyway...
			itemHandler.setItemSkipPolicy(new NeverSkipItemSkipPolicy());
		}
		
		step.setItemHandler(itemHandler);
	}
	
	
}
