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
package org.springframework.batch.execution.step.support;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.common.ExceptionClassifier;
import org.springframework.batch.common.SubclassExceptionClassifier;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.io.exception.FlatFileParsingException;

/**
 * <p>{@link ItemSkipPolicy} that determines whether or not reading should
 * continue based upon how many items have been skipped.  This is extremely
 * useful behavior, as it allows you to skip records, but will throw a
 * {@link SkipLimitExceededException} if a set limit has been exceeded.  For example,
 * it is generally advisable to skip {@link FlatFileParsingException}s, however, if 
 * the vast majority of records are causing exceptions, the file is likely bad.</p>
 * 
 * <p>Furthermore, it is also likely that you only want to skip certain exceptions.  
 * {@link FlatFileParsingException} is a good example of an exception you will likely
 * want to skip, but a {@link FileNotFoundException} should cause immediate termination
 * of the {@link Step}.  Because it would be impossible for a general purpose policy to
 * determine all the types of exceptions that should be skipped from those that shouldn't,
 * a list must be passed in, with all of the exceptions that are 'skippable'</p>
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class LimitCheckingItemSkipPolicy implements ItemSkipPolicy {

	/**
	 * Label for classifying skippable exceptions.
	 */
	private static final String SKIP = "skip";

	private final int skipLimit;
	
	private ExceptionClassifier exceptionClassifier;

	public LimitCheckingItemSkipPolicy(int skipLimit) {
		this(skipLimit, new ArrayList(0));
	}

	public LimitCheckingItemSkipPolicy(int skipLimit, List skippableExceptions) {
		this.skipLimit = skipLimit;
		SubclassExceptionClassifier exceptionClassifier = new SubclassExceptionClassifier();
		Map typeMap = new HashMap();
		for (Iterator iterator = skippableExceptions.iterator(); iterator.hasNext();) {
			Class throwable = (Class) iterator.next();
			typeMap.put(throwable, SKIP);
		}
		exceptionClassifier.setTypeMap(typeMap);
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Given the provided exception and StepExecution, determine whether or not
	 * processing should continue for the given exception.  If the exception
	 * is not within the list of 'skippable exceptions', false will be returned.
	 * If the exception is within the list, and {@link StepExecution} skipCount 
	 * is greater than the skipLimit, then a {@link SkipLimitExceededException}
	 * will be thrown.
	 */
	public boolean shouldSkip(Exception ex, StepContribution stepContribution){
		if(exceptionClassifier.classify(ex).equals(SKIP)){
			if(stepContribution.getSkipCount() < skipLimit){
				stepContribution.incrementSkipCount();
				return true;
			}
			else{
				throw new SkipLimitExceededException(skipLimit, ex);
			}
		}
		else{
			return false;
		}
	}
	
}
