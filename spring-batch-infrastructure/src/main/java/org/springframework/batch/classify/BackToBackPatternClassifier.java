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

import java.util.Map;

/**
 * A special purpose {@link Classifier} with easy configuration options for
 * mapping from one arbitrary type of object to another via a pattern matcher.
 * 
 * @author Dave Syer
 * 
 */
public class BackToBackPatternClassifier<C, T> implements Classifier<C, T> {

	private Classifier<C, String> router;

	private Classifier<String, T> matcher;

	/**
	 * Default constructor, provided as a convenience for people using setter
	 * injection.
	 */
	public BackToBackPatternClassifier() {
	}

	/**
	 * Set up a classifier with input to the router and output from the matcher.
	 * 
	 * @param router see {@link #setRouterDelegate(Object)}
	 * @param matcher see {@link #setMatcherMap(Map)}
	 */
	public BackToBackPatternClassifier(Classifier<C, String> router, Classifier<String, T> matcher) {
		super();
		this.router = router;
		this.matcher = matcher;
	}

	/**
	 * A convenience method for creating a pattern matching classifier for the
	 * matcher component.
	 * 
	 * @param map maps pattern keys with wildcards to output values
	 */
	public void setMatcherMap(Map<String, T> map) {
		this.matcher = new PatternMatchingClassifier<T>(map);
	}

	/**
	 * A convenience method of creating a router classifier based on a plain old
	 * Java Object. The object provided must have precisely one public method
	 * that either has the @Classifier annotation or accepts a single argument
	 * and outputs a String. This will be used to create an input classifier for
	 * the router component. <br/>
	 * 
	 * @param delegate the delegate object used to create a router classifier
	 */
	public void setRouterDelegate(Object delegate) {
		this.router = new ClassifierAdapter<C,String>(delegate);
	}

	/**
	 * Classify the input and map to a String, then take that and put it into a
	 * pattern matcher to match to an output value.
	 */
	public T classify(C classifiable) {
		return matcher.classify(router.classify(classifiable));
	}

}
