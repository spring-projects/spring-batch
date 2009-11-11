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
 * JSON-based record separator. Waits for a valid JSON object before returning a
 * complete line. A valid object has balanced braces ({}), possibly nested, and
 * ends with a closing brace. This separator can be used to split a stream into
 * JSON objects, even if those objects are spread over multiple lines, e.g.
 * 
 * <pre>
 * {"foo": "bar",
 *  "value": { "spam": 2 }}
 *  {"foo": "rab",
 *  "value": { "spam": 3, "foo": "bar" }}
 * </pre>
 * 
 * @author Dave Syer
 * 
 */
public class JsonRecordSeparatorPolicy extends SimpleRecordSeparatorPolicy {

	/**
	 * True if the line can be parsed to a JSON object.
	 * 
	 * @see RecordSeparatorPolicy#isEndOfRecord(String)
	 */
	public boolean isEndOfRecord(String line) {
		return StringUtils.countOccurrencesOf(line, "{") == StringUtils.countOccurrencesOf(line, "}")
				&& line.trim().endsWith("}");
	}

}
