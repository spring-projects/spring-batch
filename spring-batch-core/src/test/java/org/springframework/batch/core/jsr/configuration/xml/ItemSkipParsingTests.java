/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ItemSkipParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public TestSkipListener skipListener;

	@Test
	@Ignore
	public void test() throws Exception {
		// Read skip and fail
		JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().iterator().next().getSkipCount());
		assertEquals(1, skipListener.readSkips);
		assertEquals(0, skipListener.processSkips);
		assertEquals(0, skipListener.writeSkips);
		assertEquals("read fail because of me", execution.getAllFailureExceptions().get(0).getCause().getMessage());

		skipListener.resetCounts();

		// Process skip and fail
		execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().iterator().next().getSkipCount());
		assertEquals(0, skipListener.readSkips);
		assertEquals(1, skipListener.processSkips);
		assertEquals(0, skipListener.writeSkips);
		assertEquals("process fail because of me", execution.getAllFailureExceptions().get(0).getCause().getMessage());

		skipListener.resetCounts();

		// Write skip and fail
		execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().iterator().next().getSkipCount());
		assertEquals(0, skipListener.readSkips);
		assertEquals(0, skipListener.processSkips);
		assertEquals(1, skipListener.writeSkips);
		assertEquals("write fail because of me", execution.getAllFailureExceptions().get(0).getCause().getMessage());

		skipListener.resetCounts();

		// Complete
		execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(0, execution.getStepExecutions().iterator().next().getSkipCount());
		assertEquals(0, skipListener.readSkips);
		assertEquals(0, skipListener.processSkips);
		assertEquals(0, skipListener.writeSkips);
	}

	public static class SkipErrorGeneratingReader implements ItemReader<String> {
		private int count = 0;

		@Override
		public String read() throws Exception, UnexpectedInputException,
		ParseException, NonTransientResourceException {
			count++;

			if(count == 1) {
				throw new Exception("read skip me");
			} else if (count == 2) {
				return "item" + count;
			} else if(count == 3) {
				throw new RuntimeException("read fail because of me");
			} else if(count < 15) {
				return "item" + count;
			} else {
				return null;
			}
		}
	}

	public static class SkipErrorGeneratingProcessor implements ItemProcessor<String, String> {
		private int count = 0;

		@Override
		public String process(String item) throws Exception {
			count++;

			if(count == 4) {
				throw new Exception("process skip me");
			} else if(count == 5) {
				return item;
			} else if(count == 6) {
				throw new RuntimeException("process fail because of me");
			} else {
				return item;
			}
		}
	}

	public static class SkipErrorGeneratingWriter implements ItemWriter<String> {
		private int count = 0;
		protected List<String> writtenItems = new ArrayList<String>();
		private List<String> skippedItems = new ArrayList<String>();

		@Override
		public void write(List<? extends String> items) throws Exception {
			if(items.size() > 0 && !skippedItems.contains(items.get(0))) {
				count++;
			}

			if(count == 7) {
				skippedItems.addAll(items);
				throw new Exception("write skip me");
			} else if(count == 9) {
				skippedItems = new ArrayList<String>();
				throw new RuntimeException("write fail because of me");
			} else {
				writtenItems.addAll(items);
			}
		}
	}

	public static class TestSkipListener implements SkipListener<String, String> {

		protected int readSkips = 0;
		protected int processSkips = 0;
		protected int writeSkips = 0;

		@Override
		public void onSkipInRead(Throwable t) {
			readSkips++;
		}

		@Override
		public void onSkipInWrite(String item, Throwable t) {
			writeSkips++;
		}

		@Override
		public void onSkipInProcess(String item, Throwable t) {
			processSkips++;
		}

		public void resetCounts() {
			readSkips = 0;
			processSkips = 0;
			writeSkips = 0;
		}
	}
}
