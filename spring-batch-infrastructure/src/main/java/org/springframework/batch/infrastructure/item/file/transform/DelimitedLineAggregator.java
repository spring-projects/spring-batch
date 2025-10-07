/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A {@link LineAggregator} implementation that converts an object into a delimited list
 * of strings. The default delimiter is a comma. An optional quote value can be set to add
 * surrounding quotes for each element of the list. Default is empty string, which means
 * not quotes.
 *
 * @author Dave Syer
 * @author Glenn Renfro
 */
public class DelimitedLineAggregator<T> extends ExtractorLineAggregator<T> {

	private String delimiter = ",";

	private String quoteCharacter = "";

	/**
	 * Public setter for the delimiter.
	 * @param delimiter the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Setter for the quote character.
	 * @since 5.1
	 * @param quoteCharacter the quote character to set
	 */
	public void setQuoteCharacter(String quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	@Override
	public String doAggregate(Object[] fields) {
		return Arrays.stream(fields)
			.map(field -> this.quoteCharacter + field + this.quoteCharacter)
			.collect(Collectors.joining(this.delimiter));
	}

}
