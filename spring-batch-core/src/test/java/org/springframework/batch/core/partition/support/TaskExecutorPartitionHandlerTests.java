package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

public class TaskExecutorPartitionHandlerTests {

	private TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

	private int count = 0;
	
	private Collection<String> stepExecutions = new TreeSet<String>();

	private StepExecution stepExecution = new StepExecution("step", new JobExecution(1L));

	private StepExecutionSplitter stepExecutionSplitter = new StepExecutionSplitter() {

		public String getStepName() {
			return stepExecution.getStepName();
		}

		public Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException {
			HashSet<StepExecution> result = new HashSet<StepExecution>();
			for (int i = gridSize; i-- > 0;) {
				result.add(stepExecution.getJobExecution().createStepExecution("foo" + i));
			}
			return result;
		}
	};

	@Before
	public void setUp() throws Exception {
		handler.setStep(new StepSupport() {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				count++;
				stepExecutions.add(stepExecution.getStepName());
			}
		});
		handler.afterPropertiesSet();
	}

	@Test
	public void testNullStep() throws Exception {
		handler = new TaskExecutorPartitionHandler();
		try {
			handler.handle(stepExecutionSplitter, stepExecution);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("Step"));
		}
	}

	@Test
	public void testSetGridSize() throws Exception {
		handler.setGridSize(2);
		handler.handle(stepExecutionSplitter, stepExecution);
		assertEquals(2, count);
		assertEquals("[foo0, foo1]", stepExecutions.toString());
	}

	@Test
	public void testSetTaskExecutor() throws Exception {
		handler.setTaskExecutor(new SimpleAsyncTaskExecutor());
		handler.handle(stepExecutionSplitter, stepExecution);
		assertEquals(1, count);
	}

	@Test
	public void testTaskExecutorFailure() throws Exception {
		handler.setGridSize(2);
		handler.setTaskExecutor(new TaskExecutor() {
			public void execute(Runnable task) {
				if (count > 0) {
					throw new TaskRejectedException("foo");
				}
				task.run();
			}
		});
		Collection<StepExecution> executions = handler.handle(stepExecutionSplitter, stepExecution);
		new DefaultStepExecutionAggregator().aggregate(stepExecution, executions);
		assertEquals(1, count);
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
	}

}
