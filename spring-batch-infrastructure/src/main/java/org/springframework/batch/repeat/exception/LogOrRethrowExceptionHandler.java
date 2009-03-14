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
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.classify.ClassifierSupport;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;

/**
 * Implementation of {@link ExceptionHandler} based on an {@link Classifier}.
 * The classifier determines whether to log the exception or rethrow it. The
 * keys in the classifier must be the same as the static enum in this class.
 * 
 * @author Dave Syer
 * 
 */
public class LogOrRethrowExceptionHandler implements ExceptionHandler {

	/**
	 * Logging levels for the handler.
	 * 
	 * @author Dave Syer
	 * 
	 */
	public static enum Level {

		/**
		 * Key for {@link Classifier} signalling that the throwable should be
		 * rethrown. If the throwable is not a RuntimeException it is wrapped in
		 * a {@link RepeatException}.
		 */
		RETHROW,

		/**
		 * Key for {@link Classifier} signalling that the throwable should be
		 * logged at debug level.
		 */
		DEBUG,

		/**
		 * Key for {@link Classifier} signalling that the throwable should be
		 * logged at warn level.
		 */
		WARN,

		/**
		 * Key for {@link Classifier} signalling that the throwable should be
		 * logged at error level.
		 */
		ERROR

	}

	protected final Log logger = LogFactory.getLog(LogOrRethrowExceptionHandler.class);

	private Classifier<Throwable, Level> exceptionClassifier = new ClassifierSupport<Throwable, Level>(Level.RETHROW);

	/**
	 * Setter for the {@link Classifier} used by this handler. The default is to
	 * map all throwable instances to {@link Level#RETHROW}.
	 * 
	 * @param exceptionClassifier the ExceptionClassifier to use
	 */
	public void setExceptionClassifier(Classifier<Throwable, Level> exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Classify the throwables and decide whether to rethrow based on the
	 * result. The context is not used.
	 * 
	 * @throws Throwable
	 * 
	 * @see ExceptionHandler#handleException(RepeatContext, Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {

		Level key = exceptionClassifier.classify(throwable);
		if (Level.ERROR.equals(key)) {
			logger.error("Exception encountered in batch repeat.", throwable);
		}
		else if (Level.WARN.equals(key)) {
			logger.warn("Exception encountered in batch repeat.", throwable);
		}
		else if (Level.DEBUG.equals(key) && logger.isDebugEnabled()) {
			logger.debug("Exception encountered in batch repeat.", throwable);
		}
		else if (Level.RETHROW.equals(key)) {
			throw throwable;
		}

	}

}
