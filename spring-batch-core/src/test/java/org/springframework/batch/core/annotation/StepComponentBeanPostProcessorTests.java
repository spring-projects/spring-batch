/**
 * 
 */
package org.springframework.batch.core.annotation;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;

/**
 * @author Lucas Ward
 * 
 */
public class StepComponentBeanPostProcessorTests {

	@BatchComponent
	private class TestComponent {

		boolean afterStepCalled = false;

		@AfterStep
		public void testMethod() {
			afterStepCalled = true;
		}
	}

	@Test
	public void testNormalCase() throws Exception {
		AbstractStep step = new StubStep();
		step.setJobRepository(createMock(JobRepository.class));
		StepComponentBeanPostProcessor postProcessor = new StepComponentBeanPostProcessor(
				step, "org.springframework.batch.core.annotation");
		TestComponent testComponent = new TestComponent();
		postProcessor.postProcessAfterInitialization(testComponent,
				"testComponent");
		step.execute(new StepExecution("teststep", new JobExecution(11L)));
		assertTrue(testComponent.afterStepCalled);
	}

	private class StubStep extends AbstractStep {

		@Override
		protected void doExecute(StepExecution stepExecution) throws Exception {
			stepExecution.setExitStatus(ExitStatus.FINISHED);
		}

	}
}
