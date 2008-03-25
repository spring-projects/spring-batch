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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.support.ExceptionClassifier;
import org.springframework.batch.support.ExceptionClassifierSupport;

/**
 * Implementation of {@link ExceptionHandler} based on an {@link ExceptionClassifier}. The classifier determines
 * whether to log the exception or rethrow it. The keys in the classifier must be the same as the static contants in
 * this class.
 * 
 * @author Dave Syer
 * 
 */
public class LogOrRethrowExceptionHandler implements ExceptionHandler {

	/**
	 * Key for {@link ExceptionClassifier} signalling that the throwable should be rethrown. If the throwable is not a
	 * RuntimeException it is wrapped in a {@link RepeatException}.
	 */
	public static final String RETHROW = "rethrow";

	/**
	 * Key for {@link ExceptionClassifier} signalling that the throwable should be logged at debug level.
	 */
	public static final String DEBUG = "debug";

	/**
	 * Key for {@link ExceptionClassifier} signalling that the throwable should be logged at warn level.
	 */
	public static final String WARN = "warn";

	/**
	 * Key for {@link ExceptionClassifier} signalling that the throwable should be logged at error level.
	 */
	public static final String ERROR = "error";

	protected final Log logger = LogFactory.getLog(LogOrRethrowExceptionHandler.class);

	private ExceptionClassifier exceptionClassifier = new ExceptionClassifierSupport() {
		public Object classify(Throwable throwable) {
			return RETHROW;
		}
	};

	/**
	 * Setter for the {@link ExceptionClassifier} used by this handler. The default is to map all throwable instances to
	 * {@value #RETHROW}.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Classify the throwables and decide whether to rethrow based on the result. The context is not used.
	 * 
	 * @throws RuntimeException
	 * 
	 * @see {@link ExceptionHandler#handleException(RepeatContext, Throwable)}
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {

		Object key = exceptionClassifier.classify(throwable);
		if (ERROR.equals(key)) {
			logger.error("Exception encountered in batch repeat.", throwable);
		} else if (WARN.equals(key)) {
			logger.warn("Exception encountered in batch repeat.", throwable);
		} else if (DEBUG.equals(key) && logger.isDebugEnabled()) {
			logger.debug("Exception encountered in batch repeat.", throwable);
		} else if (RETHROW.equals(key)) {
			DefaultExceptionHandler.rethrow(throwable);
		} else {
			throw new IllegalStateException(
			        "Unclassified exception encountered.  Did you mean to classifiy this as 'rethrow'?");
		}

	}

}
