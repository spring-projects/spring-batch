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

package org.springframework.batch.item.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.reader.ListItemReader;

import junit.framework.TestCase;

public class ListItemReaderTests extends TestCase {

	ListItemReader provider = new ListItemReader(Arrays.asList(new String[] { "a", "b", "c" }));

	public void testNext() throws Exception {
		assertEquals("a", provider.read());
		assertEquals("b", provider.read());
		assertEquals("c", provider.read());
		assertEquals(null, provider.read());
	}

	public void testChangeList() throws Exception {
		List list = new ArrayList(Arrays.asList(new String[] { "a", "b", "c" }));
		provider = new ListItemReader(list);
		assertEquals("a", provider.read());
		list.clear();
		assertEquals(0, list.size());
		assertEquals("b", provider.read());
	}
}
