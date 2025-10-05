/*
 * Copyright 2006-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.batch.infrastructure.item.Chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class AlmostStatefulRetryChunkTests {

	private final Log logger = LogFactory.getLog(getClass());

	private int retryAttempts = 0;

	private static final int BACKSTOP_LIMIT = 1000;

	private int count = 0;

	@MethodSource
	@ParameterizedTest
	void testRetry(List<String> args, int limit) {
		Chunk<String> chunk = new Chunk<>();
		for (String string : args) {
			chunk.add(string);
		}
		logger.debug("Starting simple scenario");
		List<String> items = new ArrayList<>(chunk.getItems());
		int before = items.size();
		items.removeAll(Collections.singleton("fail"));
		boolean error = true;
		while (error && count++ < BACKSTOP_LIMIT) {
			try {
				statefulRetry(chunk, limit);
				error = false;
			}
			catch (Exception e) {
				error = true;
			}
		}
		logger.debug("Chunk: " + chunk);
		assertTrue(count < BACKSTOP_LIMIT, "Backstop reached.  Probably an infinite loop...");
		assertFalse(chunk.getItems().contains("fail"));
		assertEquals(items, chunk.getItems());
		assertEquals(before - chunk.getItems().size(), chunk.getSkips().size());
	}

	/**
	 * @param chunk Chunk to retry
	 */
	private void statefulRetry(Chunk<String> chunk, int retryLimit) throws Exception {
		if (retryAttempts <= retryLimit) {
			try {
				// N.B. a classic stateful retry goes straight to recovery here
				logger.debug(String.format("Retry (attempts=%d) chunk: %s", retryAttempts, chunk));
				doWrite(chunk.getItems());
				retryAttempts = 0;
			}
			catch (Exception e) {
				retryAttempts++;
				// stateful retry always rethrow
				throw e;
			}
		}
		else {
			try {
				logger.debug(String.format("Recover (attempts=%d) chunk: %s", retryAttempts, chunk));
				recover(chunk);
			}
			finally {
				retryAttempts = 0;
			}
		}
		// recovery
	}

	/**
	 * @param chunk Chunk to recover
	 */
	private void recover(Chunk<String> chunk) throws Exception {
		for (Chunk<String>.ChunkIterator iterator = chunk.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			try {
				doWrite(Collections.singletonList(string));
			}
			catch (Exception e) {
				iterator.remove(e);
				throw e;
			}
		}
	}

	/**
	 * @param items items to write
	 */
	private void doWrite(List<String> items) throws Exception {
		if (items.contains("fail")) {
			throw new Exception();
		}
	}

	static Stream<Arguments> testRetry() {
		return Stream.of(Arguments.of(List.of("foo"), 0), Arguments.of(List.of("foo", "bar"), 0),
				Arguments.of(List.of("foo", "bar", "spam"), 0),
				Arguments.of(List.of("foo", "bar", "spam", "maps", "rab", "oof"), 0), Arguments.of(List.of("fail"), 0),
				Arguments.of(List.of("foo", "fail"), 0), Arguments.of(List.of("fail", "bar"), 0),
				Arguments.of(List.of("foo", "fail", "spam"), 0), Arguments.of(List.of("fail", "bar", "spam"), 0),
				Arguments.of(List.of("foo", "fail", "spam", "maps", "rab", "oof"), 0),
				Arguments.of(List.of("foo", "fail", "spam", "fail", "rab", "oof"), 0),
				Arguments.of(List.of("fail", "bar", "spam", "fail", "rab", "oof"), 0),
				Arguments.of(List.of("foo", "fail", "fail", "fail", "rab", "oof"), 0), Arguments.of(List.of("fail"), 1),
				Arguments.of(List.of("foo", "fail", "fail", "fail", "rab", "oof"), 1),
				Arguments.of(List.of("foo", "fail", "fail", "fail", "rab", "oof"), 4));
	}

}
