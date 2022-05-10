/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.sample.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.sample.support.ExceptionThrowingItemReaderProxy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionThrowingItemReaderProxyTests {

	// expected call count before exception is thrown (exception should be thrown in next
	// iteration)
	private static final int ITER_COUNT = 5;

	@AfterEach
	void tearDown() {
		RepeatSynchronizationManager.clear();
	}

	@Test
	void testProcess() throws Exception {

		// create module and set item processor and iteration count
		ExceptionThrowingItemReaderProxy<String> itemReader = new ExceptionThrowingItemReaderProxy<>();
		itemReader.setDelegate(new ListItemReader<>(List.of("a", "b", "c", "d", "e", "f")));
		itemReader.setThrowExceptionOnRecordNumber(ITER_COUNT + 1);

		RepeatSynchronizationManager.register(new RepeatContextSupport(null));

		// call process method multiple times and verify whether exception is thrown when
		// expected
		for (int i = 0; i <= ITER_COUNT; i++) {
			try {
				itemReader.read();
				assertTrue(i < ITER_COUNT);
			}
			catch (UnexpectedJobExecutionException bce) {
				assertEquals(ITER_COUNT, i);
			}
		}

	}

}
