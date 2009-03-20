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
import java.util.Collection;
import java.util.Collections;

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
 * immediate termination of the {@link Step}. Because it would be impossible for
 * a general purpose policy to determine all the types of exceptions that should
 * be skipped from those that shouldn't, two lists are passed in, with all
 * of the exceptions that are 'fatal' and 'skippable'. The two lists are not
 * enforced to be exclusive, they are prioritized instead - exceptions that are
 * fatal will never be skipped, regardless whether the exception can also be
 * classified as skippable.
 * </p>
 * 
 * @author Ben Hale
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class LimitCheckingItemSkipPolicy implements SkipPolicy {

	private final int skipLimit;

	private final Classifier<Throwable, Boolean> fatalExceptionClassifier;

	private final Classifier<Throwable, Boolean> skippableExceptionClassifier;

	/**
	 * Convenience constructor that assumes all exception types are skippable
	 * and none are fatal.
	 * @param skipLimit the number of exceptions allowed to skip
	 */
	public LimitCheckingItemSkipPolicy(int skipLimit) {
		this(skipLimit, Collections.<Class<? extends Throwable>> singleton(Exception.class), Collections
				.<Class<? extends Throwable>> emptyList());
	}

	/**
	 * 
	 * @param skipLimit the number of skippable exceptions that are allowed to
	 * be skipped
	 * @param skippableExceptions exception classes that can be skipped
	 * (non-critical)
	 * @param fatalExceptions exception classes that should never be skipped
	 */
	public LimitCheckingItemSkipPolicy(int skipLimit, Collection<Class<? extends Throwable>> skippableExceptions,
			Collection<Class<? extends Throwable>> fatalExceptions) {
		this(skipLimit, new BinaryExceptionClassifier(skippableExceptions), new BinaryExceptionClassifier(
				fatalExceptions));
	}

	/**
	 * 
	 * @param skipLimit the number of skippable exceptions that are allowed to
	 * be skipped
	 * @param skippableExceptionClassifier exception classifier for those that
	 * can be skipped (non-critical)
	 * @param fatalExceptionClassifier exception classifier for classes that
	 * should never be skipped
	 */
	public LimitCheckingItemSkipPolicy(int skipLimit, Classifier<Throwable, Boolean> skippableExceptionClassifier,
			Classifier<Throwable, Boolean> fatalExceptionClassifier) {
		this.skipLimit = skipLimit;
		this.skippableExceptionClassifier = skippableExceptionClassifier;
		this.fatalExceptionClassifier = fatalExceptionClassifier;
	}

	/**
	 * Given the provided exception and skip count, determine whether or not
	 * processing should continue for the given exception. If the exception is
	 * not within the list of 'skippable exceptions' or belongs to the list of
	 * 'fatal exceptions', false will be returned. If the exception is within
	 * the skippable list (and not in the fatal list), and {@link StepExecution}
	 * skipCount is greater than the skipLimit, then a
	 * {@link SkipLimitExceededException} will be thrown.
	 */
	public boolean shouldSkip(Throwable t, int skipCount) {
		if (fatalExceptionClassifier.classify(t)) {
			return false;
		}
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
