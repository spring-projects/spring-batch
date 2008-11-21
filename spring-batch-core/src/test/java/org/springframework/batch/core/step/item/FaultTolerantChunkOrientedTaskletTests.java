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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.ChunkContext;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.batch.support.Classifier;

/**
 * @author Dave Syer
 * 
 */
public class FaultTolerantChunkOrientedTaskletTests {

	private Log logger = LogFactory.getLog(getClass());

	private int count = 0;

	private int limit = 3;

	private int skipLimit = 2;

	private List<String> written = new ArrayList<String>();

	private List<Integer> processed = new ArrayList<Integer>();

	private FaultTolerantChunkOrientedTasklet<Integer, String> tasklet;

	private RepeatTemplate chunkOperations = new RepeatTemplate();

	private ItemReader<Integer> itemReader = new ItemReader<Integer>() {
		public Integer read() {
			return count++ >= limit ? null : count;
		};
	};

	private ItemWriter<String> itemWriter = new ItemWriter<String>() {
		public void write(List<? extends String> items) throws Exception {
			written.addAll(items);
		}
	};

	private ItemProcessor<Integer, String> itemProcessor = new ItemProcessor<Integer, String>() {
		public String process(Integer item) throws Exception {
			return "" + item;
		}
	};

	private RetryTemplate retryTemplate = new RetryTemplate();

	private Classifier<Throwable, Boolean> rollbackClassifier = new Classifier<Throwable, Boolean>() {
		public Boolean classify(Throwable classifiable) {
			return true;
		}
	};

