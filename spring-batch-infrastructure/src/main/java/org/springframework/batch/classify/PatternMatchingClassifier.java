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
package org.springframework.batch.classify;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.support.PatternMatcher;

/**
 * A {@link Classifier} that maps from String patterns with wildcards to a set
 * of values of a given type. An input String is matched with the most specific
 * pattern possible to the corresponding value in an input map. A default value
 * should be specified with a pattern key of "*".
 * 
 * @author Dave Syer
 * 
 */
public class PatternMatchingClassifier<T> implements Classifier<String, T> {

	private PatternMatcher<T> values;

	/**
	 * Default constructor. Use the setter or the other constructor to create a
	 * sensible classifier, otherwise all inputs will cause an exception.
	 */
	public PatternMatchingClassifier() {
		this(new HashMap<String, T>());
	}

	/**
	 * Create a classifier from the provided map. The keys are patterns, using
	 * '?' as a single character and '*' as multi-character wildcard.
	 * 
	 * @param values
	 */
	public PatternMatchingClassifier(Map<String, T> values) {
		super();
		this.values = new PatternMatcher<T>(values);
	}

	/**
	 * A map from pattern to value
	 * @param values the pattern map to set
	 */
	public void setPatternMap(Map<String, T> values) {
		this.values = new PatternMatcher<T>(values);
	}

	/**
	 * Classify the input by matching it against the patterns provided in
	 * {@link #setPatternMap(Map)}. The most specific pattern that matches will
	 * be used to locate a value.
	 * 
	 * @return the value matching the most specific pattern possible
	 * 
	 * @throws IllegalStateException if no matching value is found.
	 */
	public T classify(String classifiable) {
		T value = values.match(classifiable);
		return value;
	}

}
