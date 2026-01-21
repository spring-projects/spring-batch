/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.core.repository.dao.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MongoSequenceIncrementer}.
 */
public class MongoSequenceIncrementerTests {
    
	@Test
	void testTimeOrdering() throws DataAccessException {
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer();
		List<Long> ids = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			ids.add(incrementer.nextLongValue());
		}

		List<Long> sorted = new ArrayList<>(ids);
		Collections.sort(sorted);
		assertEquals(sorted, ids, "IDs should be in time order");
	}

	@Test
	void testConcurrency() throws InterruptedException {
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer();
		Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
		int threadCount = 10;
		int idsPerThread = 100;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					for (int j = 0; j < idsPerThread; j++) {
						ids.add(incrementer.nextLongValue());
					}
				}
				catch (DataAccessException e) {
					fail("Should not throw DataAccessException: " + e.getMessage());
				}
				finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executor.shutdown();

		assertEquals(threadCount * idsPerThread, ids.size(),
				"All IDs generated from multiple threads should be unique");
	}

    @Test
    void testNodeIdSeparation() throws DataAccessException {
        MongoSequenceIncrementer incrementer1 = new MongoSequenceIncrementer(1);
        MongoSequenceIncrementer incrementer2 = new MongoSequenceIncrementer(2);

        long id1 = incrementer1.nextLongValue();
        long id2 = incrementer2.nextLongValue();

        assertNotEquals(id1, id2, "IDs from different nodes should be different");

        long nodeId1 = (id1 >> 12) & 0x3FF;
        long nodeId2 = (id2 >> 12) & 0x3FF;

        assertEquals(1, nodeId1, "First ID should have node ID 1");
        assertEquals(2, nodeId2, "Second ID should have node ID 2");
    }

}
