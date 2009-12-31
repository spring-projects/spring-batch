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
package org.springframework.batch.core.step.skip;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * <p>
 * {@link SkipPolicy} that determines whether or not reading should continue
 * based upon how many items have been skipped. This is extremely useful
 * behavior, as it allows you to skip records, but will throw a
 * {@link SkipLimitExceededException} if a set limit has been exceeded. For
 * example, it is generally advisable to skip {@link FlatFileParseException}s,
 * however, if the vast majority of records are causing exceptions, the file is
 * likely bad.
 * </p>
 * 
 * <p>
 * Furthermore, it is also likely that you only want to skip certain exceptions.
 * {@link FlatFileParseException} is a good example of an exception you will
 * likely want to skip, but a {@link FileNotFoundException} should cause
 * immediate termination of the {@link Step}. A {@link Classifier} is used to
 * determine whether a particular exception is skippable or not.
 * </p>
 * 
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author Dan Garrette
 */
public class LimitCheckingItemSkipPolicy implements SkipPolicy {

	private int skipLimit;

	private Classifier<Throwable, Boolean> skippableExceptionClassifier;

	/**
	 * Convenience constructor that assumes all exception types are fatal.
	 */
	public LimitCheckingItemSkipPolicy() {
		this(0, Collections.<Class<? extends Throwable>, Boolean> emptyMap());
	}

	/**
	 * @param skipLimit the number of skippable exceptions that are allowed to
	 * be skipped
	 * @param skippableExceptions exception classes that can be skipped
	 * (non-critical)
	 */
	public LimitCheckingItemSkipPolicy(int skipLimit, Map<Class<? extends Throwable>, Boolean> skippableExceptions) {
		this(skipLimit, new BinaryExceptionClassifier(skippableExceptions));
	}

	/**
	 * @param skipLimit the number of skippable exceptions that are allowed to
	 * be skipped
	 * @param skippableExceptionClassifier exception classifier for those that
	 * can be skipped (non-critical)
	 */
	public LimitCheckingItemSkipPolicy(int skipLimit, Classifier<Throwable, Boolean> skippableExceptionClassifier) {
		this.skipLimit = skipLimit;
		this.skippableExceptionClassifier = skippableExceptionClassifier;
	}

	/**
	 * The absolute number of skips (of skippable exceptions) that can be
	 * tolerated before a failure.
	 * 
	 * @param skipLimit the skip limit to set
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * The classifier that will be used to decide on skippability. If an
	 * exception classifies as "true" then it is skippable, and otherwise not.
	 * 
	 * @param skippableExceptionClassifier the skippableExceptionClassifier to
	 * set
	 */
	public void setSkippableExceptionClassifier(Classifier<Throwable, Boolean> skippableExceptionClassifier) {
		this.skippableExceptionClassifier = skippableExceptionClassifier;
	}

	/**
	 * Set up the classifier through a convenient map from throwable class to
	 * boolean (true if skippable).
	 * 
	 * @param skippableExceptions the skippable exceptions to set
	 */
	public void setSkippableExceptionMap(Map<Class<? extends Throwable>, Boolean> skippableExceptions) {
		this.skippableExceptionClassifier = new BinaryExceptionClassifier(skippableExceptions);
	}

	/**
	 * Given the provided exception and skip count, determine whether or not
	 * processing should continue for the given exception. If the exception is
	 * not classified as skippable in the classifier, false will be returned. If
	 * the exception is classified as skippable and {@link StepExecution}
	 * skipCount is greater than the skipLimit, then a
	 * {@link SkipLimitExceededException} will be thrown.
	 */
	public boolean shouldSkip(Throwable t, int skipCount) {
		if (skippableExceptionClassifier.classify(t)) {
			if (skipCount < skipLimit) {
				return true;
			}
			else {
				throw new SkipLimitExceededException(skipLimit, t);
			}
		}
		else {
			return false;
		}
	}

}
