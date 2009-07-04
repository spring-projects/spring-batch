/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public abstract class AbstractExceptionThrowingItemHandlerStub<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private Collection<T> failures = Collections.emptyList();

	private Constructor<? extends Exception> exception;

	public AbstractExceptionThrowingItemHandlerStub() throws Exception {
		exception = SkippableRuntimeException.class.getConstructor(String.class);
	}

	public void setFailures(T... failures) {
		this.failures = new ArrayList<T>(Arrays.asList(failures));
	}

	public void setExceptionType(Class<? extends Exception> exceptionType) throws Exception {
		exception = exceptionType.getConstructor(String.class);
	}

	public void clearFailures() {
		failures.clear();
	}

	protected void checkFailure(T item) throws Exception {
		if (isFailure(item)) {
			throw exception.newInstance("Intended Failure: " + item);
		}
	}

	protected boolean isFailure(T item) {
		return this.failures.contains(item);
	}
}
