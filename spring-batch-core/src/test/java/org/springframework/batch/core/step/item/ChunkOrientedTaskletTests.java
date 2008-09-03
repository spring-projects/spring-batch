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
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.PassthroughItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.core.AttributeAccessor;

/**
 * @author Dave Syer
 * 
 */
public class ChunkOrientedTaskletTests {

	private StubItemReader itemReader = new StubItemReader();

	private StubItemWriter itemWriter = new StubItemWriter();

	private RepeatTemplate repeatTemplate = new RepeatTemplate();

	private AttributeAccessor context = new RepeatContextSupport(null);

	@Before
	public void setUp() {
		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
	}

	@Test
	public void testHandle() throws Exception {
		ChunkOrientedTasklet<String, String> handler = new ChunkOrientedTasklet<String, String>(itemReader,
				new PassthroughItemProcessor<String>(), itemWriter, repeatTemplate);
		StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(new JobInstance(
				123L, new JobParameters(), "job"))));
		handler.execute(contribution, context);
		assertEquals(2, itemReader.count);
		assertEquals("12", itemWriter.values);
	}

	@Test
	public void testHandleWithItemProcessorFailure() throws Exception {
		ChunkOrientedTasklet<String, String> handler = new ChunkOrientedTasklet<String, String>(itemReader,
				new StubItemProcessor(), itemWriter, repeatTemplate);
		StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(new JobInstance(
				123L, new JobParameters(), "job"))));
		try {
			handler.execute(contribution, context);
			fail("Expected ValidationException");
		}
		catch (ValidationException e) {
			// expected
		}
		assertEquals(2, itemReader.count);
		assertEquals(2, contribution.getItemCount());
		assertEquals(0, contribution.getFilterCount());
		assertEquals("", itemWriter.values);
	}

	@Test
	public void testHandleCompositeItem() throws Exception {
		ChunkOrientedTasklet<String, String> handler = new ChunkOrientedTasklet<String, String>(itemReader,
				new AggregateItemProcessor(), itemWriter, repeatTemplate);
		StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(new JobInstance(
				123L, new JobParameters(), "job"))));
		handler.execute(contribution, context);
		assertEquals(2, itemReader.count);
		assertEquals(2, contribution.getItemCount());
		assertEquals(2, contribution.getReadCount());
		assertEquals(1, contribution.getFilterCount());
		assertEquals(1, contribution.getWriteCount());
		assertEquals("12", itemWriter.values);
	}
	

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class AggregateItemProcessor implements ItemProcessor<String, String> {
		private int count = 0;

		private String value = "";

		public String process(String item) throws Exception {
			value += item;
			if (count++ < 1) {
				return null;
			}
			String result = value;
			value = "";
			count = 0;
			return result;
		}
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class StubItemProcessor implements ItemProcessor<String, String> {
		public String process(String item) throws Exception {
			if ("2".equals(item)) {
				throw new ValidationException("Planned failure");
			}
			return item;
		}
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class StubItemWriter implements ItemWriter<String> {
		private String values = "";

		public void write(List<? extends String> items) throws Exception {
			for (String item : items) {
				values += item;
			}
		}
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private final class StubItemReader implements ItemReader<String> {
		private int count = 0;

		public String read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
			if (count++ < 5)
				return "" + count;
			return null;
		}
	}

}
