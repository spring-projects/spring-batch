/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.support.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Intermittent false-negative failures") // #3847
class ConcurrentTransactionAwareProxyTests {

	private static final Log logger = LogFactory.getLog(ConcurrentTransactionAwareProxyTests.class);

	private final PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	int outerMax = 20;

	int innerMax = 30;

	private ExecutorService executor;

	private CompletionService<List<String>> completionService;

	@BeforeEach
	void init() {
		executor = Executors.newFixedThreadPool(outerMax);
		completionService = new ExecutorCompletionService<>(executor);
	}

	@AfterEach
	void close() {
		executor.shutdown();
	}

	@Test
	void testConcurrentTransactionalSet() {
		Set<String> set = TransactionAwareProxyFactory.createTransactionalSet();
		assertThrows(Throwable.class, () -> testSet(set));
	}

	@Test
	void testConcurrentTransactionalAppendOnlySet() throws Exception {
		Set<String> set = TransactionAwareProxyFactory.createAppendOnlyTransactionalSet();
		testSet(set);
	}

	@Test
	void testConcurrentTransactionalAppendOnlyList() throws Exception {
		List<String> list = TransactionAwareProxyFactory.createAppendOnlyTransactionalList();
		testList(list, false);
	}

	@Test
	void testConcurrentTransactionalAppendOnlyMap() throws Exception {
		Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createAppendOnlyTransactionalMap();
		testMap(map);
	}

	@Test
	void testConcurrentTransactionalMap() {
		Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createTransactionalMap();
		assertThrows(ExecutionException.class, () -> testMap(map));
	}

	@Test
	void testTransactionalContains() {
		final Map<Long, Map<String, String>> map = TransactionAwareProxyFactory.createAppendOnlyTransactionalMap();
		boolean result = new TransactionTemplate(transactionManager).execute(status -> map.containsKey(0L));
		assertFalse(result);
	}

	private void testSet(Set<String> set) throws Exception {

		for (int i = 0; i < outerMax; i++) {

			final int count = i;
			completionService.submit(() -> {
				List<String> list = new ArrayList<>();
				for (int i1 = 0; i1 < innerMax; i1++) {
					String value = count + "bar" + i1;
					saveInSetAndAssert(set, value);
					list.add(value);
				}
				return list;
			});

		}

		for (int i = 0; i < outerMax; i++) {
			List<String> result = completionService.take().get();
			assertEquals(innerMax, result.size());
		}

		assertEquals(innerMax * outerMax, set.size());

	}

	private void testList(List<String> list, final boolean mutate) throws Exception {

		for (int i = 0; i < outerMax; i++) {

			completionService.submit(() -> {
				List<String> result = new ArrayList<>();
				for (int i1 = 0; i1 < innerMax; i1++) {
					String value = "bar" + i1;
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
			});

		}

		for (int i = 0; i < outerMax; i++) {
			List<String> result = completionService.take().get();
			assertEquals(innerMax, result.size(), "Wrong number of results in inner task");
		}

		assertEquals(innerMax * outerMax, list.size(), "Wrong number of results in aggregate");

	}

	private void testMap(Map<Long, Map<String, String>> map) throws Exception {

		int numberOfKeys = outerMax;

		for (int i = 0; i < outerMax; i++) {

			for (int j = 0; j < numberOfKeys; j++) {
				final long id = j * 1000L + 123L + i;

				completionService.submit(() -> {
					List<String> list = new ArrayList<>();
					for (int i1 = 0; i1 < innerMax; i1++) {
						String value = "bar" + i1;
						list.add(saveInMapAndAssert(map, id, value).get("foo"));
					}
					return list;
				});
			}

			for (int j = 0; j < numberOfKeys; j++) {
				completionService.take().get();
			}

		}

	}

	private String saveInSetAndAssert(Set<String> set, final String value) {

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			set.add(value);
			return null;
		});

		Assert.state(set.contains(value), "Lost update: value=" + value);

		return value;

	}

	private String saveInListAndAssert(List<String> list, final String value) {

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			list.add(value);
			return null;
		});

		Assert.state(list.contains(value), "Lost update: value=" + value);

		return value;

	}

	private Map<String, String> saveInMapAndAssert(Map<Long, Map<String, String>> map, final Long id,
			final String value) {

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			if (!map.containsKey(id)) {
				map.put(id, new HashMap<>());
			}
			map.get(id).put("foo", value);
			return null;
		});

		Map<String, String> result = map.get(id);
		Assert.state(result != null, "Lost insert: null String at value=" + value);
		String foo = result.get("foo");
		Assert.state(value.equals(foo), "Lost update: wrong value=" + value + " (found " + foo + ") for id=" + id);

		return result;

	}

}