	private SkipPolicy readSkipPolicy = new SkipPolicy() {
		public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
			if (skipCount < skipLimit) {
				return true;
			}
			throw new SkipLimitExceededException(skipLimit, t);
		}
	};

	private SkipPolicy writeSkipPolicy = readSkipPolicy;

	@Before
	public void setUp() {
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
	}

	@Test
	public void testBasicHandle() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor, itemWriter,
				chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		tasklet.execute(contribution, new ChunkContext());
		assertEquals(limit, contribution.getReadCount());
	}

	@Test
	public void testSkipOnRead() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(new ItemReader<Integer>() {
			public Integer read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
				throw new RuntimeException("Barf!");
			}
		}, itemProcessor, itemWriter, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy,
				writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}
		assertEquals(0, contribution.getReadCount());
		assertEquals(2, contribution.getReadSkipCount());
	}

	@Test
	public void testSkipSingleItemOnWrite() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor,
				new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						written.addAll(items);
						throw new RuntimeException("Barf!");
					}
				}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		assertTrue(attributes.hasAttribute("SKIPPED_OUTPUTS_KEY"));
		tasklet.execute(contribution, attributes);
		assertEquals(1, contribution.getReadCount());
		assertEquals(1, contribution.getWriteSkipCount());
		assertEquals(1, written.size());
	}

	@Test
	public void testSkipMultipleItemsOnWrite() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor,
				new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						logger.debug("Writing items: " + items);
						written.addAll(items);
						throw new RuntimeException("Barf!");
					}
				}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(2));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();

		// Count to 3: (try + skip + skip)
		for (int i = 0; i < 3; i++) {
			try {
				tasklet.execute(contribution, attributes);
				fail("Expected RuntimeException on i=" + i);
			}
			catch (Exception e) {
				assertEquals("Barf!", e.getMessage());
			}
			assertTrue(attributes.hasAttribute("SKIPPED_OUTPUTS_KEY"));
		}
		@SuppressWarnings("unchecked")
		Map<String, Exception> skips = (Map<String, Exception>) attributes.getAttribute("SKIPPED_OUTPUTS_KEY");
		assertEquals(1, skips.size());
		// The last recovery for this chunk...
		tasklet.execute(contribution, attributes);

		attributes = new ChunkContext();
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}
		assertTrue(attributes.hasAttribute("SKIPPED_OUTPUTS_KEY"));
		assertEquals(3, contribution.getReadCount());
		assertEquals(0, contribution.getFilterCount());
		assertEquals(2, contribution.getWriteSkipCount());
		assertEquals(5, written.size());
	}

	@Test
	public void testSkipSingleItemOnProcess() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader,
				new ItemProcessor<Integer, String>() {
					public String process(Integer item) throws Exception {
						logger.debug("Processing item: " + item);
						processed.add(item);
						if (item == 3) {
							throw new RuntimeException("Barf!");
						}
						return "p" + item;
					}
				}, itemWriter, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy,
				writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(3));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();

		// try
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		assertTrue(attributes.hasAttribute("INPUT_BUFFER_KEY"));

		// skip...
		tasklet.execute(contribution, attributes);

		assertEquals(3, contribution.getReadCount());
		assertEquals(1, contribution.getProcessSkipCount());
		assertEquals(5, processed.size());
		assertEquals("[p1, p2]", written.toString());
	}

	@Test
	public void testSkipOverLimitOnProcess() throws Exception {
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader,
				new ItemProcessor<Integer, String>() {
					public String process(Integer item) throws Exception {
						logger.debug("Processing item: " + item);
						processed.add(item);
						throw new RuntimeException("Barf!");
					}
				}, itemWriter, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy,
				writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(2));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();

		// Count to 2: (try first + fail) + (skip first + try second + fail)
		for (int i = 0; i < 2; i++) {
			try {
				tasklet.execute(contribution, attributes);
				fail("Expected RuntimeException on i=" + i);
			}
			catch (Exception e) {
				assertEquals("Barf!", e.getMessage());
			}
			assertTrue(attributes.hasAttribute("INPUT_BUFFER_KEY"));
		}
		@SuppressWarnings("unchecked")
		Map<Integer, Exception> skips = (Map<Integer, Exception>) attributes.getAttribute("SKIPPED_INPUTS_KEY");
		assertEquals(1, skips.size());

		// The last recovery for this chunk...
		tasklet.execute(contribution, attributes);

		attributes = new ChunkContext();
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		try {
			tasklet.execute(contribution, attributes);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}
		assertTrue(attributes.hasAttribute("INPUT_BUFFER_KEY"));
		assertEquals(3, contribution.getReadCount());
		assertEquals(2, contribution.getProcessSkipCount());
		// Just before the skip at the end we process once more
		assertEquals(3, processed.size());
	}

	/**
	 * When writer throws an exception that causes rollback, items are
	 * re-processed in next iteration.
	 */
	@Test
	public void testReprocessAfterWriterRollback() {
		final String WRITER_FAILED_MESSAGE = "writer failed";
		final int CHUNK_SIZE = 2;
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader,
				new ItemProcessor<Integer, String>() {
					public String process(Integer item) throws Exception {
						processed.add(item);
						return String.valueOf(item);
					}
				}, new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						throw new RuntimeException(WRITER_FAILED_MESSAGE);
					}

				}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(CHUNK_SIZE));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();

		for (int i = 1; i <= 2; i++) {
			try {
				tasklet.execute(contribution, attributes);
				fail();
			}
			catch (Exception e) {
				assertEquals(WRITER_FAILED_MESSAGE, e.getMessage());
				assertEquals(i * CHUNK_SIZE, processed.size());
			}
		}

	}

	/**
	 * Make sure skip counts are correct when items are skipped on both process
	 * and write in the same chunk.
	 */
	@Test
	public void testSkipItemOnProcessAndWrite() throws Exception {
		final String WRITER_FAILED_MESSAGE = "writer failed";
		final String PROCESSOR_FAILED_MESSAGE = "processor failed";
		final RuntimeException writerException = new RuntimeException(WRITER_FAILED_MESSAGE);
		final RuntimeException processorException = new RuntimeException(PROCESSOR_FAILED_MESSAGE);
		final int CHUNK_SIZE = 2;
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader,
				new ItemProcessor<Integer, String>() {
					public String process(Integer item) throws Exception {
						if (item == 1) {
							throw processorException;
						}
						processed.add(item);
						return String.valueOf(item);
					}
				}, new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						throw writerException;
					}

				}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(CHUNK_SIZE));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		ChunkContext attributes = new ChunkContext();

		// mock checks skip listener is called as expected
		@SuppressWarnings("unchecked")
		SkipListener<Integer, String> skipListener = createStrictMock(SkipListener.class);
		tasklet.registerListener(skipListener);
		skipListener.onSkipInProcess(1, processorException);
		expectLastCall().once();
		skipListener.onSkipInWrite("2", writerException);
		expectLastCall().once();
		replay(skipListener);

		// processor fails first
		try {
			tasklet.execute(contribution, attributes);
			fail();
		}
		catch (Exception e) {
			assertEquals(PROCESSOR_FAILED_MESSAGE, e.getMessage());
		}

		// we've only rolled back, nothing has been skipped yet
		assertEquals(0, contribution.getProcessSkipCount());
		assertEquals(0, contribution.getWriteSkipCount());

		try {
			tasklet.execute(contribution, attributes);
			fail();
		}
		catch (Exception e) {
			assertEquals(WRITER_FAILED_MESSAGE, e.getMessage());
		}

		// processor skipped failed item, writer fails and causes rollback
		assertEquals(1, contribution.getProcessSkipCount());
		assertEquals(0, contribution.getWriteSkipCount());

		tasklet.execute(contribution, attributes);

		// both processor and writer skipped
		assertEquals(1, contribution.getProcessSkipCount());
		assertEquals(1, contribution.getWriteSkipCount());

		verify(skipListener);
	}

	@Test
	public void testRethrowNonSkippableExceptionOnWriteAsap() throws Exception {
		final List<String> chunk = Arrays.asList(new String[] { "1", "2" });
		final Exception ex = new RuntimeException();
		final StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		final Map<String, Exception> skipped = new HashMap<String, Exception>();
		writeSkipPolicy = new NeverSkipItemSkipPolicy();

		@SuppressWarnings("unchecked")
		ItemWriter<String> itemWriter = createMock(ItemWriter.class);
		itemWriter.write(chunk);
		expectLastCall().andThrow(ex);
		replay(itemWriter);
		tasklet = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor, itemWriter,
				chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);

		try {
			tasklet.write(chunk, contribution, skipped);
			fail();
		}
		catch (Exception e) {
			assertSame(ex, e);
		}

		try {
			tasklet.write(chunk, contribution, skipped);
			fail();
		}
		catch (Exception e) {
			assertTrue(e instanceof RetryException);
			assertSame(ex, e.getCause());
		}

		/*
		 * writer was called only on first failed attempt, exception is rethrown
		 * immediately when chunk is reprocessed because it is not skippable
		 */
		verify(itemWriter);
	}

}
