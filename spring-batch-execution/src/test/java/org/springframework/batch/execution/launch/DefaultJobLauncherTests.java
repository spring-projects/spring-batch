/**
 * 
 */
package org.springframework.batch.execution.launch;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobLocator;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * @author Lucas Ward
 *
 */
public class DefaultJobLauncherTests extends TestCase {

	private DefaultJobLauncher jobLauncher;
	
	private JobExecutor jobExecutor;
	private JobRepository jobRepository;
	private JobLocator jobLocator;
	
	private MockControl executorControl = MockControl.createControl(JobExecutor.class);
	private MockControl repositoryControl = MockControl.createControl(JobRepository.class);
	private MockControl locatorControl = MockControl.createControl(JobLocator.class);
	
	private JobIdentifier jobIdentifier = new SimpleJobIdentifier("job");
	private Job job = new Job();
	private JobExecution jobExecution = new JobExecution(null);
	JobExecutor blockingExecutor = new BlockingExecutor();
	
	protected void setUp() throws Exception {
		super.setUp();
		
		jobLauncher = new DefaultJobLauncher();
		
		jobExecutor = (JobExecutor)executorControl.getMock();
		jobRepository = (JobRepository)repositoryControl.getMock();
		jobLocator = (JobLocator)locatorControl.getMock();
		
		jobLauncher.setJobExecutor(jobExecutor);
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setJobLocator(jobLocator);
	}

	
	public void testRun() throws Exception{
		
		jobLocator.getJob("job");
		locatorControl.setDefaultReturnValue(job);
		jobRepository.findOrCreateJob(job, jobIdentifier);
		repositoryControl.setReturnValue(new JobExecution(null));
		jobExecutor.run(job, jobExecution);
		executorControl.setDefaultReturnValue(ExitStatus.FINISHED);
		
		locatorControl.replay();
		repositoryControl.replay();
		executorControl.replay();
		
		jobLauncher.run(jobIdentifier);
		
		locatorControl.verify();
		repositoryControl.verify();
		executorControl.verify();
	}
	
	public void testIsRunning() throws Exception{
		
		jobLauncher.setJobExecutor(blockingExecutor);
		
		jobLocator.getJob("job");
		locatorControl.setDefaultReturnValue(job);
		jobRepository.findOrCreateJob(job, jobIdentifier);
		repositoryControl.setReturnValue(jobExecution);
		
		locatorControl.replay();
		repositoryControl.replay();
		
		jobLauncher.run(jobIdentifier);
		
		assertTrue(jobLauncher.isRunning(jobIdentifier));
		
		Thread.sleep(250);
		assertFalse(jobLauncher.isRunning(jobIdentifier));
		assertEquals(ExitStatus.FINISHED, jobExecution.getExitStatus());
	}
	
	public void testAlreadyRunningJob() throws Exception{
		jobLauncher.setJobExecutor(blockingExecutor);
		
		jobLocator.getJob("job");
		locatorControl.setDefaultReturnValue(job);
		jobRepository.findOrCreateJob(job, jobIdentifier);
		repositoryControl.setReturnValue(jobExecution);
		
		locatorControl.replay();
		repositoryControl.replay();
		
		jobLauncher.run(jobIdentifier);
		assertTrue(jobLauncher.isRunning(jobIdentifier));
		
		try{
			jobLauncher.run(jobIdentifier);
			fail();
		}
		catch(JobExecutionAlreadyRunningException ex){
			//expected
		}
	}
	
	public void testStop() throws Exception{
		jobLauncher.setJobExecutor(blockingExecutor);
		RepeatContext stepContext = new RepeatContextSupport(null);
		jobExecution.registerStepContext(stepContext);
		
		jobLocator.getJob("job");
		locatorControl.setDefaultReturnValue(job);
		jobRepository.findOrCreateJob(job, jobIdentifier);
		repositoryControl.setReturnValue(jobExecution);
		
		locatorControl.replay();
		repositoryControl.replay();
		
		jobLauncher.run(jobIdentifier);
		
		assertTrue(jobLauncher.isRunning(jobIdentifier));
		
		jobLauncher.stop(jobIdentifier);
		
		Collection contexts = jobExecution.getStepContexts();
		for(Iterator it = contexts.iterator();it.hasNext();){
			RepeatContext context = (RepeatContext)it.next();
			assertEquals(stepContext, context);
			assertTrue(context.isTerminateOnly());
		}
	}
	
	private class BlockingExecutor implements JobExecutor{
		public ExitStatus run(Job job, JobExecution execution)
				throws BatchCriticalException {
			
			try{
				Thread.sleep(50);
			}
			catch(InterruptedException ex){
				throw new RuntimeException(ex);
			}
			
			return ExitStatus.FINISHED;
		}
	};
}
