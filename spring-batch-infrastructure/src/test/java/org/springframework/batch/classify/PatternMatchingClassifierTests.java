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
package org.springframework.batch.classify;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.classify.PatternMatchingClassifier;

/**
 * @author Dave Syer
 * 
 */
public class PatternMatchingClassifierTests {

	private PatternMatchingClassifier<String> classifier = new PatternMatchingClassifier<String>();

	private Map<String, String> map;

	@Before
	public void createMap() {
		map = new HashMap<String, String>();
		map.put("foo", "bar");
		map.put("*", "spam");
	}

	@Test
	public void testSetPatternMap() {
		classifier.setPatternMap(map);
		assertEquals("bar", classifier.classify("foo"));
		assertEquals("spam", classifier.classify("bucket"));
	}

	@Test
	public void testCreateFromMap() {
		classifier = new PatternMatchingClassifier<String>(map);
		assertEquals("bar", classifier.classify("foo"));
		assertEquals("spam", classifier.classify("bucket"));		
	}

}