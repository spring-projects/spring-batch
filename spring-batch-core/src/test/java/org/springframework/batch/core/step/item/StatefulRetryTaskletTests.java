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
import org.springframework.batch.core.step.item.SkipLimitStepFactoryBean.StatefulRetryTasklet;
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

/**
 * @author Dave Syer
 * 
 */
public class StatefulRetryTaskletTests {
	
	private Log logger = LogFactory.getLog(getClass());

	private int count = 0;

	private int limit = 3;

	protected int skipLimit = 2;

	protected List<String> written = new ArrayList<String>();

	private StatefulRetryTasklet<Integer, String> handler;

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
		handler = new StatefulRetryTasklet<Integer, String>(itemReader, itemProcessor, itemWriter, chunkOperations,
				retryTemplate, readSkipPolicy, writeSkipPolicy);
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		handler.execute(contribution, new BasicAttributeAccessor());
		assertEquals(limit, contribution.getItemCount());
	}

	@Test
	public void testSkipOnRead() throws Exception {
		handler = new StatefulRetryTasklet<Integer, String>(new ItemReader<Integer>() {
			public Integer read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
				throw new RuntimeException("Barf!");
			}
		}, itemProcessor, itemWriter, chunkOperations, retryTemplate, readSkipPolicy, writeSkipPolicy);
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
		assertEquals(0, contribution.getItemCount());
		assertEquals(2, contribution.getReadSkipCount());
	}

	@Test
	public void testSkipSingleItemOnWrite() throws Exception {
		handler = new StatefulRetryTasklet<Integer, String>(itemReader, itemProcessor, new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				written.addAll(items);
				throw new RuntimeException("Barf!");
			}
		}, chunkOperations, retryTemplate, readSkipPolicy, writeSkipPolicy);
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
		assertEquals(1, contribution.getItemCount());
		assertEquals(1, contribution.getWriteSkipCount());
		assertEquals(1, written.size());
	}

	@Test
	public void testSkipMultipleItems() throws Exception {
		handler = new StatefulRetryTasklet<Integer, String>(itemReader, itemProcessor, new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				logger.debug("Writing items: "+items);
				written.addAll(items);
				throw new RuntimeException("Barf!");
			}
		}, chunkOperations, retryTemplate, readSkipPolicy, writeSkipPolicy);
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(2));
		StepContribution contribution = new StepExecution("foo", null).createStepContribution();
		BasicAttributeAccessor attributes = new BasicAttributeAccessor();
		
		// Count to 3: (try + skip + skip) 
		for (int i = 0; i < 3; i++) {
			try {
				handler.execute(contribution, attributes);
				fail("Expected RuntimeException on i="+i);
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
			fail("Expected RuntimeException on i=");
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
		assertEquals(3, contribution.getItemCount());
		assertEquals(2, contribution.getWriteSkipCount());
		assertEquals(5, written.size());
	}

}
