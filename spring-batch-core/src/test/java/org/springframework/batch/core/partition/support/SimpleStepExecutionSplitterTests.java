package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;

public class SimpleStepExecutionSplitterTests {

	private Step step;

	private JobRepository jobRepository;

	private StepExecution stepExecution;

	@Before
	public void setUp() throws Exception {
		step = new TaskletStep("step");
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		jobRepository = (JobRepository) factory.getObject();
		stepExecution = jobRepository.createJobExecution("job", new JobParameters()).createStepExecution("bar");
		jobRepository.add(stepExecution);
	}

	@Test
	public void testSimpleStepExecutionProviderJobRepositoryStep() throws Exception {
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> execs = splitter.split(stepExecution, 2);
		assertEquals(2, execs.size());

		for (StepExecution execution : execs) {
			assertNotNull("step execution partition is saved", execution.getId());
		}
	}

	@Test
	public void testSimpleStepExecutionProviderJobRepositoryStepPartitioner() throws Exception {
		final Map<String, ExecutionContext> map = Collections.singletonMap("foo", new ExecutionContext());
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new Partitioner() {
					public Map<String, ExecutionContext> partition(int gridSize) {
						return map;
					}
				});
		assertEquals(1, splitter.split(stepExecution, 2).size());
	}

	@Test
	public void testRememberGridSize() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.FAILED);
		assertEquals(2, provider.split(stepExecution, 3).size());
	}

	@Test
	public void testRememberPartitionNames() throws Exception {
		class CustomPartitioner implements Partitioner, PartitionNameProvider {
			public Map<String, ExecutionContext> partition(int gridSize) {
				return Collections.singletonMap("foo", new ExecutionContext());
			}

			public Collection<String> getPartitionNames(int gridSize) {
				return Arrays.asList("foo");
			}
		}
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new CustomPartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(1, split.size());
		assertEquals("step:foo", split.iterator().next().getStepName());
		stepExecution = update(split, stepExecution, BatchStatus.FAILED);
		split = provider.split(stepExecution, 2);
		assertEquals("step:foo", split.iterator().next().getStepName());
	}

	@Test
	public void testGetStepName() {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		assertEquals("step", provider.getStepName());
	}

	@Test
	public void testUnkownStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.UNKNOWN);
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("UNKNOWN"));
		}
	}

	@Test
	public void testCompleteStatusAfterFailure() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, false, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		StepExecution nextExecution = update(split, stepExecution, BatchStatus.COMPLETED, false);
		// If already complete in another JobExecution we don't execute again
		assertEquals(0, provider.split(nextExecution, 2).size());
	}

	@Test
	public void testCompleteStatusSameJobExecution() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, false, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.COMPLETED);
		// If already complete in the same JobExecution we should execute again
		assertEquals(2, provider.split(stepExecution, 2).size());
	}

	@Test
	public void testIncompleteStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.STARTED);
		// If not already complete we don't execute again
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("STARTED"));
		}
	}

	@Test
	public void testAbandonedStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.ABANDONED);
		// If not already complete we don't execute again
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("ABANDONED"));
		}
	}

	private StepExecution update(Set<StepExecution> split, StepExecution stepExecution, BatchStatus status)
			throws Exception {
		return update(split, stepExecution, status, true);
	}

	private StepExecution update(Set<StepExecution> split, StepExecution stepExecution, BatchStatus status,
			boolean sameJobExecution) throws Exception {

		ExecutionContext executionContext = stepExecution.getExecutionContext();

		for (StepExecution child : split) {
			child.setEndTime(new Date());
			child.setStatus(status);
			jobRepository.update(child);
		}

		stepExecution.setEndTime(new Date());
		stepExecution.setStatus(status);
		jobRepository.update(stepExecution);

		JobExecution jobExecution = stepExecution.getJobExecution();
		if (!sameJobExecution) {
			jobExecution.setStatus(BatchStatus.FAILED);
			jobExecution.setEndTime(new Date());
			jobRepository.update(jobExecution);
			JobInstance jobInstance = jobExecution.getJobInstance();
			jobExecution = jobRepository.createJobExecution(jobInstance.getJobName(), jobInstance.getJobParameters());
		}

		stepExecution = jobExecution.createStepExecution(stepExecution.getStepName());
		stepExecution.setExecutionContext(executionContext);

		jobRepository.add(stepExecution);
		return stepExecution;

	}

}
