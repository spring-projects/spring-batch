/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.step.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.batch.infrastructure.item.UnexpectedInputException;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Jimmy Praet
 */
class ChunkMonitorTests {

	private static final int CHUNK_SIZE = 5;

	private ChunkMonitor monitor = new ChunkMonitor();

	private int count = 0;

	private boolean closed = false;

	@BeforeEach
	void setUp() {
		monitor.setItemReader(new ItemReader<String>() {

			@Override
			public @Nullable String read() throws Exception, UnexpectedInputException, ParseException {
				return String.valueOf(count++);
			}
		});
		monitor.registerItemStream(new ItemStreamSupport() {
			@Override
			public void close() {
				super.close();
				closed = true;
			}
		});
		monitor.setChunkSize(CHUNK_SIZE);
	}

	@Test
	void testIncrementOffset() {
		assertEquals(0, monitor.getOffset());
		monitor.incrementOffset();
		assertEquals(1, monitor.getOffset());
	}

	@Test
	void testResetOffsetManually() {
		monitor.incrementOffset();
		monitor.resetOffset();
		assertEquals(0, monitor.getOffset());
	}

	@Test
	void testResetOffsetAutomatically() {
		for (int i = 0; i < CHUNK_SIZE; i++) {
			monitor.incrementOffset();
		}
		assertEquals(0, monitor.getOffset());
	}

	@Test
	void testClose() {
		monitor.incrementOffset();
		monitor.close();
		assertTrue(closed);
		assertEquals(0, monitor.getOffset());
	}

	@Test
	void testOpen() {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(monitor.getExecutionContextKey("OFFSET"), 2);
		monitor.open(executionContext);
		assertEquals(2, count);
		assertEquals(0, monitor.getOffset());
	}

	@Test
	void testOpenWithNullReader() {
		monitor.setItemReader(null);
		ExecutionContext executionContext = new ExecutionContext();
		monitor.open(executionContext);
		assertEquals(0, monitor.getOffset());
	}

	@Test
	void testOpenWithErrorInReader() {
		monitor.setItemReader(new ItemReader<String>() {

			@Override
			public @Nullable String read() throws Exception, UnexpectedInputException, ParseException {
				throw new IllegalStateException("Expected");
			}
		});
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(monitor.getExecutionContextKey("OFFSET"), 2);
		assertThrows(ItemStreamException.class, () -> monitor.open(executionContext));
	}

	@Test
	void testUpdateOnBoundary() {
		monitor.resetOffset();
		ExecutionContext executionContext = new ExecutionContext();
		monitor.update(executionContext);
		assertEquals(0, executionContext.size());

		executionContext.put(monitor.getExecutionContextKey("OFFSET"), 3);
		monitor.update(executionContext);
		assertEquals(0, executionContext.size());
	}

	@Test
	void testUpdateVanilla() {
		monitor.incrementOffset();
		ExecutionContext executionContext = new ExecutionContext();
		monitor.update(executionContext);
		assertEquals(1, executionContext.size());
	}

	@Test
	void testUpdateWithNoStream() {
		monitor = new ChunkMonitor();
		monitor.setItemReader(new ItemReader<String>() {

			@Override
			public @Nullable String read() throws Exception, UnexpectedInputException, ParseException {
				return String.valueOf(count++);
			}
		});
		monitor.setChunkSize(CHUNK_SIZE);
		monitor.incrementOffset();
		ExecutionContext executionContext = new ExecutionContext();
		monitor.update(executionContext);
		assertEquals(0, executionContext.size());
	}

}
