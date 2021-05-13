/*
 * Copyright 2006-2007 the original author or authors.
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

import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link LineAggregator} implementation that converts an object into a
 * delimited list of strings. The default delimiter is a comma.  An optional
 * quote value can be set to add surrounding quotes for each of elements of the list.
 * Default is null, which means not quotes.
 * 
 * @author Dave Syer
 * @author Glenn Renfro
 * 
 */
public class DelimitedLineAggregator<T> extends ExtractorLineAggregator<T> {

	private String delimiter = ",";

	private String quote;

	/**
	 * Public setter for the delimiter.
	 * @param delimiter the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public String doAggregate(Object[] fields) {
		return arrayToDelimitedString(fields, this.delimiter);
	}

	public String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		} else if (arr.length == 1) {
			return quote(handleEmbeddedQuote(ObjectUtils.nullSafeToString(arr[0])));
		} else {
			StringJoiner sj = new StringJoiner(delim);
			Object[] var3 = arr;
			int var4 = arr.length;

			for(int var5 = 0; var5 < var4; ++var5) {
				Object elem = var3[var5];
				sj.add(quote(handleEmbeddedQuote(String.valueOf(elem))));
			}

			return sj.toString();
		}
	}

	/**
	 * Public setter for the quote.
	 * If quote string is contained in the body of the entry it will be replaced
	 * with a triple quote. For example a quote of % contained in the element,
	 * will be replaced with %%%.
	 * @param quote the quote to set
	 */
	public void setQuote(String quote) {
		this.quote = quote;
	}

	private String quote(final String str) {
		String result = str;
		if (StringUtils.hasText(str) && StringUtils.hasText(this.quote)) {
			result =  new StringBuffer().append(this.quote).append(str).append(this.quote).toString();
		}
		return result;
	}

	private String handleEmbeddedQuote(final String str) {
		String result = str;
		if (StringUtils.hasText(str) && StringUtils.hasText(this.quote)) {
			result =  str.replace(this.quote, new StringBuffer().append(this.quote).append(this.quote).append(this.quote));
		}
		return result;
	}
}
