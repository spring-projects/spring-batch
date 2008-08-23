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

package org.springframework.batch.support;

/**
 * Interface for a classifier of exceptions.
 * 
 * @author Dave Syer
 * 
 */
public interface ExceptionClassifier<T,C> {

	/**
	 * Get a default value, normally the same as would be returned by
	 * {@link #classify(Object)} with null argument.
	 * 
	 * @return the default value.
	 */
	T getDefault();

	/**
	 * Classify the given object and return a non-null object. The return
	 * type depends on the implementation but typically would be a key in a map
	 * which the client maintains.
	 * 
	 * @param classifiable the input object. Can be null.
	 * @return an object.
	 */
	T classify(C classifiable);

}
