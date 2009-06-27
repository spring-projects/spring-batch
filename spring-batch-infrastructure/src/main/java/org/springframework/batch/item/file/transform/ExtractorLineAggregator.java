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

import org.springframework.util.Assert;

/**
 * An abstract {@link LineAggregator} implementation that utilizes a
 * {@link FieldExtractor} to convert the incoming object to an array of its
 * parts. Extending classes must decide how those parts will be aggregated
 * together.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class ExtractorLineAggregator<T> implements LineAggregator<T> {

	private FieldExtractor<T> fieldExtractor = new PassThroughFieldExtractor<T>();

	/**
	 * Public setter for the field extractor responsible for splitting an input
	 * object up into an array of objects. Defaults to
	 * {@link PassThroughFieldExtractor}.
	 * 
	 * @param fieldExtractor The field extractor to set
	 */
	public void setFieldExtractor(FieldExtractor<T> fieldExtractor) {
		this.fieldExtractor = fieldExtractor;
	}

	/**
	 * Extract fields from the given item using the {@link FieldExtractor} and
	 * then aggregate them. Any null field returned by the extractor will be
	 * replaced by an empty String. Null items are not allowed.
	 * 
	 * @see org.springframework.batch.item.file.transform.LineAggregator#aggregate(java.lang.Object)
	 */
	public String aggregate(T item) {
		Assert.notNull(item);
		Object[] fields = this.fieldExtractor.extract(item);

		//
		// Replace nulls with empty strings
		//
		Object[] args = new Object[fields.length];
		for (int i = 0; i < fields.length; i++) {
			if (fields[i] == null) {
				args[i] = "";
			}
			else {
				args[i] = fields[i];
			}
		}

		return this.doAggregate(args);
	}

	/**
	 * Aggregate provided fields into single String.
	 * 
	 * @param fields An array of the fields that must be aggregated
	 * @return aggregated string
	 */
	protected abstract String doAggregate(Object[] fields);
}
