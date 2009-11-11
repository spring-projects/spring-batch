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

package org.springframework.batch.item.file.separator;

import org.springframework.util.StringUtils;

/**
 * A {@link RecordSeparatorPolicy} that treats all lines as record endings, as
 * long as they do not have unterminated quotes, and do not end in a
 * continuation marker.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultRecordSeparatorPolicy extends SimpleRecordSeparatorPolicy {

	private static final String QUOTE = "\"";

	private static final String CONTINUATION = "\\";

	private String quoteCharacter = QUOTE;

	private String continuation = CONTINUATION;

	/**
	 * Default constructor.
	 */
	public DefaultRecordSeparatorPolicy() {
		this(QUOTE, CONTINUATION);
	}

	/**
	 * Convenient constructor with quote character as parameter.
	 */
	public DefaultRecordSeparatorPolicy(String quoteCharacter) {
		this(quoteCharacter, CONTINUATION);
	}

	/**
	 * Convenient constructor with quote character and continuation marker as
	 * parameters.
	 */
	public DefaultRecordSeparatorPolicy(String quoteCharacter, String continuation) {
		super();
		this.continuation = continuation;
		this.quoteCharacter = quoteCharacter;
	}

	/**
	 * Public setter for the quoteCharacter. Defaults to double quote mark.
	 * 
	 * @param quoteCharacter the quoteCharacter to set
	 */
	public void setQuoteCharacter(String quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	/**
	 * Public setter for the continuation. Defaults to back slash.
	 * 
	 * @param continuation the continuation to set
	 */
	public void setContinuation(String continuation) {
		this.continuation = continuation;
	}

	/**
	 * Return true if the line does not have unterminated quotes (delimited by
	 * "), and does not end with a continuation marker ('\'). The test for the
	 * continuation marker ignores whitespace at the end of the line.
	 * 
	 * @see org.springframework.batch.item.file.separator.RecordSeparatorPolicy#isEndOfRecord(java.lang.String)
	 */
	public boolean isEndOfRecord(String line) {
		return !isQuoteUnterminated(line) && !isContinued(line);
	}

	/**
	 * If we are in an unterminated quote, add a line separator. Otherwise
	 * remove the continuation marker (plus whitespace at the end) if it is
	 * there.
	 * 
	 * @see org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy#preProcess(java.lang.String)
	 */
	public String preProcess(String line) {
		if (isQuoteUnterminated(line)) {
			return line + "\n";
		}
		if (isContinued(line)) {
			return line.substring(0, line.lastIndexOf(continuation));
		}
		return line;
	}

	/**
	 * Determine if the current line (or buffered concatenation of lines)
	 * contains an unterminated quote, indicating that the record is continuing
	 * onto the next line.
	 * 
	 * @param result
	 * @return
	 */
	private boolean isQuoteUnterminated(String line) {
		return StringUtils.countOccurrencesOf(line, quoteCharacter) % 2 != 0;
	}

	/**
	 * Determine if the current line (or buffered concatenation of lines) ends
	 * with the continuation marker, indicating that the record is continuing
	 * onto the next line.
	 * 
	 * @param result
	 * @return
	 */
	private boolean isContinued(String line) {
		if (line == null) {
			return false;
		}
		return line.trim().endsWith(continuation);
	}
}
