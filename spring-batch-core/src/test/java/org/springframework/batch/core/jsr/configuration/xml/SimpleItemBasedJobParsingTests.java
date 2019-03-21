/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleItemBasedJobParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public Step step1;

	@Autowired
	public CountingItemProcessor processor;

	@Autowired
	public CountingCompletionPolicy policy;

	@Autowired
	public CountingItemWriter writer;

	@Autowired
	public JobLauncher jobLauncher;

	@Test
	public void test() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(4, execution.getStepExecutions().size());
		assertEquals(27, processor.count);
		assertEquals(1, policy.checkpointCount);
		assertEquals(7, writer.writeCount);
		assertEquals(27, writer.itemCount);
	}

	public static class CountingItemWriter implements ItemWriter {

		protected int writeCount = 0;
		protected int itemCount = 0;

		@Override
		public void open(Serializable checkpoint) throws Exception {
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public void writeItems(List<Object> items) throws Exception {
			System.err.println("Items to be written: " + items);
			writeCount++;
			itemCount += items.size();
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}
	}

	public static class CountingCompletionPolicy implements CheckpointAlgorithm {

		protected int itemCount = 0;
		protected int checkpointCount = 0;

		@Override
		public int checkpointTimeout() throws Exception {
			return 0;
		}

		@Override
		public void beginCheckpoint() throws Exception {
		}

		@Override
		public boolean isReadyToCheckpoint() throws Exception {
			itemCount++;
			return itemCount % 3 == 0;
		}

		@Override
		public void endCheckpoint() throws Exception {
			checkpointCount++;
		}
	}
}
