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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.tasklet.BasicAttributeAccessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
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

	private FaultTolerantChunkOrientedTasklet<Integer, String> handler;

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

	private ItemSkipPolicy readSkipPolicy = new ItemSkipPolicy() {
		public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
			if (skipCount < skipLimit) {
				return true;
			}
			throw new SkipLimitExceededException(skipLimit, t);
		}
	};

	private ItemSkipPolicy writeSkipPolicy = readSkipPolicy;

	@Before
	public void setUp() {
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
	}

	@Test
	public void testBasicHandle() throws Exception {
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor, itemWriter, chunkOperations,
				retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		handler.execute(contribution, new BasicAttributeAccessor());
		assertEquals(limit, contribution.getReadCount());
	}

	@Test
	public void testSkipOnRead() throws Exception {
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(new ItemReader<Integer>() {
			public Integer read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
				throw new RuntimeException("Barf!");
			}
		}, itemProcessor, itemWriter, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy,
				writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();
		try {
			handler.execute(contribution, attributes);
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
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor, new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				written.addAll(items);
				throw new RuntimeException("Barf!");
			}
		}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();
		try {
			handler.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		assertTrue(attributes.hasAttribute("OUTPUT_BUFFER_KEY"));
		handler.execute(contribution, attributes);
		assertEquals(1, contribution.getReadCount());
		assertEquals(1, contribution.getWriteSkipCount());
		assertEquals(1, written.size());
	}

	@Test
	public void testSkipMultipleItemsOnWrite() throws Exception {
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, itemProcessor, new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				logger.debug("Writing items: " + items);
				written.addAll(items);
				throw new RuntimeException("Barf!");
			}
		}, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(2));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();

		// Count to 3: (try + skip + skip)
		for (int i = 0; i < 3; i++) {
			try {
				handler.execute(contribution, attributes);
				fail("Expected RuntimeException on i=" + i);
			}
			catch (Exception e) {
				assertEquals("Barf!", e.getMessage());
			}
			assertTrue(attributes.hasAttribute("OUTPUT_BUFFER_KEY"));
		}
		@SuppressWarnings("unchecked")
		Chunk<String> chunk = (Chunk<String>) attributes.getAttribute("OUTPUT_BUFFER_KEY");
		assertEquals(1, chunk.getSkips().size());
		// The last recovery for this chunk...
		handler.execute(contribution, attributes);

		attributes = new BasicAttributeAccessor();
		try {
			handler.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		try {
			handler.execute(contribution, attributes);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}
		assertTrue(attributes.hasAttribute("OUTPUT_BUFFER_KEY"));
		assertEquals(3, contribution.getReadCount());
		assertEquals(0, contribution.getFilterCount());
		assertEquals(2, contribution.getWriteSkipCount());
		assertEquals(5, written.size());
	}

	@Test
	public void testSkipSingleItemOnProcess() throws Exception {
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, new ItemProcessor<Integer, String>() {
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
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();

		// try
		try {
			handler.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		assertTrue(attributes.hasAttribute("INPUT_BUFFER_KEY"));

		@SuppressWarnings("unchecked")
		Chunk<Integer> chunk = (Chunk<Integer>) attributes.getAttribute("INPUT_BUFFER_KEY");

		// skip...
		handler.execute(contribution, attributes);
		assertEquals(1, chunk.getSkips().size());

		assertEquals(3, contribution.getReadCount());
		assertEquals(1, contribution.getProcessSkipCount());
		assertEquals(5, processed.size());
		assertEquals("[p1, p2]", written.toString());
	}

	@Test
	public void testSkipOverLimitOnProcess() throws Exception {
		handler = new FaultTolerantChunkOrientedTasklet<Integer, String>(itemReader, new ItemProcessor<Integer, String>() {
			public String process(Integer item) throws Exception {
				logger.debug("Processing item: " + item);
				processed.add(item);
				throw new RuntimeException("Barf!");
			}
		}, itemWriter, chunkOperations, retryTemplate, rollbackClassifier, readSkipPolicy, writeSkipPolicy,
				writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(2));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();

		// Count to 3: (try + skip + try)
		for (int i = 0; i < 2; i++) {
			try {
				handler.execute(contribution, attributes);
				fail("Expected RuntimeException on i=" + i);
			}
			catch (Exception e) {
				assertEquals("Barf!", e.getMessage());
			}
			assertTrue(attributes.hasAttribute("INPUT_BUFFER_KEY"));
		}
		@SuppressWarnings("unchecked")
		Chunk<Integer> chunk = (Chunk<Integer>) attributes.getAttribute("INPUT_BUFFER_KEY");
		assertEquals(1, chunk.getSkips().size());

		// The last recovery for this chunk...
		handler.execute(contribution, attributes);
		assertEquals(2, chunk.getSkips().size());

		attributes = new BasicAttributeAccessor();
		try {
			handler.execute(contribution, attributes);
			fail("Expected RuntimeException");
		}
		catch (Exception e) {
			assertEquals("Barf!", e.getMessage());
		}
		try {
			handler.execute(contribution, attributes);
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
}
