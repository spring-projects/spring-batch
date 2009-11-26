package org.springframework.batch.core.partition.support;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;

public class SimpleStepExecutionSplitterTests {

	private Step step;

	private JobRepository jobRepository;

	private StepExecution stepExecution = new StepExecution("bar", new JobExecution(11L));

	@Before
	public void setUp() throws Exception {
		step = new TaskletStep("step");
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		jobRepository = (JobRepository) factory.getObject();
	}

	@Test
	public void testSimpleStepExecutionProviderJobRepositoryStep() throws Exception {
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, step);
		Set<StepExecution> execs = splitter.split(stepExecution, 2);
		assertEquals(2, execs.size());
		
		for (StepExecution execution : execs) {
			assertNotNull("step execution partition is saved", execution.getId());
		}
	}

	@Test
	public void testSimpleStepExecutionProviderJobRepositoryStepPartitioner() throws Exception {
		final Map<String, ExecutionContext> map = Collections.singletonMap("foo", new ExecutionContext());
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, step, new Partitioner() {
			public Map<String, ExecutionContext> partition(int gridSize) {
				return map;
			}
		});
		assertEquals(1, splitter.split(stepExecution, 2).size());
	}

	@Test
	public void testRememberGridSize() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, step);
		assertEquals(2, provider.split(stepExecution, 2).size());
		assertEquals(2, provider.split(stepExecution, 3).size());
	}

	@Test
	public void testGetStepName() {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, step);
		assertEquals("step", provider.getStepName());
	}

}
