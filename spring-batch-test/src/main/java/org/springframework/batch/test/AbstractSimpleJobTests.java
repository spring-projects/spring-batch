package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for testing batch jobs using the SimpleJob implementation. 
 * It provides methods for launching a Job, or individual Steps within a Job on their own,
 * allowing for end to end testing of individual steps, without having to run every step
 * in the job.  Any test classes inheriting from this class should make sure they are part
 * of an ApplicationContext, which is generally expected to be done as part of the Spring
 * test framework.  Furthermore, the ApplicationContext in which it is a part of is expected
 * to have one {@link JobLauncher}, {@link JobRepository}, and a single Job implementation.
 * It should be noted that using any of the methods that don't conain {@link JobParameters} 
 * in their signature, will result in one being created with the current system time as a
 * parameter.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractSimpleJobTests {

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	private JobLauncher launcher;
	private JobRepository jobRepository;
	private SimpleJob job;
	private StepRunner stepRunner;

	private Map<String, Step> stepMap = new HashMap<String, Step>();
	private List<Step> stepList = new ArrayList<Step>();

	@Autowired
	public void setLauncher(JobLauncher bootstrap) {
		this.launcher = bootstrap;
	}
	
	@Autowired
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@Autowired
	public void setJob(SimpleJob job) {
		this.job = job;

		for (Step step : job.getSteps()) {
			stepMap.put(step.getName(), step);
			stepList.add(step);
		}
	}

	public StepRunner getStepRunner() {
		if(stepRunner == null){
			stepRunner = new StepRunner(launcher, jobRepository);
		}
		return stepRunner;
	}
	
	public SimpleJob getJob() {
		return job;
	}

	/**
	 * Public getter for the launcher.
	 * 
	 * @return the launcher
	 */
	protected JobLauncher getLauncher() {
		return launcher;
	}

	public Step getStep(String stepName){
		
		if(!stepMap.containsKey(stepName)){
			throw new IllegalStateException("No Step found with name: [" + stepName + "]");
		}
		return stepMap.get(stepName);
	}
	/**
	 * Launch the entire job, including all steps, in order.
	 * 
	 * @return JobExecution, so that the test may validate the exit status
	 */
	public JobExecution launchJob() {
		return this.launchJob(this.makeUniqueJobParameters());
	}

	/**
	 * Launch the entire job, including all steps, in order.
	 * 
	 * @param jobParameters
	 * @return JobExecution, so that the test may validate the exit status
	 */
	public JobExecution launchJob(JobParameters jobParameters) {
		try {
			return getLauncher().run(job, jobParameters);
		} catch (JobExecutionAlreadyRunningException e) {
			throw new RuntimeException(e);
		} catch (JobRestartException e) {
			throw new RuntimeException(e);
		} catch (JobInstanceAlreadyCompleteException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Launch just the specified step in the job.
	 * 
	 * @param stepName
	 */
	public JobExecution launchStep(String stepName) {
		return getStepRunner().launchStep(getStep(stepName));
	}

	/**
	 * Launch just the specified step in the job.
	 * 
	 * @param stepName
	 * @param jobParameters
	 */
	public JobExecution launchStep(String stepName, JobParameters jobParameters) {
		return getStepRunner().launchStep(getStep(stepName), jobParameters);
	}

	/**
	 * @return a new JobParameters object containing only a parameter for the
	 *         current timestamp, to ensure that the job instance will be unique
	 */
	private JobParameters makeUniqueJobParameters() {
		Map<String, JobParameter> parameters = new HashMap<String, JobParameter>();
		parameters.put("timestamp", new JobParameter(new Date().getTime()));
		return new JobParameters(parameters);
	}
}
