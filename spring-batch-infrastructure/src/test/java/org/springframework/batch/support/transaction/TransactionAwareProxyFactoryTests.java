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

package org.springframework.batch.support.transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class TransactionAwareProxyFactoryTests extends TestCase {

	public void testCreateList() throws Exception {
		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		list.add("foo");
		assertEquals(1, list.size());
	}

	public void testCreateListWithValues() throws Exception {
		List<String> list = TransactionAwareProxyFactory.createTransactionalList(Collections.singletonList("foo"));
		assertEquals(1, list.size());
	}

	public void testCreateSet() throws Exception {
		Set<String> set = TransactionAwareProxyFactory.createTransactionalSet();
		set.add("foo");
		assertEquals(1, set.size());
	}

	public void testCreateSetWithValues() throws Exception {
		Set<String> list = TransactionAwareProxyFactory.createTransactionalSet(Collections.singleton("foo"));
		assertEquals(1, list.size());
	}

	public void testCreateMap() throws Exception {
		Map<String, String> map = TransactionAwareProxyFactory.createTransactionalMap();
		map.put("foo", "bar");
		assertEquals(1, map.size());
	}

	public void testCreateMapWithValues() throws Exception {
		Map<String, String> map = TransactionAwareProxyFactory.createTransactionalMap(Collections.singletonMap("foo",
				"bar"));
		assertEquals(1, map.size());
	}

}
