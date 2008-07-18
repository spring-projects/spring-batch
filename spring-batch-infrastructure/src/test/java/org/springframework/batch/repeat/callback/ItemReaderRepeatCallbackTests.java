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

package org.springframework.batch.repeat.callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.item.support.ListItemReader;

public class ItemReaderRepeatCallbackTests extends TestCase {

	ItemReaderRepeatCallback<String> callback;

	List<Object> list = new ArrayList<Object>();

	public void testDoWithRepeat() throws Exception {
		callback = new ItemReaderRepeatCallback<String>(new ListItemReader<String>(Arrays.asList(new String[] { "foo", "bar" })),
				new AbstractItemWriter() {
					public void write(Object data) {
						list.add(data);
					}
				});
		callback.doInIteration(null);
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));
	}

	public void testDoWithRepeatNullProcessor() throws Exception {
		ListItemReader<String> provider = new ListItemReader<String>(Arrays.asList(new String[] { "foo", "bar" }));
		callback = new ItemReaderRepeatCallback<String>(provider);
		callback.doInIteration(null);
		assertEquals(0, list.size());
		assertEquals("bar", provider.read());
	}

}
