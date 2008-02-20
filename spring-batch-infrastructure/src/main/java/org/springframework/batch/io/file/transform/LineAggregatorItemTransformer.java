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
package org.springframework.batch.io.file.transform;

import org.springframework.batch.io.file.mapping.DefaultFieldSet;
import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.batch.item.writer.ItemTransformer;

/**
 * An {@link ItemTransformer} that expects a String[] as input and delegates to
 * a {@link LineAggregator}.
 * 
 * @author Dave Syer
 * 
 */
public class LineAggregatorItemTransformer implements ItemTransformer {

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
	 * @see org.springframework.batch.item.writer.ItemTransformer#transform(java.lang.Object)
	 */
	public Object transform(Object item) throws Exception {
		return aggregator.aggregate(createFieldSet(item));
	}

	/**
	 * @param item
	 * @return
	 */
	protected FieldSet createFieldSet(Object item) {
		return new DefaultFieldSet((String[]) item);
	}
}
