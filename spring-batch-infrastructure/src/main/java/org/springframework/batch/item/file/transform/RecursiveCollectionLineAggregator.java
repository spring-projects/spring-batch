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

package org.springframework.batch.item.file.transform;

import java.util.Collection;

import org.springframework.util.Assert;

/**
 * An implementation of {@link LineAggregator} that concatenates a collection of items of
 * a common type with a line separator.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class RecursiveCollectionLineAggregator<T> implements LineAggregator<Collection<T>> {

	private String lineSeparator = System.lineSeparator();

	private LineAggregator<T> delegate = new PassThroughLineAggregator<>();

	/**
	 * Public setter for the {@link LineAggregator} to use on single items, that are not
	 * Strings. This can be used to strategise the conversion of collection and array
	 * elements to a String.<br>
	 * @param delegate the line aggregator to set. Defaults to a pass through.
	 */
	public void setDelegate(LineAggregator<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Set the line separator to use. Defaults to the System's line separator.
	 * @param lineSeparator the line separator to use. Must not be {@code null}.
	 * @since 5.2
	 */
	public void setLineSeparator(String lineSeparator) {
		Assert.notNull(lineSeparator, "The line separator must not be null");
		this.lineSeparator = lineSeparator;
	}

	@Override
	public String aggregate(Collection<T> items) {
		if (items.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (T value : items) {
			builder.append(delegate.aggregate(value)).append(lineSeparator);
		}
		return builder.delete(builder.length() - lineSeparator.length(), builder.length()).toString();
	}

}
