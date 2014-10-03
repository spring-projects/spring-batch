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

import org.springframework.util.ObjectUtils;

/**
 * A {@link LineAggregator} implementation that converts an array of object into
 * a quoted delimited list of strings. The default delimiter is a comma, default
 * quote character a quote.
 * 
 * The aggregator adheres to RFC 4180 and will wrap strings that contain the
 * delimiter, quote or new line character in quotes. Quotes in the string are
 * replaced with double quotes.
 * 
 * @author Dave Syer
 * 
 * @author M.P. Korstanje
 * 
 */
public class DelimitedLineAggregator<T> extends ExtractorLineAggregator<T> {

	public static final char DEFAULT_QUOTE = '"';
	public static final String DEFAULT_DELIMITER = ",";
	public static final char DEFAULT_NEWLINE = '\n';

	private String delimiter = DEFAULT_DELIMITER;
	private String quoteString = "" + DEFAULT_QUOTE;
	private String newLine = "" + DEFAULT_NEWLINE;

	/**
	 * Public setter for quote character
	 * 
	 * @param newLine
	 *            the quote to set
	 */
	public void setQoute(char quote) {
		this.quoteString = "" + quote;
	}

	/**
	 * Public setter for the new line.
	 * 
	 * @param newLine
	 *            the newLine to set
	 */

	public void setNewLine(char newLine) {
		this.newLine = "" + newLine;
	}

	/**
	 * Public setter for the delimiter.
	 * 
	 * @param delimiter
	 *            the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	@Override
	public String doAggregate(Object[] fields) {

		if (ObjectUtils.isEmpty(fields)) {
			return "";
		}
		if (fields.length == 1) {
			return wrapQoutes(fields[0]);
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fields.length; i++) {
			if (i > 0) {
				sb.append(delimiter);
			}
			sb.append(wrapQoutes(fields[i]));

		}
		return sb.toString();
	}

	private String wrapQoutes(Object object) {

		String string = ObjectUtils.nullSafeToString(object);

		if (isQouteNeeded(string)) {
			return quoteString + string.replaceAll(quoteString, quoteString+quoteString)
					+ quoteString;
		}

		return string;

	}

	private boolean isQouteNeeded(String string) {
		return string.contains(quoteString) || string.contains(delimiter)
				|| string.contains(newLine);
	}

}
