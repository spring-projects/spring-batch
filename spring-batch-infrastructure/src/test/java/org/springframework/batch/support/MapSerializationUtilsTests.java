/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class MapSerializationUtilsTests {

	private Map<String, Object> map = new ConcurrentHashMap<String, Object>();

	@Test
	public void testCycle() throws Exception {
		map.put("foo.bar.spam", 123);
		Map<String, Object> result = getCopy(map);
		assertEquals(map, result);
	}

	@Test
	public void testMultipleCycles() throws Exception {
		map.put("foo.bar.spam", 123);
		for (int i = 0; i < 1000; i++) {
			Map<String, Object> result = getCopy(map);
			assertEquals(map, result);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getCopy(Map<String, Object> map) {
		return (Map<String, Object>) SerializationUtils.deserialize(SerializationUtils.serialize(map));
	}

}
