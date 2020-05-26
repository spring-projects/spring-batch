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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.jsr.AbstractJsrTestCase;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PartitionParserTests extends AbstractJsrTestCase {
	private Pattern caPattern = Pattern.compile("ca");
	private Pattern asPattern = Pattern.compile("AS");
	private static final long TIMEOUT = 10000L;

	@Before
	public void before() {
		MyBatchlet.processed = 0;
		MyBatchlet.threadNames = Collections.synchronizedSet(new HashSet<>());
		MyBatchlet.artifactNames = Collections.synchronizedSet(new HashSet<>());
		PartitionCollector.artifactNames = Collections.synchronizedSet(new HashSet<>());
	}

	@Test
	public void testBatchletNoProperties() throws Exception {
		BatchStatus curBatchStatus = runJob("partitionParserTestsBatchlet", new Properties(), TIMEOUT).getBatchStatus();

		assertEquals(BatchStatus.COMPLETED, curBatchStatus);
		assertEquals(10, MyBatchlet.processed);
		assertEquals(10, MyBatchlet.threadNames.size());
	}

	@Test
	public void testChunkNoProperties() throws Exception {
		JobExecution execution = runJob("partitionParserTestsChunk", new Properties(), TIMEOUT);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(30, ItemReader.processedItems.size());
		assertEquals(10, ItemReader.threadNames.size());
		assertEquals(30, ItemWriter.processedItems.size());
		assertEquals(10, ItemWriter.threadNames.size());
	}

	@Test
	public void testFullPartitionConfiguration() throws Exception {
		JobExecution execution = runJob("fullPartitionParserTests", new Properties(), TIMEOUT);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertTrue(execution.getExitStatus().startsWith("BPS_"));
		assertTrue(execution.getExitStatus().endsWith("BPSC_APSC"));
		assertEquals(3, countMatches(execution.getExitStatus(), caPattern));
		assertEquals(3, countMatches(execution.getExitStatus(), asPattern));
		assertEquals(3, MyBatchlet.processed);
		assertEquals(3, MyBatchlet.threadNames.size());
	}

	@Test
	public void testFullPartitionConfigurationWithProperties() throws Exception {
		JobExecution execution = runJob("fullPartitionParserWithPropertiesTests", new Properties(), TIMEOUT);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertTrue(execution.getExitStatus().startsWith("BPS_"));
		assertTrue(execution.getExitStatus().endsWith("BPSC_APSC"));
		assertEquals(3, countMatches(execution.getExitStatus(), caPattern));
		assertEquals(3, countMatches(execution.getExitStatus(), asPattern));
		assertEquals(3, MyBatchlet.processed);
		assertEquals(3, MyBatchlet.threadNames.size());
		assertEquals(MyBatchlet.artifactNames.iterator().next(), "batchlet");
		assertEquals(PartitionMapper.name, "mapper");
		assertEquals(PartitionAnalyzer.name, "analyzer");
		assertEquals(PartitionReducer.name, "reducer");
		assertEquals(PartitionCollector.artifactNames.size(), 1);
		assertTrue(PartitionCollector.artifactNames.contains("collector"));
	}

	@Test
	public void testFullPartitionConfigurationWithMapperSuppliedProperties() throws Exception {
		JobExecution execution = runJob("fullPartitionParserWithMapperPropertiesTests", new Properties(), TIMEOUT);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertTrue(execution.getExitStatus().startsWith("BPS_"));
		assertTrue(execution.getExitStatus().endsWith("BPSC_APSC"));
		assertEquals(3, countMatches(execution.getExitStatus(), caPattern));
		assertEquals(3, countMatches(execution.getExitStatus(), asPattern));
		assertEquals(3, MyBatchlet.processed);
		assertEquals(3, MyBatchlet.threadNames.size());

		assertEquals(MyBatchlet.artifactNames.size(), 3);
		assertTrue(MyBatchlet.artifactNames.contains("batchlet0"));
		assertTrue(MyBatchlet.artifactNames.contains("batchlet1"));
		assertTrue(MyBatchlet.artifactNames.contains("batchlet2"));
		assertEquals(PartitionCollector.artifactNames.size(), 3);
		assertTrue(PartitionCollector.artifactNames.contains("collector0"));
		assertTrue(PartitionCollector.artifactNames.contains("collector1"));
		assertTrue(PartitionCollector.artifactNames.contains("collector2"));

		assertEquals(PartitionAnalyzer.name, "analyzer");
		assertEquals(PartitionReducer.name, "reducer");
	}

	@Test
	public void testFullPartitionConfigurationWithHardcodedProperties() throws Exception {
		JobExecution execution = runJob("fullPartitionParserWithHardcodedPropertiesTests", new Properties(), TIMEOUT);

		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertTrue(execution.getExitStatus().startsWith("BPS_"));
		assertTrue(execution.getExitStatus().endsWith("BPSC_APSC"));
		assertEquals(3, countMatches(execution.getExitStatus(), caPattern));
		assertEquals(3, countMatches(execution.getExitStatus(), asPattern));
		assertEquals(3, MyBatchlet.processed);
		assertEquals(3, MyBatchlet.threadNames.size());

		assertEquals(MyBatchlet.artifactNames.size(), 3);
		assertTrue(MyBatchlet.artifactNames.contains("batchlet0"));
		assertTrue(MyBatchlet.artifactNames.contains("batchlet1"));
		assertTrue(MyBatchlet.artifactNames.contains("batchlet2"));
		assertEquals(PartitionCollector.artifactNames.size(), 3);
		assertTrue(PartitionCollector.artifactNames.contains("collector0"));
		assertTrue(PartitionCollector.artifactNames.contains("collector1"));
		assertTrue(PartitionCollector.artifactNames.contains("collector2"));

		assertEquals(PartitionMapper.name, "mapper");
		assertEquals(PartitionAnalyzer.name, "analyzer");
		assertEquals(PartitionReducer.name, "reducer");
	}

	private int countMatches(String string, Pattern pattern) {
		Matcher matcher = pattern.matcher(string);

		int count = 0;
		while(matcher.find()) {
			count++;
		}

		return count;
	}

	public static class PartitionReducer implements javax.batch.api.partition.PartitionReducer {

		public static String name;

		@Inject
		@BatchProperty
		String artifactName;

		@Inject
		protected JobContext jobContext;

		@Override
		public void beginPartitionedStep() throws Exception {
			name = artifactName;
			jobContext.setExitStatus("BPS_");
		}

		@Override
		public void beforePartitionedStepCompletion() throws Exception {
			jobContext.setExitStatus(jobContext.getExitStatus() + "BPSC_");
		}

		@Override
		public void rollbackPartitionedStep() throws Exception {
			jobContext.setExitStatus(jobContext.getExitStatus() + "RPS");
		}

		@Override
		public void afterPartitionedStepCompletion(PartitionStatus status)
				throws Exception {
			jobContext.setExitStatus(jobContext.getExitStatus() + "APSC");
		}
	}

	public static class PartitionAnalyzer implements javax.batch.api.partition.PartitionAnalyzer {

		public static String name;

		@Inject
		@BatchProperty
		String artifactName;

		@Inject
		protected JobContext jobContext;

		@Override
		public void analyzeCollectorData(Serializable data) throws Exception {
			name = artifactName;

			Assert.isTrue(data.equals("c"), "Expected c but was " + data);
			jobContext.setExitStatus(jobContext.getExitStatus() + data + "a");
		}

		@Override
		public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
				throws Exception {
			Assert.isTrue(batchStatus.equals(BatchStatus.COMPLETED), String.format("expected %s but received %s", BatchStatus.COMPLETED, batchStatus));
			jobContext.setExitStatus(jobContext.getExitStatus() + "AS");
		}
	}

	public static class PartitionCollector implements javax.batch.api.partition.PartitionCollector {

		protected static Set<String> artifactNames = Collections.synchronizedSet(new HashSet<>());

		@Inject
		@BatchProperty
		String artifactName;

		@Override
		public Serializable collectPartitionData() throws Exception {
			artifactNames.add(artifactName);
			return "c";
		}
	}

	public static class PropertyPartitionMapper implements javax.batch.api.partition.PartitionMapper {

		@Override
		public PartitionPlan mapPartitions() throws Exception {
			Properties[] props = new Properties[3];

			for(int i = 0; i < props.length; i++) {
				props[i] = new Properties();
				props[i].put("collectorName", "collector" + i);
				props[i].put("batchletName", "batchlet" + i);
			}

			PartitionPlan plan = new PartitionPlanImpl();
			plan.setPartitions(3);
			plan.setThreads(3);
			plan.setPartitionProperties(props);

			return plan;
		}
	}

	public static class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

		public static String name;

		@Inject
		@BatchProperty
		public String artifactName;

		@Override
		public PartitionPlan mapPartitions() throws Exception {
			name = artifactName;

			PartitionPlan plan = new PartitionPlanImpl();
			plan.setPartitions(3);
			plan.setThreads(3);

			return plan;
		}
	}

	public static class MyBatchlet implements Batchlet {

		protected static int processed = 0;
		protected static Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
		protected static Set<String> artifactNames = Collections.synchronizedSet(new HashSet<>());

		@Inject
		@BatchProperty
		String artifactName;

		@Inject
		StepContext stepContext;

		@Inject
		JobContext jobContext;

		@Override
		public String process() throws Exception {
			artifactNames.add(artifactName);
			threadNames.add(Thread.currentThread().getName());
			processed++;

			stepContext.setExitStatus("bad step exit status");
			jobContext.setExitStatus("bad job exit status");

			return null;
		}

		@Override
		public void stop() throws Exception {
		}
	}

	public static class ItemReader extends AbstractItemReader {

		private List<Integer> items;
		protected static Vector<Integer> processedItems = new Vector<>();
		protected static Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());

		@Override
		public void open(Serializable checkpoint) throws Exception {
			items = new ArrayList<>();
			items.add(1);
			items.add(2);
			items.add(3);
		}

		@Override
		public Object readItem() throws Exception {
			threadNames.add(Thread.currentThread().getName());
			if(items.size() > 0) {
				Integer curItem = items.remove(0);
				processedItems.add(curItem);
				return curItem;
			} else {
				return null;
			}
		}
	}

	public static class ItemWriter extends AbstractItemWriter {

		protected static Vector<Integer> processedItems = new Vector<>();
		protected static Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());

		@Override
		public void writeItems(List<Object> items) throws Exception {
			threadNames.add(Thread.currentThread().getName());
			for (Object object : items) {
				processedItems.add((Integer) object);
			}
		}
	}
}
