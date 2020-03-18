/*
 * Copyright 2009-2010 the original author or authors.
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

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonLineMapperTests {
	
	private JsonLineMapper mapper = new JsonLineMapper();

	@Test
	public void testMapLine() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMapNested() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1, \"bar\" : {\"foo\": 2}}", 1);
		assertEquals(1, map.get("foo"));
		assertEquals(2, ((Map<String, Object>) map.get("bar")).get("foo"));
	}

	@Test(expected=JsonParseException.class)
	public void testMappingError() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1", 1);
		assertEquals(1, map.get("foo"));
	}

}
