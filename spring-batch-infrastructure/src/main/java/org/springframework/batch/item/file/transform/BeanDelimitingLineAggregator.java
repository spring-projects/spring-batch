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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * This is a delimited line aggregator for a java bean. Given an array of
 * property names, it will reflectively call getters on the item to aggregate.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class BeanDelimitingLineAggregator<T> implements LineAggregator<T>, InitializingBean {

	private BeanWrapperFieldExtractor<T> extractor;

	private DelimitedLineAggregator<Object> delimitedLineAggregator = new DelimitedLineAggregator<Object>();

	/**
	 * @param names names of properties of the aggregated item that will be
	 * included in the resulting string. Must not be <code>null</code>.
	 */
	public void setNames(String[] names) {
		extractor = new BeanWrapperFieldExtractor<T>();
		extractor.setNames(names);
		extractor.afterPropertiesSet();
	}

	/**
	 * @param delimiter used to separate property values in the
	 * {@link #aggregate(Object)} result
	 */
	public void setDelimiter(String delimiter) {
		this.delimitedLineAggregator.setDelimiter(delimiter);
	}

	public String aggregate(T item) {
		Object[] fields = this.extractor.extract(item);
		return this.delimitedLineAggregator.aggregate(fields);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.extractor, "The 'names' property must be set.");
	}
}
