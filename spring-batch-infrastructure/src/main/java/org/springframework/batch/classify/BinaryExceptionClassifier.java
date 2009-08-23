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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Classifier} for exceptions that has only two classes (true and
 * false). Classifies objects according to their inheritance relation with the
 * supplied types. If the object to be classified is one of the provided types,
 * or is a subclass of one of the types, then the non-default value is returned
 * (usually true).
 * 
 * @see SubclassClassifier
 * 
 * @author Dave Syer
 * 
 */
public class BinaryExceptionClassifier extends SubclassClassifier<Throwable, Boolean> {

	/**
	 * Create a binary exception classifier with the provided default value.
	 * 
	 * @param defaultValue defaults to false
	 */
	public BinaryExceptionClassifier(boolean defaultValue) {
		super(defaultValue);
	}

	/**
	 * Create a binary exception classifier with the provided classes and their
	 * subclasses. The mapped value for these exceptions will be the one
	 * provided (which will be the opposite of the default).
	 * 
	 * @param value
	 */
	public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses, boolean value) {
		this(!value);
		if (exceptionClasses != null) {
			Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
			for (Class<? extends Throwable> type : exceptionClasses) {
				map.put(type, !getDefault());
			}
			setTypeMap(map);
		}
	}

	/**
	 * Create a binary exception classifier with the default value false and
	 * value mapping true for the provided classes and their subclasses.
	 */
	public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses) {
		this(exceptionClasses, true);
	}

	/**
	 * Create a binary exception classifier using the given classification map
	 * and a default classification of false.
	 * 
	 * @param typeMap
	 */
	public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap) {
		this(typeMap, false);
	}

	/**
	 * Create a binary exception classifier using the given classification map
	 * and a default classification of false.
	 * 
	 * @param typeMap
	 */
	public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap, boolean defaultValue) {
		super(typeMap, defaultValue);
	}

}
