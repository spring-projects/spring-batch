/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.item;

import org.jspecify.annotations.Nullable;

/**
 * Wrapper for an item and its exception if it failed processing.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @deprecated since 6.0 with no replacement. Scheduled for removal in 7.0.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class SkipWrapper<T> {

	private final @Nullable Throwable exception;

	private final @Nullable T item;

	/**
	 * @param item the item being wrapped.
	 */
	public SkipWrapper(T item) {
		this(item, null);
	}

	public SkipWrapper(@Nullable T item, @Nullable Throwable e) {
		this.item = item;
		this.exception = e;
	}

	/**
	 * Public getter for the exception.
	 * @return the exception
	 */
	public @Nullable Throwable getException() {
		return exception;
	}

	/**
	 * Public getter for the item.
	 * @return the item
	 */
	public @Nullable T getItem() {
		return item;
	}

	@Override
	public String toString() {
		return String.format("[exception=%s, item=%s]", exception, item);
	}

}
