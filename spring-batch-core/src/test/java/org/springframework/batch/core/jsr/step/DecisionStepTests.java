package org.springframework.batch.core.jsr.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

@SuppressWarnings("resource")
public class DecisionStepTests {

	@Test
	public void testDecisionAsFirstStepOfJob() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionAsFirstStep-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
	}

	@Test
	public void testDecisionThrowsException() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionThrowsException-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
		List<Throwable> allFailureExceptions = execution.getAllFailureExceptions();

		boolean found = false;
		for (Throwable throwable : allFailureExceptions) {
			if(throwable.getMessage().equals("Expected")) {
				found = true;
				break;
			}
		}

		if(!found) {
			fail();
		}
	}

	@Test
	public void testDecisionValidExitStatus() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionValidExitStatus-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
	}

	@Test
	public void testDecisionUnmappedExitStatus() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionInvalidExitStatus-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());

		for (org.springframework.batch.core.StepExecution curExecution : execution.getStepExecutions()) {
			assertEquals(BatchStatus.COMPLETED, curExecution.getStatus());
		}
	}

	@Test
	public void testDecisionCustomExitStatus() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionCustomExitStatus-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(2, execution.getStepExecutions().size());
		assertEquals("CustomFail", execution.getExitStatus().getExitCode());
	}

	@Test
	@Ignore("Flows as first steps are not supported yet")
	public void testDecisionAfterFlow() throws Exception {
		ApplicationContext context = new GenericXmlApplicationContext("classpath:/org/springframework/batch/core/jsr/step/DecisionStepTests-decisionAfterFlow-context.xml");

		JobLauncher launcher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution execution = launcher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
	}

	@Test
	@Ignore("Splits are not implemented yet as part of our JSR implementation")
	public void testDecisionAfterSplit() {
	}

	public static class NextDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			for(StepExecution stepExecution : executions) {
				if ("customFailTest".equals(stepExecution.getStepName())) {
					return "CustomFail";
				}
			}

			return "next";
		}
	}

	public static class FailureDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			throw new RuntimeException("Expected");
		}
	}
}
