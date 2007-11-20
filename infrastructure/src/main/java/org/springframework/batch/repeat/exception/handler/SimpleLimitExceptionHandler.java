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

package org.springframework.batch.repeat.exception.handler;

import java.util.HashMap;

import org.springframework.batch.common.ExceptionClassifierSupport;
import org.springframework.batch.io.exception.TransactionInvalidException;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Simple implementation of exception handler which looks for one exception
 * type. If it is found in the Collection of throwables then a counter is
 * incremented and the a limit is checked to determine if it has been exceeded
 * and the Throwable should be re-thrown.
 * 
 * @author Dave Syer
 */
public class SimpleLimitExceptionHandler implements ExceptionHandler {

	/**
	 * Name of exception classifier key for the
	 * {@link TransactionInvalidException}.
	 */
	private static final String TX_INVALID = "TX_INVALID";

	private RethrowOnThresholdExceptionHandler delegate = new RethrowOnThresholdExceptionHandler();

	private Class type = TransactionInvalidException.class;

	/**
	 * Flag to indicate the the exception counters should be shared between
	 * sibling contexts in a nested batch (i.e. inner loop). Default is false.
	 * Set this flag to true if you want to count exceptions for the whole
	 * (outer) loop in a typical container.
	 * 
	 * @param useParent true if the parent context should be used to store the
	 * counters.
	 */
	public void setUseParent(boolean useParent) {
		delegate.setUseParent(useParent);
	}

	/**
	 * Default constructor for the {@link SimpleLimitExceptionHandler}.
	 */
	public SimpleLimitExceptionHandler() {
		super();
		delegate.setExceptionClassifier(new ExceptionClassifierSupport() {
			public Object classify(Throwable throwable) {
				if (type.isAssignableFrom(throwable.getClass())) {
					return TX_INVALID;
				}
				return super.classify(throwable);
			}
		});
	}

	/**
	 * Rethrows only if the limit is breached for this context on the exception
	 * type specified.
	 * 
	 * @see #setType(Class)
	 * @see #setLimit(int)
	 * 
	 * @see org.springframework.batch.repeat.exception.handler.ExceptionHandler#handleException(org.springframework.batch.repeat.RepeatContext,
	 * Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {
		delegate.handleException(context, throwable);
	}

	/**
	 * The limit on the given exception type within a single context before it
	 * is rethrown.
	 * 
	 * @param limit
	 */
	public void setLimit(final int limit) {
		delegate.setThresholds(new HashMap() {
			{
				put(ExceptionClassifierSupport.DEFAULT, new Integer(0));
				put(TX_INVALID, new Integer(limit));
			}
		});
	}

	/**
	 * Setter for the Throwable type that this handler counts. Defaults to
	 * {@link TransactionInvalidException}.
	 * 
	 * @param type
	 */
	public void setType(Class type) {
		this.type = type;
	}

}
