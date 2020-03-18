/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.batch.item.file.transform;

import junit.framework.TestCase;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;

public class PassThroughLineAggregatorTests extends TestCase {
	
	private LineAggregator<Object> mapper = new PassThroughLineAggregator<>();

	public void testUnmapItemAsFieldSet() throws Exception {
		Object item = new Object();
		assertEquals(item.toString(), mapper.aggregate(item));
	}

	public void testUnmapItemAsString() throws Exception {
		assertEquals("foo", mapper.aggregate("foo"));
	}

}
