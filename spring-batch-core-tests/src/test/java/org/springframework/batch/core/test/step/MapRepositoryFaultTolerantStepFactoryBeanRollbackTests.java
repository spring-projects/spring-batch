/*
 * Copyright 2010-2019 the original author or authors.
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
package org.springframework.batch.core.test.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
public class MapRepositoryFaultTolerantStepFactoryBeanRollbackTests {

	private static final int MAX_COUNT = 1000;

	private final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private SkipReaderStub reader;

	private SkipProcessorStub processor;

	private SkipWriterStub writer;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		
		reader = new SkipReaderStub();
		writer = new SkipWriterStub();
		processor = new SkipProcessorStub();

		factory = new FaultTolerantStepFactoryBean<>();

		factory.setTransactionManager(transactionManager);
		factory.setBeanName("stepName");
		factory.setCommitInterval(3);
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(6);
		taskExecutor.setQueueCapacity(0);
		taskExecutor.afterPropertiesSet();
		factory.setTaskExecutor(taskExecutor);

		factory.setSkipLimit(10);
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

	}
	
	@Test
	public void testUpdatesNoRollback() throws Exception {

		writer.write(Arrays.asList("foo", "bar"));
		processor.process("spam");
		assertEquals(2, writer.getWritten().size());
		assertEquals(1, processor.getProcessed().size());

		writer.clear();
		processor.clear();
		assertEquals(0, processor.getProcessed().size());

	}

	@Test
	public void testMultithreadedSkipInWrite() throws Throwable {

		for (int i = 0; i < MAX_COUNT; i++) {
			
			if (i%100==0) {
				logger.info("Starting step: "+i);
				repository = new MapJobRepositoryFactoryBean(transactionManager).getObject();
				factory.setJobRepository(repository);
				jobExecution = repository.createJobExecution("vanillaJob", new JobParameters());
			}

			reader.clear();
			reader.setItems("1", "2", "3", "4", "5");
			factory.setItemReader(reader);
			writer.clear();
			factory.setItemWriter(writer);
			processor.clear();
			factory.setItemProcessor(processor);

			writer.setFailures("1", "2", "3", "4", "5");

			try {

				Step step = factory.getObject();

				stepExecution = jobExecution.createStepExecution(factory.getName());
				repository.add(stepExecution);
				step.execute(stepExecution);
				assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

				assertEquals(5, stepExecution.getSkipCount());
				List<String> processed = new ArrayList<>(processor.getProcessed());
				Collections.sort(processed);
				assertEquals("[1, 1, 2, 2, 3, 3, 4, 4, 5, 5]", processed.toString());

			}
			catch (Throwable e) {
				logger.info("Failed on iteration " + i + " of " + MAX_COUNT);
				throw e;
			}

		}

	}

	private static class SkipReaderStub implements ItemReader<String> {

		private String[] items;

		private int counter = -1;

		public SkipReaderStub() throws Exception {
			super();
		}

		public void setItems(String... items) {
			Assert.isTrue(counter < 0, "Items cannot be set once reading has started");
			this.items = items;
		}

		public void clear() {
			counter = -1;
		}

		@Nullable
		@Override
		public synchronized String read() throws Exception {
			counter++;
			if (counter >= items.length) {
				return null;
			}
			return items[counter];
		}
	}

	private static class SkipWriterStub implements ItemWriter<String> {

		private final Log logger = LogFactory.getLog(getClass());

		private List<String> written = new CopyOnWriteArrayList<>();

		private Collection<String> failures = Collections.emptySet();

		public void setFailures(String... failures) {
			this.failures = Arrays.asList(failures);
		}

		public List<String> getWritten() {
			return written;
		}

		public void clear() {
			written.clear();
		}

		@Override
		public void write(List<? extends String> items) throws Exception {
			for (String item : items) {
				logger.trace("Writing: "+item);
				written.add(item);
				checkFailure(item);
			}
		}

		private void checkFailure(String item) {
			if (failures.contains(item)) {
				throw new RuntimeException("Planned failure");
			}
		}
	}

	private static class SkipProcessorStub implements ItemProcessor<String, String> {

		private final Log logger = LogFactory.getLog(getClass());

		private List<String> processed = new CopyOnWriteArrayList<>();
		
		public List<String> getProcessed() {
			return processed;
		}

		public void clear() {
			processed.clear();
		}

		@Nullable
		@Override
		public String process(String item) throws Exception {
			processed.add(item);
			logger.debug("Processed item: "+item);
			return item;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}

}
