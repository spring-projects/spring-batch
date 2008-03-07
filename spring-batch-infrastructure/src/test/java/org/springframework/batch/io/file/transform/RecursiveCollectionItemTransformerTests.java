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
package org.springframework.batch.io.file.transform;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.item.writer.ItemTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 
 */
public class RecursiveCollectionItemTransformerTests extends TestCase {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private RecursiveCollectionItemTransformer transformer = new RecursiveCollectionItemTransformer();

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#setDelegate(org.springframework.batch.item.writer.ItemTransformer)}.
	 * @throws Exception
	 */
	public void testSetDelegate() throws Exception {
		transformer.setDelegate(new ItemTransformer() {
			public Object transform(Object item) throws Exception {
				return "bar";
			}
		});
		assertEquals("bar", transformer.transform(new Object()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#setDelegate(org.springframework.batch.item.writer.ItemTransformer)}.
	 * @throws Exception
	 */
	public void testSetDelegateAndPassInString() throws Exception {
		transformer.setDelegate(new ItemTransformer() {
			public Object transform(Object item) throws Exception {
				return "bar";
			}
		});
		assertEquals("foo", transformer.transform("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#setDelegate(org.springframework.batch.item.writer.ItemTransformer)}.
	 * @throws Exception
	 */
	public void testSetDelegateReturnsList() throws Exception {
		transformer.setDelegate(new ItemTransformer() {
			public Object transform(Object item) throws Exception {
				return Collections.singletonList("bar");
			}
		});
		// The result of the delegate is a list, which will simply be
		// converted to a string by concatenating with "":
		assertEquals("[bar]", transformer.transform(new Object()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testTransformString() throws Exception {
		assertEquals("foo", transformer.transform("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testTransformArray() throws Exception {
		String result = (String) transformer.transform(StringUtils.commaDelimitedListToStringArray("foo,bar"));
		String[] array = StringUtils.delimitedListToStringArray(result, LINE_SEPARATOR);
		assertEquals("foo", array[0]);
		assertEquals("bar", array[1]);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testTransformList() throws Exception {
		String result = (String) transformer.transform(Arrays.asList(StringUtils.commaDelimitedListToStringArray("foo,bar")));
		String[] array = StringUtils.delimitedListToStringArray(result, LINE_SEPARATOR);
		assertEquals("foo", array[0]);
		assertEquals("bar", array[1]);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.transform.RecursiveCollectionItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testTransformArrayOfArrays() throws Exception {
		String[][] input = new String[][] { StringUtils.commaDelimitedListToStringArray("foo,bar"),
				StringUtils.commaDelimitedListToStringArray("spam,bucket") };
		String result = (String) transformer.transform(input);
		String[] array = StringUtils.delimitedListToStringArray(result, LINE_SEPARATOR);
		assertEquals(4,array.length);
		assertEquals("foo", array[0]);
		assertEquals("spam", array[2]);
	}
}
