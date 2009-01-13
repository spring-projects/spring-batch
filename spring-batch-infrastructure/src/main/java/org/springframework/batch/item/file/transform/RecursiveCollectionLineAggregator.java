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

package org.springframework.batch.item.file.transform;

import java.util.Collection;


/**
 * An implementation of {@link LineAggregator} that concatenates a collection of
 * items of a common type with the system line separator.
 * 
 * @author Dave Syer
 * 
 */
public class RecursiveCollectionLineAggregator<T> implements LineAggregator<Collection<T>> {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private LineAggregator<T> delegate = new PassThroughLineAggregator<T>();

	/**
	 * Public setter for the {@link LineAggregator} to use on single items, that
	 * are not Strings. This can be used to strategise the conversion of
	 * collection and array elements to a String.<br/>
	 * 
	 * @param delegate the line aggregator to set. Defaults to a pass through.
	 */
	public void setDelegate(LineAggregator<T> delegate) {
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.file.transform.LineAggregator#aggregate(java.lang.Object)
	 */
	public String aggregate(Collection<T> items) {
		StringBuilder builder = new StringBuilder();
		for (T value : items) {
			builder.append(delegate.aggregate(value) + LINE_SEPARATOR);
		}
		return builder.delete(builder.length()-LINE_SEPARATOR.length(),builder.length()).toString();
	}

}