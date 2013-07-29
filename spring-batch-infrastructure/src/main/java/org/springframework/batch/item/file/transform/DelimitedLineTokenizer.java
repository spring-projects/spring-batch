/*
 * Copyright 2006-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link LineTokenizer} implementation that splits the input String on a
 * configurable delimiter. This implementation also supports the use of an
 * escape character to escape delimiters and line endings.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Michael Minella
 */
public class DelimitedLineTokenizer extends AbstractLineTokenizer {
	/**
	 * Convenient constant for the common case of a tab delimiter.
	 */
	public static final String DELIMITER_TAB = "\t";

	/**
	 * Convenient constant for the common case of a comma delimiter.
	 */
	public static final String DELIMITER_COMMA = ",";

	/**
	 * Convenient constant for the common case of a " character used to escape
	 * delimiters or line endings.
	 */
	public static final char DEFAULT_QUOTE_CHARACTER = '"';

	// the delimiter character used when reading input.
	private String delimiter;

	private char quoteCharacter = DEFAULT_QUOTE_CHARACTER;

	private String quoteString;

	private Collection<Integer> includedFields = null;

	/**
	 * Create a new instance of the {@link DelimitedLineTokenizer} class for the
	 * common case where the delimiter is a {@link #DELIMITER_COMMA comma}.
	 *
	 * @see #DelimitedLineTokenizer(String)
	 * @see #DELIMITER_COMMA
	 */
	public DelimitedLineTokenizer() {
		this(DELIMITER_COMMA);
	}

	/**
	 * Create a new instance of the {@link DelimitedLineTokenizer} class.
	 *
	 * @param delimiter the desired delimiter
	 */
	public DelimitedLineTokenizer(String delimiter) {
		Assert.state(!delimiter.equals(String.valueOf(DEFAULT_QUOTE_CHARACTER)), "[" + DEFAULT_QUOTE_CHARACTER
				+ "] is not allowed as delimiter for tokenizers.");

		this.delimiter = delimiter;
		setQuoteCharacter(DEFAULT_QUOTE_CHARACTER);
	}

	/**
	 * Setter for the delimiter character.
	 *
	 * @param delimiter
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * The fields to include in the output by position (starting at 0). By
	 * default all fields are included, but this property can be set to pick out
	 * only a few fields from a larger set. Note that if field names are
	 * provided, their number must match the number of included fields.
	 *
	 * @param includedFields the included fields to set
	 */
	public void setIncludedFields(int[] includedFields) {
		this.includedFields = new HashSet<Integer>();
		for (int i : includedFields) {
			this.includedFields.add(i);
		}
	}

	/**
	 * Public setter for the quoteCharacter. The quote character can be used to
	 * extend a field across line endings or to enclose a String which contains
	 * the delimiter. Inside a quoted token the quote character can be used to
	 * escape itself, thus "a""b""c" is tokenized to a"b"c.
	 *
	 * @param quoteCharacter the quoteCharacter to set
	 *
	 * @see #DEFAULT_QUOTE_CHARACTER
	 */
	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
		this.quoteString = "" + quoteCharacter;
	}

	/**
	 * Yields the tokens resulting from the splitting of the supplied
	 * <code>line</code>.
	 *
	 * @param line the line to be tokenized
	 *
	 * @return the resulting tokens
	 */
	@Override
	protected List<String> doTokenize(String line) {

		List<String> tokens = new ArrayList<String>();

		// line is never null in current implementation
		// line is checked in parent: AbstractLineTokenizer.tokenize()
		char[] chars = line.toCharArray();
		boolean inQuoted = false;
		int lastCut = 0;
		int length = chars.length;
		int fieldCount = 0;
		int endIndexLastDelimiter = -1;

		for (int i = 0; i < length; i++) {
			char currentChar = chars[i];
			boolean isEnd = (i == (length - 1));

			boolean isDelimiter = isDelimiter(chars, i, delimiter, endIndexLastDelimiter);

			if ((isDelimiter && !inQuoted) || isEnd) {
				endIndexLastDelimiter = i;
				int endPosition = (isEnd ? (length - lastCut) : (i - lastCut));

				if (isEnd && isDelimiter) {
					endPosition = endPosition - delimiter.length();
				}
				else if (!isEnd){
					endPosition = (endPosition - delimiter.length()) + 1;
				}

				if (includedFields == null || includedFields.contains(fieldCount)) {
					String value = maybeStripQuotes(new String(chars, lastCut, endPosition));
					tokens.add(value);
				}

				fieldCount++;

				if (isEnd && (isDelimiter)) {
					if (includedFields == null || includedFields.contains(fieldCount)) {
						tokens.add("");
					}
					fieldCount++;
				}

				lastCut = i + 1;
			}
			else if (isQuoteCharacter(currentChar)) {
				inQuoted = !inQuoted;
			}

		}

		return tokens;
	}

	/**
	 * If the string is quoted strip (possibly with whitespace outside the
	 * quotes (which will be stripped), replace escaped quotes inside the
	 * string. Quotes are escaped with double instances of the quote character.
	 *
	 * @param string
	 * @return the same string but stripped and unescaped if necessary
	 */
	private String maybeStripQuotes(String string) {
		String value = string.trim();
		if (isQuoted(value)) {
			value = StringUtils.replace(value, "" + quoteCharacter + quoteCharacter, "" + quoteCharacter);
			int endLength = value.length() - 1;
			// used to deal with empty quoted values
			if (endLength == 0) {
				endLength = 1;
			}
			value = value.substring(1, endLength);
			return value;
		}
		return string;
	}

	/**
	 * Is this string surrounded by quote characters?
	 *
	 * @param value
	 * @return true if the value starts and ends with the
	 * {@link #quoteCharacter}
	 */
	private boolean isQuoted(String value) {
		if (value.startsWith(quoteString) && value.endsWith(quoteString)) {
			return true;
		}
		return false;
	}

	/**
	 * Is the supplied character the delimiter character?
	 *
	 * @param c the character to be checked
	 * @return <code>true</code> if the supplied character is the delimiter
	 * character
	 * @see DelimitedLineTokenizer#DelimitedLineTokenizer(char)
	 */
	private boolean isDelimiter(char[] chars, int i, String token, int endIndexLastDelimiter) {
		boolean result = false;

		if(i-endIndexLastDelimiter >= delimiter.length()) {
			if(i >= token.length() - 1) {
				String end = new String(chars, (i-token.length()) + 1, token.length());
				if(token.equals(end)) {
					result = true;
				}
			}
		}

		return result;
	}

	/**
	 * Is the supplied character a quote character?
	 *
	 * @param c the character to be checked
	 * @return <code>true</code> if the supplied character is an quote character
	 * @see #setQuoteCharacter(char)
	 */
	protected boolean isQuoteCharacter(char c) {
		return c == quoteCharacter;
	}
}
