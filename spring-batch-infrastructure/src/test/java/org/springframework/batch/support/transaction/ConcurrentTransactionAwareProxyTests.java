/*
 * Copyright 2006-2009 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class ConcurrentTransactionAwareProxyTests {

	private static Log logger = LogFactory.getLog(ConcurrentTransactionAwareProxyTests.class);

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	int outerMax = 20;

	int innerMax = 30;

	private ExecutorService executor;

	private CompletionService<List<String>> completionService;

	@Before
	public void init() {
		executor = Executors.newFixedThreadPool(outerMax);
		completionService = new ExecutorCompletionService<List<String>>(executor);
	}

	@After
	public void close() {
		executor.shutdown();
	}

	@Test(expected = Throwable.class)
	public void testConcurrentTransactionalSet() throws Exception {
		Set<String> set = TransactionAwareProxyFactory.createTransactionalSet();
		testSet(set);
	}

	@Test
	public void testConcurrentTransactionalAppendOnlySet() throws Exception {
		Set<String> set = TransactionAwareProxyFactory.createAppendOnlyTransactionalSet();
		testSet(set);
	}

	@Test
	public void testConcurrentTransactionalAppendOnlyList() throws Exception {
		List<String> list = TransactionAwareProxyFactory.createAppendOnlyTransactionalList();
		testList(list, false);
	}

	@Ignore("This fails too often and is a false negative")
	@Test
	public void testConcurrentTransactionalList() throws Exception {
		List<String> list = TransactionAwareProxyFactory.createTransactionalList();
		try {
			testList(list, true);
			fail("Expected ExecutionException or AssertionError (but don't panic if it didn't happen: it probably just means we got lucky for a change)");
		}
		catch (ExecutionException e) {
			String message = e.getCause().getMessage();
			assertTrue("Wrong message: " + message, message.startsWith("Lost update"));
		}
		catch (AssertionError e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.startsWith("Wrong number of results"));
		}
	}

	@Test
	public void testConcurrentTransactionalAppendOnlyMap() throws Exception {
		Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createAppendOnlyTransactionalMap();
		testMap(map);
	}

	@Test(expected = ExecutionException.class)
	public void testConcurrentTransactionalMap() throws Exception {
		Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createTransactionalMap();
		testMap(map);
	}

	@Test
	public void testTransactionalContains() throws Exception {
		final Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createAppendOnlyTransactionalMap();
		boolean result = (Boolean) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return map.containsKey("foo");
			}
		});
		assertFalse(result);
	}

	private void testSet(final Set<String> set) throws Exception {

		for (int i = 0; i < outerMax; i++) {

			final int count = i;
			completionService.submit(new Callable<List<String>>() {
				public List<String> call() throws Exception {
					List<String> list = new ArrayList<String>();
					for (int i = 0; i < innerMax; i++) {
						String value = count + "bar" + i;
						saveInSetAndAssert(set, value);
						list.add(value);
					}
					return list;
				}
			});

		}

		for (int i = 0; i < outerMax; i++) {
			List<String> result = completionService.take().get();
			assertEquals(innerMax, result.size());
		}

		assertEquals(innerMax * outerMax, set.size());

	}

	private void testList(final List<String> list, final boolean mutate) throws Exception {

		for (int i = 0; i < outerMax; i++) {

			completionService.submit(new Callable<List<String>>() {
				public List<String> call() throws Exception {
					List<String> result = new ArrayList<String>();
					for (int i = 0; i < innerMax; i++) {
						String value = "bar" + i;
						saveInListAndAssert(list, value);
						result.add(value);
						// Need to slow it down to allow threads to interleave
						Thread.sleep(10L);
						if (mutate) {
							list.remove(value);
							list.add(value);
						}
					}
					logger.info("Added: " + innerMax + " values");
					return result;
				}
			});

		}

		for (int i = 0; i < outerMax; i++) {
			List<String> result = completionService.take().get();
			assertEquals("Wrong number of results in inner task", innerMax, result.size());
		}

		assertEquals("Wrong number of results in aggregate", innerMax * outerMax, list.size());

	}

	private void testMap(final Map<Long, Map<String, String>> map) throws Exception {

		int numberOfKeys = outerMax;

		for (int i = 0; i < outerMax; i++) {

			for (int j = 0; j < numberOfKeys; j++) {
				final long id = j * 1000 + 123L + i;

				completionService.submit(new Callable<List<String>>() {
					public List<String> call() throws Exception {
						List<String> list = new ArrayList<String>();
						for (int i = 0; i < innerMax; i++) {
							String value = "bar" + i;
							list.add(saveInMapAndAssert(map, id, value).get("foo"));
						}
						return list;
					}
				});
			}

			for (int j = 0; j < numberOfKeys; j++) {
				completionService.take().get();
			}

		}

	}

	private String saveInSetAndAssert(final Set<String> set, final String value) {

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				set.add(value);
				return null;
			}
		});

		Assert.state(set.contains(value), "Lost update: value=" + value);

		return value;

	}

	private String saveInListAndAssert(final List<String> list, final String value) {

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				list.add(value);
				return null;
			}
		});

		Assert.state(list.contains(value), "Lost update: value=" + value);

		return value;

	}

	private Map<String, String> saveInMapAndAssert(final Map<Long, Map<String, String>> map, final Long id,
			final String value) {

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if (!map.containsKey(id)) {
					map.put(id, new HashMap<String, String>());
				}
				map.get(id).put("foo", value);
				return null;
			}
		});

		Map<String, String> result = map.get(id);
		Assert.state(result != null, "Lost insert: null String at value=" + value);
		String foo = result.get("foo");
		Assert.state(value.equals(foo), "Lost update: wrong value=" + value + " (found " + foo + ") for id=" + id);

		return result;

	}

}
