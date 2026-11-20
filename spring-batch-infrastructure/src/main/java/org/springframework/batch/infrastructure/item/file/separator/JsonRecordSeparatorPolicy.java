/*
 * Copyright 2006-2026 the original author or authors.
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

package org.springframework.batch.infrastructure.item.file.separator;

import org.jspecify.annotations.Nullable;

/**
 * JSON-based record separator. Waits for a valid JSON object before returning a complete
 * line. A valid object has balanced braces ({}), possibly nested, and ends with a closing
 * brace. Braces within quoted strings are ignored. This separator can be used to split a
 * stream into JSON objects, even if those objects are spread over multiple lines, e.g.
 *
 * <pre>
 * {"foo": "bar",
 *  "value": { "spam": 2 }}
 *  {"foo": "rab",
 *  "value": { "spam": 3, "foo": "bar" }}
 * </pre>
 *
 * @author Dave Syer
 * @author Lee Sang Won
 *
 */
public class JsonRecordSeparatorPolicy extends SimpleRecordSeparatorPolicy {

	/**
	 * True if the line can be parsed to a JSON object.
	 *
	 * @see RecordSeparatorPolicy#isEndOfRecord(String)
	 */
	@Override
	public boolean isEndOfRecord(@Nullable String line) {
		if (line == null) {
			return false;
		}
		boolean inString = false;
		boolean escaped = false;
		int braceCount = 0;
		for (int i = 0; i < line.length(); i++) {
			char character = line.charAt(i);
			if (inString) {
				if (escaped) {
					escaped = false;
				}
				else if (character == '\\') {
					escaped = true;
				}
				else if (character == '"') {
					inString = false;
				}
			}
			else if (character == '"') {
				inString = true;
			}
			else if (character == '{') {
				braceCount++;
			}
			else if (character == '}') {
				braceCount--;
			}
		}
		return !inString && braceCount == 0 && line.trim().endsWith("}");
	}

}
