/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.partition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.batch.api.partition.PartitionCollector;

import org.junit.Before;
import org.junit.Test;

public class PartitionCollectorAdapterTests {

	private PartitionCollectorAdapter adapter;

	@Before
	public void setUp() throws Exception {
		adapter = new PartitionCollectorAdapter();
	}

	@Test
	public void testPropertiesSeet() throws Exception {
		try {
			adapter.afterPropertiesSet();
			fail("Did not check for a PartitionCollector instance");
		} catch (IllegalArgumentException iae) {
			assertEquals(iae.getMessage(), "A PartitionCollector instance is required");
		}

		adapter.setPartitionCollector(new PartitionCollector() {

			@Override
			public Serializable collectPartitionData() throws Exception {
				return null;
			}
		});

		try {
			adapter.afterPropertiesSet();
			fail("Did not check for a queue");
		} catch (IllegalArgumentException iae) {
			assertEquals(iae.getMessage(), "A thread safe Queue instance is required");
		}

		adapter.setPartitionQueue(new ConcurrentLinkedQueue<Serializable>());

		adapter.afterPropertiesSet();
	}

	@Test
	public void testAfterChunkSuccessful() throws Exception {
		adapter.setPartitionCollector(new PartitionCollector() {

			private int count = 0;

			@Override
			public Serializable collectPartitionData() throws Exception {
				return String.valueOf(count++);
			}
		});

		Queue<Serializable> dataQueue = new ConcurrentLinkedQueue<Serializable>();
		adapter.setPartitionQueue(dataQueue);
		adapter.afterPropertiesSet();

		adapter.afterChunk(null);
		adapter.afterChunk(null);
		adapter.afterChunk(null);

		assertEquals(3, dataQueue.size());
		assertEquals("0", dataQueue.remove());
		assertEquals("1", dataQueue.remove());
		assertEquals("2", dataQueue.remove());
	}

	@Test(expected=PartitionException.class)
	public void testAfterChunkException() {
		// Throws an NPE due to a null collector
		adapter.afterChunk(null);
	}
}
