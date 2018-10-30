package org.springframework.batch.item.support;

import org.junit.Before;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;

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
 * Common parent class for {@link SynchronizedItemStreamWriterTests} and
 * {@link org.springframework.batch.item.support.builder.SynchronizedItemStreamWriterBuilderTests}
 *
 * @author Dimitrios Liapis
 *
 */
public abstract class AbstractSynchronizedItemStreamWriterTests {

	private static final int THREAD_SIZE = 20;

	private AtomicInteger numberOfThreadsInsideWriteMethod = new AtomicInteger(0);
	private CountDownLatch threadsStartingLine = new CountDownLatch(1);
	private List<String> errors = Collections.synchronizedList(new ArrayList<>());
	private boolean isClosed = false;


	@Before
	public void init() {
		numberOfThreadsInsideWriteMethod = new AtomicInteger(0);
		threadsStartingLine = new CountDownLatch(1);
		errors = Collections.synchronizedList(new ArrayList<>());
		isClosed = false;
	}

	protected void multiThreadedInvocation(ItemStreamWriter itemStreamWriter) throws Exception {

		ExecutionContext executionContext = new ExecutionContext();

		itemStreamWriter.open(executionContext);
		assertEquals(true, executionContext.get(TestItemWriter.HAS_BEEN_OPENED));
		assertThat(isClosed, is(false));

		IntStream.rangeClosed(1, THREAD_SIZE)
				.mapToObj(i -> new ItemStreamWriterTestThread(itemStreamWriter, executionContext))
				.forEach(Thread::start);

		//release all the threads
		threadsStartingLine.countDown();

		Thread.sleep(5000);
		itemStreamWriter.close();

		assertThat(errors,is(empty()));
		assertThat(isClosed, is(true));
		assertEquals(THREAD_SIZE, executionContext.getInt(TestItemWriter.UPDATE_COUNT_KEY));

	}

	/**
	 * Inspired by the SynchronizedItemStreamReaderTests#TestItemReader
	 */
	public class TestItemWriter extends AbstractItemStreamItemWriter<Integer> implements ItemStreamWriter<Integer> {

		static final String HAS_BEEN_OPENED = "hasBeenOpened";
		static final String UPDATE_COUNT_KEY = "updateCount";

		@Override
		public void write(List<? extends Integer> items) {
			//If synchronized there can only be one thread at a time
			//therefore the atomic integer can never grow above one
			assertThat(numberOfThreadsInsideWriteMethod.incrementAndGet(), is(not(greaterThan(1))));
			numberOfThreadsInsideWriteMethod.decrementAndGet();
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

	private class ItemStreamWriterTestThread extends Thread {

		private ItemStreamWriter itemStreamWriter;
		private ExecutionContext executionContext;

		ItemStreamWriterTestThread(ItemStreamWriter itemStreamWriter, ExecutionContext executionContext) {
			this.itemStreamWriter = itemStreamWriter;
			this.executionContext = executionContext;
		}

		public void run() {

			try {
				//ensure all threads await invocation of the write() method
				threadsStartingLine.await();
				itemStreamWriter.write(new ArrayList());
				itemStreamWriter.update(executionContext);

			} catch (AssertionError assertionError) {
				//should be the case on non-thread safe invocations
				errors.add(assertionError.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
