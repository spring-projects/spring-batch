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

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ExceptionClassifier} that has only two classes of exception.
 * Provides convenient methods for setting up and querying the classification
 * with boolean return type.
 * 
 * @author Dave Syer
 * 
 */
public class BinaryExceptionClassifier extends ExceptionClassifierSupport {

	/**
	 * The classifier result for a non-default exception.
	 */
	public static final String NON_DEFAULT = "NON_DEFAULT";

	private SubclassExceptionClassifier delegate = new SubclassExceptionClassifier();

	/**
	 * Set the special exceptions. Any exception on the list, or subclasses
	 * thereof, will be classified as non-default.
	 * 
	 * @param exceptionClasses defaults to {@link Exception}.
	 */
	public final void setExceptionClasses(Class<?>[] exceptionClasses) {
		Map<Class<?>, Object> temp = new HashMap<Class<?>, Object>();
		for (int i = 0; i < exceptionClasses.length; i++) {
			temp.put(exceptionClasses[i], NON_DEFAULT);
		}
		this.delegate.setTypeMap(temp);
	}

	/**
	 * Convenience method to return boolean if the throwable is classified as
	 * default.
	 * 
	 * @param throwable the Throwable to classify
	 * @return true if it is default classified (i.e. not on the list provided
	 * in {@link #setExceptionClasses(Class[])}.
	 */
	public boolean isDefault(Throwable throwable) {
		return classify(throwable).equals(DEFAULT);
	}

	/**
	 * Returns either {@link ExceptionClassifierSupport#DEFAULT} or
	 * {@link #NON_DEFAULT} depending on the type of the throwable. If the type
	 * of the throwable or one of its ancestors is on the exception class list
	 * the classification is as {@link #NON_DEFAULT}.
	 * 
	 * @see #setExceptionClasses(Class[])
	 * @see ExceptionClassifierSupport#classify(Throwable)
	 */
	public Object classify(Throwable throwable) {
		return delegate.classify(throwable);
	}

}
