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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.common.ExceptionClassifier;
import org.springframework.batch.common.ExceptionClassifierSupport;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextCounter;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ExceptionHandler} that rethrows when exceptions of a
 * given type reach a threshold. Requires an {@link ExceptionClassifier} that
 * maps exception types to unique keys, and also a map from those keys to
 * threshold values (Integer type).
 * 
 * @author Dave Syer
 * 
 */
public class RethrowOnThresholdExceptionHandler implements ExceptionHandler {

	protected final Log logger = LogFactory
			.getLog(RethrowOnThresholdExceptionHandler.class);

	private ExceptionClassifier exceptionClassifier = new ExceptionClassifierSupport();

	private Map thresholds = new HashMap();

	private boolean useParent = false;

	/**
	 * Flag to indicate the the exception counters should be shared between
	 * sibling contexts in a nested batch. Default is false.
	 * 
	 * @param useParent
	 *            true if the parent context should be used to store the
	 *            counters.
	 */
	public void setUseParent(boolean useParent) {
		this.useParent = useParent;
	}

	/**
	 * Set up the exception handler. Creates a default exception handler and
	 * threshold that maps all exceptions to a threshold of 0 - all exceptions
	 * are rethrown by default.
	 */
	public RethrowOnThresholdExceptionHandler() {
		super();
		thresholds.put(ExceptionClassifierSupport.DEFAULT, new Integer(0));
	}

	/**
	 * A map from classifier keys to a threshold value of type Integer. The keys
	 * are usually String literals, depending on the {@link ExceptionClassifier}
	 * implementation used.
	 * 
	 * @param thresholds
	 *            the threshold value map.
	 */
	public void setThresholds(Map thresholds) {
		for (Iterator iter = thresholds.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			if (!(entry.getKey() instanceof String)) {
				logger.warn("Key in thresholds map is not of type String: "
						+ entry.getKey());
			}
			Assert
					.state(
							entry.getValue() instanceof Integer,
							"Threshold value must be of type Integer.  "
									+ "Try using the value-type attribute if you care configuring this map via xml.");
		}
		this.thresholds = thresholds;
	}

	/**
	 * Setter for the {@link ExceptionClassifier} used by this handler. The
	 * default is to map all throwable instances to
	 * {@value ExceptionClassifierSupport#DEFAULT}, which are then mapped to a
	 * threshold of 0 by the {@link #setThresholds(Map)} map.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Classify the throwables and decide whether to rethrow based on the
	 * result. The context is used to accumulate the number of exceptions of the
	 * same type according to the classifier.
	 * 
	 * @throws Exception
	 * @see {@link ExceptionHandler#handleException(RepeatContext, Throwable)}
	 */
	public void handleException(RepeatContext context, Throwable throwable)
			throws RuntimeException {

		Object key = exceptionClassifier.classify(throwable);
		RepeatContextCounter counter = getCounter(context, key);
		counter.increment();
		int count = counter.getCount();
		Integer threshold = (Integer) thresholds.get(key);
		if (threshold == null || count > threshold.intValue()) {
			DefaultExceptionHandler.rethrow(throwable);
		}

	}

	private RepeatContextCounter getCounter(RepeatContext context, Object key) {
		String attribute = RethrowOnThresholdExceptionHandler.class + "."
				+ key.toString();
		// Creates a new counter and stores it in the correct context:
		return new RepeatContextCounter(context, attribute, useParent);
	}

}
