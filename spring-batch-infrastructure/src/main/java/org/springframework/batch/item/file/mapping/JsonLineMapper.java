/*
 * Copyright 2009-2014 the original author or authors.
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
package org.springframework.batch.item.file.mapping;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import org.springframework.batch.item.file.LineMapper;

/**
 * Interpret a line as a JSON object and parse it up to a Map. The line should be a standard JSON object, starting with
 * "{" and ending with "}" and composed of <code>name:value</code> pairs separated by commas. Whitespace is ignored,
 * e.g.
 * 
 * <pre>
 * { "foo" : "bar", "value" : 123 }
 * </pre>
 * 
 * The values can also be JSON objects (which are converted to maps):
 * 
 * <pre>
 * { "foo": "bar", "map": { "one": 1, "two": 2}}
 * </pre>
 * 
 * @author Dave Syer
 * 
 */
public class JsonLineMapper implements LineMapper<Map<String, Object>> {

	private MappingJsonFactory factory = new MappingJsonFactory();

	/**
	 * Interpret the line as a Json object and create a Map from it.
	 * 
	 * @see LineMapper#mapLine(String, int)
	 */
    @Override
	public Map<String, Object> mapLine(String line, int lineNumber) throws Exception {
		Map<String, Object> result;
		JsonParser parser = factory.createParser(line);
		@SuppressWarnings("unchecked")
		Map<String, Object> token = parser.readValueAs(Map.class);
		result = token;
		return result;
	}

}
