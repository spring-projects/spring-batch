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

import java.util.Formatter;
import java.util.Locale;

import org.springframework.util.Assert;

/**
 * A {@link LineAggregator} implementation which produces a String by
 * aggregating the provided item via the {@link Formatter} syntax.</br>
 * 
 * @see Formatter
 * 
 * @author Dave Syer
 */
public class FormatterLineAggregator<T> implements LineAggregator<T> {

	private String format;

	private FieldExtractor<T> fieldExtractor = new PassThroughFieldExtractor<T>();

	private Locale locale = Locale.getDefault();

	private int maximumLength = 0;

	private int minimumLength = 0;

	/**
	 * Public setter for the minimum length of the formatted string. If this is
	 * not set the default is to allow any length.
	 * 
	 * @param minimumLength the minimum length to set
	 */
	public void setMinimumLength(int minimumLength) {
		this.minimumLength = minimumLength;
	}

	/**
	 * Public setter for the maximum length of the formatted string. If this is
	 * not set the default is to allow any length.
	 * @param maximumLength the maximum length to set
	 */
	public void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	/**
	 * Set the format string used to aggregate items.
	 * 
	 * @see Formatter
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Public setter for the field extractor responsible for splitting an input
	 * object up into an array of objects. Defaults to
	 * {@link PassThroughFieldExtractor}.
	 * 
	 * @param fieldExtractor the field extractor to set
	 */
	public void setFieldExtractor(FieldExtractor<T> fieldExtractor) {
		this.fieldExtractor = fieldExtractor;
	}

	/**
	 * Public setter for the locale.
	 * @param locale the locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Aggregate provided item into single line using specified format.
	 * 
	 * @param item data to be aggregated
	 * @return aggregated string
	 */
	public String process(T item) {

		Assert.notNull(item);
		Assert.notNull(format);

		Object[] args = fieldExtractor.extract(item);

		String value = String.format(locale, format, args);

		if (maximumLength > 0) {
			Assert.state(value.length() <= maximumLength, String.format("String overflowed in formatter -"
					+ " longer than %d characters: [%s", maximumLength, value));
		}

		if (minimumLength > 0) {
			Assert.state(value.length() >= minimumLength, String.format("String underflowed in formatter -"
					+ " shorter than %d characters: [%s", minimumLength, value));
		}

		return value;
	}
}
