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

import org.springframework.util.StringUtils;

/**
 * A {@link LineAggregator} implementation that converts an object into a
 * delimited list of strings. The default delimiter is a comma.
 * 
 * @author Dave Syer
 * 
 */
public class DelimitedLineAggregator<T> extends ExtractorLineAggregator<T> {

	private String delimiter = ",";

	/**
	 * Public setter for the delimiter.
	 * @param delimiter the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public String doAggregate(Object[] fields) {
		return StringUtils.arrayToDelimitedString(fields, this.delimiter);
	}

}
