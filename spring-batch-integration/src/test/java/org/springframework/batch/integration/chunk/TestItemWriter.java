/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class TestItemWriter<T> implements ItemWriter<T> {

	private static final Log logger = LogFactory.getLog(TestItemWriter.class);

	/**
	 * Counts the number of chunks processed in the handler.
	 */
	public volatile static int count = 0;

	/**
	 * Item that causes failure in handler.
	 */
	public final static String FAIL_ON = "fail";

	/**
	 * Item that causes error in handler.
	 */
	public final static String UNSUPPORTED_ON = "unsupported";

	/**
	 * Item that causes error in handler.
	 */
	public final static String ERROR_ON = "error";

	/**
	 * Item that causes handler to wait to simulate delayed processing.
	 */
	public static final String WAIT_ON = "wait";

	@Override
	public void write(Chunk<? extends T> items) throws Exception {

		for (T item : items) {

			count++;

			logger.debug("Writing: " + item);

			if (item.equals(WAIT_ON)) {
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Unexpected interruption.", e);
				}
			}

			if (item.equals(FAIL_ON)) {
				throw new IllegalStateException("Planned failure on: " + FAIL_ON);
			}

			if (item.equals(UNSUPPORTED_ON)) {
				throw new UnsupportedOperationException("Planned failure on: " + UNSUPPORTED_ON);
			}

			if (item.equals(ERROR_ON)) {
				throw new Error("Planned failure on: " + ERROR_ON);
			}

		}

	}

}
