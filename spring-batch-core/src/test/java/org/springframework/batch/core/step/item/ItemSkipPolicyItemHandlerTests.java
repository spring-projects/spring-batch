/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class ItemSkipPolicyItemHandlerTests extends TestCase {

	private final SkipWriterStub writer = new SkipWriterStub();

	private ItemSkipPolicyItemHandler<Holder> handler = new ItemSkipPolicyItemHandler<Holder>(new SkipReaderStub(), writer);

	private StepContribution contribution = new StepContribution(new JobExecution(new JobInstance(new Long(11),
			new JobParameters(), "jobName")).createStepExecution(new StepSupport("foo")));

	protected void tearDown() throws Exception {
		// remove the resource if it exists
		handler.mark();
	}

	public void testReadWithNoSkip() throws Exception {
		assertEquals(new Holder("1"), handler.read(contribution));
		try {
			handler.read(contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		assertEquals(0, contribution.getSkipCount());
		assertEquals(new Holder("3"), handler.read(contribution));
	}

	public void testReadWithSkip() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		assertEquals(new Holder("1"), handler.read(contribution));
		assertEquals(new Holder("3"), handler.read(contribution));
		contribution.combineSkipCounts();
		assertEquals(1, contribution.getReadSkipCount());
		assertEquals(new Holder("4"), handler.read(contribution));
	}

	public void testWriteWithNoSkip() throws Exception {
		handler.write(new Holder("3"), contribution);
		try {
			handler.write(new Holder("4"), contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		assertEquals(0, contribution.getSkipCount());
	}

	public void testHandleWithSkip() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.handle(contribution);
		handler.handle(contribution);
		contribution.combineSkipCounts();
		assertEquals(1, contribution.getSkipCount());
		// 2 is skipped so 3 was last one processed and now we are at 4
		try {
			handler.handle(contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		
		assertEquals(3, contribution.getItemCount());
		assertEquals(2, contribution.getSkipCount());
		// No "4" because it was skipped on write
		assertEquals(new Holder("5"), handler.read(contribution));
	}

	@SuppressWarnings("unchecked")
	public void testWriteWithSkipAfterMark() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		try {
			handler.write(new Holder("4"), contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		handler.handle(contribution);
		handler.handle(contribution);
		contribution.combineSkipCounts();
		assertEquals(2, contribution.getSkipCount());
		// 2 is skipped so 3 was last one processed and now we are at 4, which
		// was previously skipped
		handler.handle(contribution);
		assertEquals(null, handler.read(contribution));
		assertEquals(3, contribution.getItemCount());
		assertEquals(2, contribution.getSkipCount());

		assertEquals(1, TransactionSynchronizationManager.getResourceMap().size());
		Set<Object> removed = (Set<Object>) TransactionSynchronizationManager.getResourceMap().values().iterator().next();
		// one skipped item was detected on read
		assertEquals(1, removed.size());
		// mark() should remove the set of removed keys
		handler.mark();
		assertEquals(0, TransactionSynchronizationManager.getResourceMap().size());
	}

	public void testWriteWithSkipAndItemKeyGenerator() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.setItemKeyGenerator(new ItemKeyGenerator() {
			public Object getKey(Object item) {
				return ((Holder) item).value;
			}
		});
		handler.write(new Holder("3"), contribution);
		try {
			handler.write(new Holder("4"), contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		assertEquals(1, contribution.getSkipCount());
		assertEquals(new Holder("1"), handler.read(contribution));
		assertEquals(new Holder("3"), handler.read(contribution));
		contribution.combineSkipCounts();
		assertEquals(2, contribution.getSkipCount());
		// No "4" because it was skipped on write
		assertEquals(new Holder("5"), handler.read(contribution));
	}

	public void testWriteWithSkipWhenMutating() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		writer.mutate = true;
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.handle(contribution);
		handler.handle(contribution);
		contribution.combineSkipCounts();
		assertEquals(1, contribution.getSkipCount());
		// 2 is skipped so 3 was last one processed and now we are at 4
		try {
			handler.handle(contribution);
			fail("Expected SkippableException");
		}
		catch (SkippableException e) {
			// expected
		}
		assertEquals(3, contribution.getItemCount());
		assertEquals(2, contribution.getSkipCount());
		// No "4" because it was skipped on write, even though it is mutating
		// its key
		assertEquals(new Holder("5"), handler.read(contribution));
	}

	public void testWriteWithSkipCapacityBreached() throws Exception {
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.setSkipCacheCapacity(0);
		handler.handle(contribution);
		handler.handle(contribution);
		contribution.combineSkipCounts();
		assertEquals(1, contribution.getSkipCount());
		// 2 is skipped so 3 was last one processed and now we are at 4
		try {
			handler.handle(contribution);
			fail("Expected UnexpectedJobExecutionException");
		}
		catch (UnexpectedJobExecutionException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'capacity': " + message, message.indexOf("capacity") >= 0);
		}
		assertEquals(2, contribution.getSkipCount());
		// No "4" because it was skipped on write, even though it is mutating
		// its key
		assertEquals(new Holder("5"), handler.read(contribution));
	}

	/**
	 * Skippable write exceptions are not re-thrown when included in the
	 * {@link ItemSkipPolicyItemHandler#setDoNotRethrowExceptionClasses(Class[])}
	 */
	public void testWriteWithSkipAndDoNotRethrow() throws Exception {

		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.setDoNotRethrowExceptionClasses(new Class[] { SkippableException.class });

		handler.handle(contribution);
		handler.handle(contribution);
		contribution.combineSkipCounts();
		assertEquals(1, contribution.getSkipCount());

		// skippable exception thrown in writer at this point, but it won't be
		// re-thrown
		handler.handle(contribution);

		assertEquals(3, contribution.getItemCount());
		assertEquals(2, contribution.getSkipCount());
		// No "4" because it was skipped on write, even though it is mutating
		// its key
		assertEquals(new Holder("5"), handler.read(contribution));
	}

	/**
	 * {@link SkipListener#onSkipInWrite(Object, Throwable)} should be called
	 * also if the exception is not re-thrown (does not cause rollback).
	 */
	public void testHandleWithSkipAndListeners() throws Exception {

		class SkipOnWriteListener extends SkipListenerSupport {

			boolean called = false;

			public void onSkipInWrite(Object item, Throwable t) {
				called = true;
			}

		}
		;
		SkipOnWriteListener skipOnWriteListener = new SkipOnWriteListener();
		handler.setSkipListeners(new SkipListener[] { skipOnWriteListener });
		handler.setDoNotRethrowExceptionClasses(new Class[] { SkippableException.class });
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		for (int i = 0; i < 5; i++) {
			handler.handle(contribution);
		}
		assertTrue("onSkipInWrite should be called although the exception is not re-thrown", skipOnWriteListener.called);

	}

	/**
	 * Simple item reader that supports skip functionality.
	 */
	private static class SkipReaderStub implements ItemReader<Holder> {

		final String[] values = { "1", "2", "3", "4", "5" };

		Collection<Object> processed = new ArrayList<Object>();

		int counter = -1;

		int marked = 0;

		public Holder read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
			counter++;
			if (counter == 1) {
				throw new SkippableException("exception in reader");
			}
			if (counter >= values.length) {
				return null;
			}
			Holder item = new Holder(values[counter]);
			processed.add(item);
			return item;
		}

		public void mark() throws MarkFailedException {
			marked = counter;
		}

		public void reset() throws ResetFailedException {
			counter = marked;
		}

	}

	/**
	 * Simple item writer that supports skip functionality.
	 */
	private static class SkipWriterStub implements ItemWriter<Holder> {

		boolean mutate = false;

		List<Object> written = new ArrayList<Object>();

		int flushIndex = -1;

		public void clear() throws ClearFailedException {
			for (int i = flushIndex + 1; i < written.size(); i++) {
				written.remove(i);
			}
		}

		public void flush() throws FlushFailedException {
			flushIndex = written.size() - 1;
		}

		public void write(Holder item) throws Exception {
			String value = item.value;
			written.add(item);
			if (mutate) {
				item.value = "done";
			}
			if (value.equals("4")) {
				throw new SkippableException("exception in writer");
			}
		}

	}

	private static class SkippableException extends Exception {
		public SkippableException(String message) {
			super(message);
		}
	}

	private static class Holder {
		private String value = null;

		public Holder(String value) {
			super();
			this.value = value;
		}

		public boolean equals(Object obj) {
			return obj instanceof Holder && value.equals(((Holder) obj).value);
		}

		public int hashCode() {
			return value.hashCode();
		}

		public String toString() {
			return "[holder:" + value + "]";
		}
	}
}
