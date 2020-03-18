/*
 * Copyright 2013-2019 the original author or authors.
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

import org.junit.Test;
import org.springframework.batch.core.jsr.AbstractJsrTestCase;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;

import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ItemSkipParsingTests extends AbstractJsrTestCase {

	@Test
	public void test() throws Exception {
		javax.batch.runtime.JobExecution execution = runJob("ItemSkipParsingTests-context", new Properties(), 10000l);
		JobOperator jobOperator = BatchRuntime.getJobOperator();

		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions(execution.getExecutionId());
		assertEquals(1, getMetric(stepExecutions.get(0), Metric.MetricType.READ_SKIP_COUNT).getValue());
		assertEquals(1, TestSkipListener.readSkips);
		assertEquals(0, TestSkipListener.processSkips);
		assertEquals(0, TestSkipListener.writeSkips);

		// Process skip and fail
		execution = restartJob(execution.getExecutionId(), new Properties(), 10000l);

		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		stepExecutions = jobOperator.getStepExecutions(execution.getExecutionId());
		assertEquals(1, getMetric(stepExecutions.get(0), Metric.MetricType.PROCESS_SKIP_COUNT).getValue());
		assertEquals(0, TestSkipListener.readSkips);
		assertEquals(1, TestSkipListener.processSkips);
		assertEquals(0, TestSkipListener.writeSkips);

		// Write skip and fail
		execution = restartJob(execution.getExecutionId(), new Properties(), 10000l);

		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		stepExecutions = jobOperator.getStepExecutions(execution.getExecutionId());
		assertEquals(1, getMetric(stepExecutions.get(0), Metric.MetricType.WRITE_SKIP_COUNT).getValue());
		assertEquals(0, TestSkipListener.readSkips);
		assertEquals(0, TestSkipListener.processSkips);
		assertEquals(1, TestSkipListener.writeSkips);

		// Complete
		execution = restartJob(execution.getExecutionId(), new Properties(), 10000l);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		stepExecutions = jobOperator.getStepExecutions(execution.getExecutionId());
		assertEquals(0, getMetric(stepExecutions.get(0), Metric.MetricType.WRITE_SKIP_COUNT).getValue());
		assertEquals(0, TestSkipListener.readSkips);
		assertEquals(0, TestSkipListener.processSkips);
		assertEquals(0, TestSkipListener.writeSkips);
	}

	public static class SkipErrorGeneratingReader implements ItemReader<String> {
		private static int count = 0;

		@Nullable
		@Override
		public String read() throws Exception {
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
		private static int count = 0;

		@Nullable
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
		private static int count = 0;
		protected List<String> writtenItems = new ArrayList<>();
		private List<String> skippedItems = new ArrayList<>();

		@Override
		public void write(List<? extends String> items) throws Exception {
			if(items.size() > 0 && !skippedItems.contains(items.get(0))) {
				count++;
			}

			if(count == 7) {
				skippedItems.addAll(items);
				throw new Exception("write skip me");
			} else if(count == 9) {
				skippedItems = new ArrayList<>();
				throw new RuntimeException("write fail because of me");
			} else {
				writtenItems.addAll(items);
			}
		}
	}

	public static class TestSkipListener implements SkipReadListener, SkipProcessListener, SkipWriteListener {

		protected static int readSkips = 0;
		protected static int processSkips = 0;
		protected static int writeSkips = 0;

		public TestSkipListener() {
			readSkips = 0;
			processSkips = 0;
			writeSkips = 0;
		}

		@Override
		public void onSkipProcessItem(Object item, Exception ex) throws Exception {
			processSkips++;
		}

		@Override
		public void onSkipReadItem(Exception ex) throws Exception {
			readSkips++;
		}

		@Override
		public void onSkipWriteItem(List<Object> items, Exception ex) throws Exception {
			writeSkips++;
		}
	}
}
