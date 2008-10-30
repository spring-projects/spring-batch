/**
 * 
 */
package org.springframework.batch.core.job;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.repository.JobRepository;

/**
 * @author Lucas Ward
 * 
 */
public class JobListenerAnnotationBeanPostProcessorTests {

	JobListenerAnnotationBeanPostProcessor postProcessor;
	AnnotatedClassStub annotatedClass;
	StubJob job;
	JobExecution jobExecution;
	
	@Before
	public void init(){
		job = new StubJob();
		annotatedClass = new AnnotatedClassStub();
		postProcessor = new JobListenerAnnotationBeanPostProcessor(job);
		jobExecution = new JobExecution(1L);
	}
	
	@Test
	public void testBeforeJob(){
		
		postProcessor.postProcessBeforeInitialization(annotatedClass, "test");
		job.execute(jobExecution);
		assertTrue(annotatedClass.beforeJobCalled);
		assertTrue(annotatedClass.beforeJobWithExecutionCalled);
		assertTrue(annotatedClass.afterJobCalled);
		assertTrue(annotatedClass.afterJobWithExecutionCalled);
	}
	
	private class AnnotatedClassStub{
		
		boolean beforeJobCalled = false;
		boolean beforeJobWithExecutionCalled = false;
		
		boolean afterJobCalled = false;
		boolean afterJobWithExecutionCalled = false;
		
		@BeforeJob
		public void beforeJobMethod(){
			beforeJobCalled = true;
		};
		
		@BeforeJob
		public void beforeJobMethodWithExecution(JobExecution jobExecution){
			beforeJobWithExecutionCalled = true;
		}
		
		@AfterJob
		public void afterJobMethod(){
			afterJobCalled = true;
		}
		
		@AfterJob
		public void afterJobMethodWithExecution(JobExecution jobExecution){
			afterJobWithExecutionCalled = true;
		}
	}
	
	private class StubJob extends AbstractJob{

		public StubJob() {
			super.setJobRepository(createMock(JobRepository.class));
		}
		
		@Override
		protected StepExecution doExecute(JobExecution execution)
				throws JobExecutionException {
			return null;
		}
		
		
	}
}
