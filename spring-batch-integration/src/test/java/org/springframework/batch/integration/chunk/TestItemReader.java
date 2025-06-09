/*
 * Copyright 2009-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

@Component
public class TestItemReader<T> implements ItemReader<T> {

	private static final Log logger = LogFactory.getLog(TestItemReader.class);

	/**
	 * Counts the number of chunks processed in the handler.
	 */
	public volatile int count = 0;

	/**
	 * Item that causes failure in handler.
	 */
	public final static String FAIL_ON = "bad";

	/**
	 * Item that causes handler to wait to simulate delayed processing.
	 */
	public static final String WAIT_ON = "wait";

	private List<T> items = new ArrayList<>();

	/**
	 * @param items the items to set
	 */
	public void setItems(List<T> items) {
		this.items = items;
	}

	@Override
	public @Nullable T read() throws Exception, UnexpectedInputException, ParseException {

		if (count >= items.size()) {
			return null;
		}

		T item = items.get(count++);

		logger.debug("Reading " + item);

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

		return item;

	}

}
