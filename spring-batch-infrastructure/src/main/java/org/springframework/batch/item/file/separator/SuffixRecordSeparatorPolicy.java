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


/**
 * A {@link RecordSeparatorPolicy} that looks for an exact match for a String at
 * the end of a line (e.g. a semicolon).
 * 
 * @author Dave Syer
 * 
 */
public class SuffixRecordSeparatorPolicy extends DefaultRecordSeparatorPolicy {

	/**
	 * Default value for record terminator suffix.
	 */
	public static final String DEFAULT_SUFFIX = ";";

	private String suffix = DEFAULT_SUFFIX;

	private boolean ignoreWhitespace = true;

	/**
	 * Lines ending in this terminator String signal the end of a record.
	 * 
	 * @param suffix
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Flag to indicate that the decision to terminate a record should ignore
	 * whitespace at the end of the line.
	 * 
	 * @param ignoreWhitespace
	 */
	public void setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.ignoreWhitespace = ignoreWhitespace;
	}

	/**
	 * Return true if the line ends with the specified substring. By default
	 * whitespace is trimmed before the comparison. Also returns true if the
	 * line is null, but not if it is empty.
	 * 
	 * @see org.springframework.batch.item.file.separator.RecordSeparatorPolicy#isEndOfRecord(java.lang.String)
	 */
	public boolean isEndOfRecord(String line) {
		if (line == null) {
			return true;
		}
		String trimmed = ignoreWhitespace ? line.trim() : line;
		return trimmed.endsWith(suffix);
	}
	
	/**
	 * Remove the suffix from the end of the record.
	 * 
	 * @see org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy#postProcess(java.lang.String)
	 */
	public String postProcess(String record) {
		if (record==null) {
			return null;
		}
		return record.substring(0, record.lastIndexOf(suffix));
	}

}
