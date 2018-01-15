package org.springframework.batch.core.test.step;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for fault tolerant {@link org.springframework.batch.core.step.item.ChunkOrientedTasklet}.
 */
@ContextConfiguration(locations = "/simple-job-launcher-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class FaultTolerantStepIntegrationTests {
	
	private static final int TOTAL_ITEMS = 30;
	private static final int CHUNK_SIZE = TOTAL_ITEMS;
	
	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
	private PlatformTransactionManager transactionManager;
	
	private SkipPolicy skipPolicy;
	
	private FaultTolerantStepBuilder<Integer, Integer> stepBuilder;
	
	@Before
	public void setUp() {
		ItemReader<Integer> itemReader = new ListItemReader<>(createItems());
		ItemProcessor<Integer, Integer> itemProcessor = item -> item > 20 ? null : item;
		ItemWriter<Integer> itemWriter = chunk -> {
			if (chunk.contains(1)) {
				throw new IllegalArgumentException();
			}
		};
		skipPolicy = new SkipIllegalArgumentExceptionSkipPolicy();
		stepBuilder = new StepBuilderFactory(jobRepository, transactionManager).get("step")
				.<Integer, Integer>chunk(CHUNK_SIZE)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.faultTolerant();
	}
	
	@Test
	public void testFilterCountWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.skipPolicy(skipPolicy)
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	@Test
	public void testFilterCountWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.skipPolicy(skipPolicy)
				.processorNonTransactional()
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	@Test
	public void testFilterCountOnRetryWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.retry(IllegalArgumentException.class)
				.retryLimit(2)
				.skipPolicy(skipPolicy)
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		// filter count is expected to be counted on each retry attempt
		assertEquals(20, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	@Test
	public void testFilterCountOnRetryWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.retry(IllegalArgumentException.class)
				.retryLimit(2)
				.skipPolicy(skipPolicy)
				.processorNonTransactional()
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		// filter count is expected to be counted on each retry attempt
		assertEquals(20, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	private List<Integer> createItems() {
		List<Integer> items = new ArrayList<>(TOTAL_ITEMS);
		for (int i = 1; i <= TOTAL_ITEMS; i++) {
			items.add(i);
		}
		return items;
	}
	
	private StepExecution execute(Step step) throws Exception {
		JobExecution jobExecution = jobRepository.createJobExecution(
								"job" + Math.random(), new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		return stepExecution;
	}
	
	private class SkipIllegalArgumentExceptionSkipPolicy implements SkipPolicy {
		
		@Override
		public boolean shouldSkip(Throwable throwable, int skipCount)
				throws SkipLimitExceededException {
			return throwable instanceof IllegalArgumentException;
		}
		
	}
}
