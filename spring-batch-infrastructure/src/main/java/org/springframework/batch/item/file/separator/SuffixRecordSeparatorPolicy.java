/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.item.file.separator;

import org.jspecify.annotations.Nullable;

/**
 * A {@link RecordSeparatorPolicy} that looks for an exact match for a String at the end
 * of a line (e.g. a semicolon).
 *
 * @author Dave Syer
 * @author Stefano Cordio
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
	 * @param suffix suffix to indicate the end of a record
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Flag to indicate that the decision to terminate a record should ignore whitespace
	 * at the end of the line.
	 * @param ignoreWhitespace indicator
	 */
	public void setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.ignoreWhitespace = ignoreWhitespace;
	}

	/**
	 * Return true if the line ends with the specified substring. By default, whitespace
	 * is trimmed before the comparison.
	 *
	 * @see RecordSeparatorPolicy#isEndOfRecord(String)
	 */
	@Override
	public boolean isEndOfRecord(@Nullable String line) {
		if (line == null) {
			return true;
		}
		String trimmed = ignoreWhitespace ? line.trim() : line;
		return trimmed.endsWith(suffix);
	}

	/**
	 * Remove the suffix from the end of the record.
	 *
	 * @see SimpleRecordSeparatorPolicy#postProcess(String)
	 */
	@Override
	public @Nullable String postProcess(@Nullable String record) {
		if (record == null) {
			return null;
		}
		return record.substring(0, record.lastIndexOf(suffix));
	}

}
