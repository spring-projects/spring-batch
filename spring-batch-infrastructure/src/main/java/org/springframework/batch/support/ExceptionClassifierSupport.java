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
 * Base class for {@link Classifier} implementations. Provides default
 * behaviour and some convenience members, like constants.
 * 
 * @author Dave Syer
 * 
 */
public class ExceptionClassifierSupport implements Classifier<Throwable,String> {

	/**
	 * Default classification key.
	 */
	public static final String DEFAULT = "default";

	/**
	 * Always returns the value of {@link #DEFAULT}.
	 * 
	 * @see org.springframework.batch.support.Classifier#classify(Object)
	 */
	public String classify(Throwable throwable) {
		return DEFAULT;
	}

	/**
	 * Wrapper for a call to {@link #classify(Throwable)} with argument null.
	 * 
	 * @see org.springframework.batch.support.Classifier#getDefault()
	 */
	public String getDefault() {
		return classify(null);
	}

}
