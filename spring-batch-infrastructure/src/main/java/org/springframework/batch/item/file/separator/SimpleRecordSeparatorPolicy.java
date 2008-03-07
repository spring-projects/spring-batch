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
 * Simplest possible {@link RecordSeparatorPolicy} - treats all lines as record
 * endings.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleRecordSeparatorPolicy implements RecordSeparatorPolicy {

	/**
	 * Always returns true.
	 * 
	 * @see org.springframework.batch.item.file.separator.RecordSeparatorPolicy#isEndOfRecord(java.lang.String)
	 */
	public boolean isEndOfRecord(String line) {
		return true;
	}

	/**
	 * Pass the record through. Do nothing.
	 * @see org.springframework.batch.item.file.separator.RecordSeparatorPolicy#postProcess(java.lang.String)
	 */
	public String postProcess(String record) {
		return record;
	}
	
	/**
	 * Pass the line through.  Do nothing.
	 * @see org.springframework.batch.item.file.separator.RecordSeparatorPolicy#preProcess(java.lang.String)
	 */
	public String preProcess(String line) {
		return line;
	}

}
