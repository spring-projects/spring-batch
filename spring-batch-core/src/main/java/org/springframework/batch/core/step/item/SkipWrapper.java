/*
 * Copyright 2006-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.step.item;

import org.springframework.lang.Nullable;

/**
 * Wrapper for an item and its exception if it failed processing.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class SkipWrapper<T> {

	final private Throwable exception;

	final private T item;

	/**
	 * @param item the item being wrapped.
	 */
	public SkipWrapper(T item) {
		this(item, null);
	}

	/**
	 * @param e instance of {@link Throwable} that being wrapped.
	 */
	public SkipWrapper(Throwable e) {
		this(null, e);
	}

	public SkipWrapper(T item, @Nullable Throwable e) {
		this.item = item;
		this.exception = e;
	}

	/**
	 * Public getter for the exception.
	 * @return the exception
	 */
	@Nullable
	public Throwable getException() {
		return exception;
	}

	/**
	 * Public getter for the item.
	 * @return the item
	 */
	public T getItem() {
		return item;
	}

	@Override
	public String toString() {
		return String.format("[exception=%s, item=%s]", exception, item);
	}

}
