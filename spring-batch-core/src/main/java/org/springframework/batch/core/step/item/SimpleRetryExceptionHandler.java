/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import java.util.Collection;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.listener.RetryListenerSupport;

/**
 * An {@link ExceptionHandler} that is aware of the retry context so that it can
 * distinguish between a fatal exception and one that can be retried. Delegates
 * the actual exception handling to another {@link ExceptionHandler}.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleRetryExceptionHandler extends RetryListenerSupport implements ExceptionHandler {

	/**
	 * Attribute key, whose existence signals an exhausted retry.
	 */
	private static final String EXHAUSTED = SimpleRetryExceptionHandler.class.getName() + ".RETRY_EXHAUSTED";
	
	private static final Log logger = LogFactory.getLog(SimpleRetryExceptionHandler.class);

	final private RetryPolicy retryPolicy;

	final private ExceptionHandler exceptionHandler;

	final private BinaryExceptionClassifier fatalExceptionClassifier;

	/**
	 * Create an exception handler from its mandatory properties.
	 * 
	 * @param retryPolicy the retry policy that will be under effect when an
	 * exception is encountered
	 * @param exceptionHandler the delegate to use if an exception actually
	 * needs to be handled
	 * @param fatalExceptionClasses
	 */
	public SimpleRetryExceptionHandler(RetryPolicy retryPolicy, ExceptionHandler exceptionHandler, Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		this.retryPolicy = retryPolicy;
		this.exceptionHandler = exceptionHandler;
		this.fatalExceptionClassifier = new BinaryExceptionClassifier(fatalExceptionClasses);
	}

	/**
	 * Check if the exception is going to be retried, and veto the handling if
	 * it is. If retry is exhausted or the exception is on the fatal list, then
	 * handle using the delegate.
	 * 
	 * @see ExceptionHandler#handleException(org.springframework.batch.repeat.RepeatContext,
	 * java.lang.Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {
		// Only bother to check the delegate exception handler if we know that
		// retry is exhausted
		if (fatalExceptionClassifier.classify(throwable) || context.hasAttribute(EXHAUSTED)) {
			logger.debug("Handled fatal exception");
			exceptionHandler.handleException(context, throwable);
		}
		else {
			logger.debug("Handled non-fatal exception", throwable);
		}
	}

	/**
	 * If retry is exhausted set up some state in the context that can be used
	 * to signal that the exception should be handled.
	 * 
	 * @see org.springframework.batch.retry.RetryListener#close(org.springframework.batch.retry.RetryContext,
	 * org.springframework.batch.retry.RetryCallback, java.lang.Throwable)
	 */
	public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
		if (!retryPolicy.canRetry(context)) {
			logger.debug("Marking retry as exhausted: "+context);
			getRepeatContext().setAttribute(EXHAUSTED, "true");
		}
	}

	/**
	 * Get the parent context (the retry is in an inner "chunk" loop and we want
	 * the exception to be handled at the outer "step" level).
	 * @return the {@link RepeatContext} that should hold the exhausted flag.
	 */
	private RepeatContext getRepeatContext() {
		RepeatContext context = RepeatSynchronizationManager.getContext();
		if (context.getParent() != null) {
			return context.getParent();
		}
		return context;
	}

}
