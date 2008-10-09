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
package org.springframework.batch.item.file.transform;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 
 */
public class RecursiveCollectionItemTransformerTests extends TestCase {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private RecursiveCollectionLineAggregator<String> aggregator = new RecursiveCollectionLineAggregator<String>();

	public void testSetDelegateAndPassInString() throws Exception {
		aggregator.setDelegate(new LineAggregator<String>() {
			public String aggregate(String item) {
				return "bar";
			}
		});
		assertEquals("bar", aggregator.aggregate(Collections.singleton("foo")));
	}

	public void testTransformList() throws Exception {
		String result = aggregator.aggregate(Arrays.asList(StringUtils.commaDelimitedListToStringArray("foo,bar")));
		String[] array = StringUtils.delimitedListToStringArray(result, LINE_SEPARATOR);
		assertEquals("foo", array[0]);
		assertEquals("bar", array[1]);
	}

}
