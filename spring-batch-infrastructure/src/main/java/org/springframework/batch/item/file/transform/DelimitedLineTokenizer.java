/*
 * Copyright 2006-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
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
 * @author Olivier Bourgain
 */
public class DelimitedLineTokenizer extends AbstractLineTokenizer
	implements InitializingBean {
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

    private String escapedQuoteString;

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
	 * @param delimiter the desired delimiter.  This is required
	 */
	public DelimitedLineTokenizer(String delimiter) {
		Assert.notNull(delimiter, "A delimiter is required");
		Assert.state(!delimiter.equals(String.valueOf(DEFAULT_QUOTE_CHARACTER)), "[" + DEFAULT_QUOTE_CHARACTER
				+ "] is not allowed as delimiter for tokenizers.");

		this.delimiter = delimiter;
		setQuoteCharacter(DEFAULT_QUOTE_CHARACTER);
	}

	/**
	 * Setter for the delimiter character.
	 *
	 * @param delimiter the String used as a delimiter
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
	public void setIncludedFields(int... includedFields) {
		this.includedFields = new HashSet<>();
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
        this.escapedQuoteString = "" + quoteCharacter + quoteCharacter;
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

		List<String> tokens = new ArrayList<>();

		// line is never null in current implementation
		// line is checked in parent: AbstractLineTokenizer.tokenize()
		boolean inQuoted = false;
		int lastCut = 0;
		int length = line.length();
		int fieldCount = 0;
		int endIndexLastDelimiter = -1;

		for (int i = 0; i < length; i++) {
			char currentChar = line.charAt(i);
			boolean isEnd = (i == (length - 1));

            boolean isDelimiter = endsWithDelimiter(line, i, endIndexLastDelimiter);

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
                    String value =
                            substringWithTrimmedWhitespaceAndQuotesIfQuotesPresent(line, lastCut, endPosition);
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
     * Trim any leading or trailing quotes (and any leading or trailing
     * whitespace before or after the quotes) from within the specified character
     * array beginning at the specified offset index for the specified count.
     * <p/>
     * Quotes are escaped with double instances of the quote character.
     *
     * @param line  the string
     * @param offset index from which to begin extracting substring
     * @param count  length of substring
     * @return a substring from the specified offset within the character array
     * with any leading or trailing whitespace trimmed.
     * @see String#trim()
     */
    private String substringWithTrimmedWhitespaceAndQuotesIfQuotesPresent(String line, int offset, int count) {
        int start = offset;
        int len = count;

        while ((start < (start + len - 1)) && (line.charAt(start) <= ' ')) {
            start++;
            len--;
        }

        while ((start < (start + len)) && ((start + len - 1 < line.length()) && (line.charAt(start + len - 1) <= ' '))) {
            len--;
        }

        String value;

        if ((line.length() >= 2) && isQuoteCharacter(line.charAt(start)) && isQuoteCharacter(line.charAt(start + len - 1))) {
			int beginIndex = start + 1;
			int endIndex = len - 2;
			value = line.substring(beginIndex, beginIndex + endIndex);
            if (value.contains(escapedQuoteString)) {
                value = StringUtils.replace(value, escapedQuoteString, quoteString);
            }
        }
        else {
            value = line.substring(offset, offset + count);
        }

        return value;
    }

    /**
     * Do the character(s) in the specified array end, at the specified end
     * index, with the delimiter character(s)?
     * <p/>
     * Checks that the specified end index is sufficiently greater than the
     * specified previous delimiter end index to warrant trying to match
     * another delimiter.  Also checks that the specified end index is
     * sufficiently large to be able to match the length of a delimiter.
     *
     * @param line     the string
     * @param end      the index in up to which the delimiter should be matched
     * @param previous the index of the end of the last delimiter
     * @return <code>true</code> if the character(s) from the specified end
     * match the delimiter character(s), otherwise false
     * @see DelimitedLineTokenizer#DelimitedLineTokenizer(String)
     */
    private boolean endsWithDelimiter(String line, int end, int previous) {
        boolean result = false;

        if (end - previous >= delimiter.length()) {
            if (end >= delimiter.length() - 1) {
                result = true;
                for (int j = 0; j < delimiter.length() && (((end - delimiter.length() + 1) + j) < line.length()); j++) {
                    if (delimiter.charAt(j) != line.charAt((end - delimiter.length() + 1) + j)) {
                        result = false;
                    }
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

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(this.delimiter, "A delimiter is required");
	}
}
