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

package org.springframework.batch.repeat.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.beans.factory.InitializingBean;

/**
 * Simple implementation of exception handler which looks for given exception
 * types. If one of the types is found then a counter is incremented and the
 * limit is checked to determine if it has been exceeded and the Throwable
 * should be re-thrown. Also allows to specify list of 'fatal' exceptions that
 * are never subject to counting, but are immediately re-thrown. The fatal list
 * has higher priority so the two lists needn't be exclusive.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class SimpleLimitExceptionHandler implements ExceptionHandler, InitializingBean {

	private RethrowOnThresholdExceptionHandler delegate = new RethrowOnThresholdExceptionHandler();

	private Collection<Class<? extends Throwable>> exceptionClasses = Collections
			.<Class<? extends Throwable>> singleton(Exception.class);

	private Collection<Class<? extends Throwable>> fatalExceptionClasses = Collections
			.<Class<? extends Throwable>> singleton(Error.class);

	private int limit = 0;

	/**
	 * Apply the provided properties to create a delegate handler.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		if (limit <= 0) {
			return;
		}
		Map<Class<? extends Throwable>, Integer> thresholds = new HashMap<Class<? extends Throwable>, Integer>();
		for (Class<? extends Throwable> type : exceptionClasses) {
			thresholds.put(type, limit);
		}
		// do the fatalExceptionClasses last so they override the others
		for (Class<? extends Throwable> type : fatalExceptionClasses) {
			thresholds.put(type, 0);
		}
		delegate.setThresholds(thresholds);
	}

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
	 * Convenience constructor for the {@link SimpleLimitExceptionHandler} to
	 * set the limit.
	 * 
	 * @param limit the limit
	 */
	public SimpleLimitExceptionHandler(int limit) {
		this();
		this.limit = limit;
	}

	/**
	 * Default constructor for the {@link SimpleLimitExceptionHandler}.
	 */
	public SimpleLimitExceptionHandler() {
		super();
	}

	/**
	 * Rethrows only if the limit is breached for this context on the exception
	 * type specified.
	 * 
	 * @see #setExceptionClasses(Collection)
	 * @see #setLimit(int)
	 * 
	 * @see org.springframework.batch.repeat.exception.ExceptionHandler#handleException(org.springframework.batch.repeat.RepeatContext,
	 * Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {
		delegate.handleException(context, throwable);
	}

	/**
	 * The limit on the given exception type within a single context before it
	 * is rethrown.
	 * 
	 * @param limit the limit
	 */
	public void setLimit(final int limit) {
		this.limit = limit;
	}

	/**
	 * Setter for the exception classes that this handler counts. Defaults to
	 * {@link Exception}. If more exceptionClasses are specified handler uses
	 * single counter that is incremented when one of the recognized exception
	 * exceptionClasses is handled.
	 * @param classes exceptionClasses
	 */
	public void setExceptionClasses(Collection<Class<? extends Throwable>> classes) {
		this.exceptionClasses = classes;
	}

	/**
	 * Setter for the exception classes that shouldn't be counted, but rethrown
	 * immediately. This list has higher priority than
	 * {@link #setExceptionClasses(Collection)}.
	 * 
	 * @param fatalExceptionClasses defaults to {@link Error}
	 */
	public void setFatalExceptionClasses(Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
	}

}
