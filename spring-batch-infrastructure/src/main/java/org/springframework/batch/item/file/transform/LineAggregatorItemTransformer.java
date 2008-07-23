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

import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.transform.ItemTransformer;

/**
 * An {@link ItemTransformer} that expects a String[] as input and delegates to
 * a {@link LineAggregator}.
 * 
 * @author Dave Syer
 * 
 */
public class LineAggregatorItemTransformer<T> implements ItemTransformer<T, String> {

	private LineAggregator aggregator = new DelimitedLineAggregator();

	/**
	 * Public setter for the {@link LineAggregator}.
	 * @param aggregator the aggregator to set
	 */
	public void setAggregator(LineAggregator aggregator) {
		this.aggregator = aggregator;
	}

	/**
	 * Assume the item is an array of String (no check is made) and delegate to
	 * the aggregator.
	 * 
	 * @see org.springframework.batch.item.transform.ItemTransformer#transform(java.lang.Object)
	 */
	public String transform(T item) throws Exception {
		return aggregator.aggregate(createFieldSet(item));
	}

	/**
	 * Extension point for subclasses. The default implementation just attempts
	 * to cast the item to String[] and creates a {@link DefaultFieldSet} from
	 * it.
	 * 
	 * @param item an object (in this implementation of type String[]).
	 * @return a {@link FieldSet} representing the item
	 * 
	 * @throws ConversionException if the field set cannot be created
	 */
	protected FieldSet createFieldSet(T item) throws ConversionException {
		try {
			return new DefaultFieldSet((String[]) item);
		}
		catch (ClassCastException e) {
			throw new ConversionException(
					"Item must be of type String[] for conversion to FieldSet. " +
					"Consider overriding this method to specify a less generic algorithm.");
		}
	}
}
