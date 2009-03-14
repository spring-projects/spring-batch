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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.classify.SubclassClassifier;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextCounter;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link ExceptionHandler} that rethrows when exceptions of a
 * given type reach a threshold. Requires an {@link Classifier} that maps
 * exception types to unique keys, and also a map from those keys to threshold
 * values (Integer type).
 * 
 * @author Dave Syer
 * 
 */
public class RethrowOnThresholdExceptionHandler implements ExceptionHandler {

	protected static final IntegerHolder ZERO = new IntegerHolder(0);

	protected final Log logger = LogFactory.getLog(RethrowOnThresholdExceptionHandler.class);

	private Classifier<? super Throwable, IntegerHolder> exceptionClassifier = new Classifier<Throwable, IntegerHolder>() {
		public RethrowOnThresholdExceptionHandler.IntegerHolder classify(Throwable classifiable) {
			return ZERO;
		}
	};

	private boolean useParent = false;

	/**
	 * Flag to indicate the the exception counters should be shared between
	 * sibling contexts in a nested batch. Default is false.
	 * 
	 * @param useParent true if the parent context should be used to store the
	 * counters.
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
	}

	/**
	 * A map from exception classes to a threshold value of type Integer.
	 * 
	 * @param thresholds the threshold value map.
	 */
	public void setThresholds(Map<Class<? extends Throwable>, Integer> thresholds) {
		Map<Class<? extends Throwable>, IntegerHolder> typeMap = new HashMap<Class<? extends Throwable>, IntegerHolder>();
		for (Entry<Class<? extends Throwable>, Integer> entry : thresholds.entrySet()) {
			typeMap.put(entry.getKey(), new IntegerHolder(entry.getValue()));
		}
		exceptionClassifier = new SubclassClassifier<Throwable, IntegerHolder>(typeMap, ZERO);
	}

	/**
	 * Classify the throwables and decide whether to re-throw based on the
	 * result. The context is used to accumulate the number of exceptions of the
	 * same type according to the classifier.
	 * 
	 * @throws Throwable
	 * @see ExceptionHandler#handleException(RepeatContext, Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {

		IntegerHolder key = exceptionClassifier.classify(throwable);

		RepeatContextCounter counter = getCounter(context, key);
		counter.increment();
		int count = counter.getCount();
		int threshold = key.getValue();
		if (count > threshold) {
			throw throwable;
		}

	}

	private RepeatContextCounter getCounter(RepeatContext context, IntegerHolder key) {
		String attribute = RethrowOnThresholdExceptionHandler.class.getName() + "." + key;
		// Creates a new counter and stores it in the correct context:
		return new RepeatContextCounter(context, attribute, useParent);
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class IntegerHolder {

		private final int value;

		/**
		 * @param value
		 */
		public IntegerHolder(int value) {
			this.value = value;
		}

		/**
		 * Public getter for the value.
		 * @return the value
		 */
		public int getValue() {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ObjectUtils.getIdentityHexString(this) + "." + value;
		}

	}

}
