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

import java.io.BufferedReader;

/**
 * Policy for text file-based input sources to determine the end of a record,
 * e.g. a record might be a single line, or it might be multiple lines
 * terminated by a semicolon.
 * 
 * @author Dave Syer
 * 
 */
public interface RecordSeparatorPolicy {

	/**
	 * Signal the end of a record based on the content of a line, being the
	 * latest line read from an input source. The input is what you would expect
	 * from {@link BufferedReader#readLine()} - i.e. no line separator character
	 * at the end. But it might have line separators embedded in it.
	 * 
	 * @param line a String without a newline character at the end.
	 * @return true if this line is the end of a record.
	 */
	boolean isEndOfRecord(String line);

	/**
	 * Give the policy a chance to post-process a complete record, e.g. remove a
	 * suffix.
	 * 
	 * @param record the complete record.
	 * @return a modified version of the record if desired.
	 */
	String postProcess(String record);

	/**
	 * Pre-process a record before another line is appended, in the case of a
	 * multi-line record. Can be used to remove a prefix or line-continuation
	 * marker. If a record is a single line this callback is not used (but
	 * {@link #postProcess(String)} will be).
	 * 
	 * @param record the current record.
	 * @return the line as it should be appended to a record.
	 */
	String preProcess(String record);

}
