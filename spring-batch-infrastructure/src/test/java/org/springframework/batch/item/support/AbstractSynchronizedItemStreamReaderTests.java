/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.batch.item.support;

import org.junit.Before;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Common parent class for {@link SynchronizedItemStreamReaderTests} and
 * {@link org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilderTests}
 *
 * @author Dimitrios Liapis
 *
 */
public class AbstractSynchronizedItemStreamReaderTests {


	private static final int THREAD_SIZE = 20;

	private AtomicInteger numberOfThreadsInsideReadMethod = new AtomicInteger(0);
	private CountDownLatch threadsStartingLine = new CountDownLatch(1);
	private List<String> errors = Collections.synchronizedList(new ArrayList<>());
	private boolean isClosed = false;

	@Before
	public void init() {
		numberOfThreadsInsideReadMethod = new AtomicInteger(0);
		threadsStartingLine = new CountDownLatch(1);
		errors = Collections.synchronizedList(new ArrayList<>());
		isClosed = false;
	}

	protected void multiThreadedInvocation(ItemStreamReader itemStreamReader) throws Exception {

		ExecutionContext executionContext = new ExecutionContext();

		itemStreamReader.open(executionContext);
		assertEquals(true, executionContext.get(TestItemReader.HAS_BEEN_OPENED));
		assertThat(isClosed, is(false));

		IntStream.rangeClosed(1, THREAD_SIZE)
				.mapToObj(i -> new ItemStreamReaderTestThread(itemStreamReader, executionContext))
				.forEach(Thread::start);

		//release all the threads
		threadsStartingLine.countDown();

		Thread.sleep(5000);
		itemStreamReader.close();

		assertThat(errors,is(empty()));
		assertThat(isClosed, is(true));
		assertEquals(THREAD_SIZE, executionContext.getInt(TestItemReader.UPDATE_COUNT_KEY));

	}

	/**
	 * A simple class used to test the SynchronizedItemStreamReader.
	 * It manages some state variables and updates an ExecutionContext.
	 *
	 * @author Matthew Ouyang
	 * @author Dimitrios Liapis
	 *
	 */
	public class TestItemReader extends AbstractItemStreamItemReader<Integer> implements ItemStreamReader<Integer> {

		static final String HAS_BEEN_OPENED = "hasBeenOpened";
		static final String UPDATE_COUNT_KEY = "updateCount";

		@Override
		public Integer read() throws Exception, ParseException, NonTransientResourceException {
			//If synchronized there can only be one thread at a time
			//therefore the atomic integer can never grow above one
			assertThat(numberOfThreadsInsideReadMethod.incrementAndGet(), is(not(greaterThan(1))));
			return numberOfThreadsInsideReadMethod.decrementAndGet();
		}

		@Override
		public void close() {
			isClosed = true;
		}

		@Override
		public void open(ExecutionContext executionContext) {
			isClosed = false;
			executionContext.put(HAS_BEEN_OPENED, true);
			executionContext.remove(UPDATE_COUNT_KEY);
		}

		@Override
		public void update(ExecutionContext executionContext) {

			if (!executionContext.containsKey(UPDATE_COUNT_KEY)) {
				executionContext.putInt(UPDATE_COUNT_KEY, 0);
			}

			executionContext.putInt(UPDATE_COUNT_KEY
					, executionContext.getInt(UPDATE_COUNT_KEY) + 1
			);
		}
	}

	private class ItemStreamReaderTestThread extends Thread {

		private ItemStreamReader itemStreamReader;
		private ExecutionContext executionContext;

		ItemStreamReaderTestThread(ItemStreamReader itemStreamReader, ExecutionContext executionContext) {
			this.itemStreamReader = itemStreamReader;
			this.executionContext = executionContext;
		}

		public void run() {

			try {
				//ensure all threads await invocation of the read() method
				threadsStartingLine.await();
				itemStreamReader.read();
				itemStreamReader.update(executionContext);

			} catch (AssertionError assertionError) {
				//should be the case on non-thread safe invocations
				errors.add(assertionError.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
