package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ItemKeyGenerator;
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

	private ItemKeyGenerator itemKeyGenerator;

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
	 * Protected getter for the fatal exceptions.
	 * @return the fatalExceptionClasses
	 */
	protected Class[] getFatalExceptionClasses() {
		return fatalExceptionClasses;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator}. This is used to identify
	 * failed items so they can be skipped if encountered again, generally in
	 * another transaction.
	 * 
	 * @param itemKeyGenerator the {@link ItemKeyGenerator} to set.
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
	}
	
	/**
	 * Protected getter for the {@link ItemKeyGenerator}.
	 * @return the {@link ItemKeyGenerator}
	 */
	protected ItemKeyGenerator getItemKeyGenerator() {
		return itemKeyGenerator;
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
			addFatalExceptionIfMissing(SkipLimitExceededException.class);
			List fatalExceptionList = Arrays.asList(fatalExceptionClasses);
			
			LimitCheckingItemSkipPolicy skipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, Arrays
					.asList(skippableExceptionClasses), fatalExceptionList);
			itemHandler.setItemSkipPolicy(skipPolicy);
			SimpleLimitExceptionHandler exceptionHandler = new SimpleLimitExceptionHandler(skipLimit);
			exceptionHandler.setExceptionClasses(skippableExceptionClasses);
			exceptionHandler.setFatalExceptionClasses(fatalExceptionClasses);

			getStepOperations().setExceptionHandler(exceptionHandler);

			// for subclass to pick up limit and exception classes
			setExceptionHandler(exceptionHandler);

			itemHandler.setItemKeyGenerator(itemKeyGenerator);

			BatchListenerFactoryHelper helper = new BatchListenerFactoryHelper();
			itemHandler.setSkipListeners(helper.getSkipListeners(getListeners()));

		}
		else {
			// This is the default in ItemOrientedStep anyway...
			itemHandler.setItemSkipPolicy(new NeverSkipItemSkipPolicy());
		}

		step.setItemHandler(itemHandler);
	}

	/**
	 * @return
	 */
	public void addFatalExceptionIfMissing(Class cls) {
		List fatalExceptionList = new ArrayList(Arrays.asList(fatalExceptionClasses));
		if (!fatalExceptionList.contains(cls)) {
			fatalExceptionList.add(cls);
		}
		fatalExceptionClasses = (Class[]) fatalExceptionList.toArray(new Class[0]);
	}

}
