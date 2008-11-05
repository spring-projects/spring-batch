package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
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

/**
 * Utility class for executing steps outside of a {@link Job}.  This is useful
 * in end to end testing in order to allow for the testing of a step individually
 * without running every Step in a job.
 * 
 * <ul>
 * <li><b>launchStep(Step step)</b>: Launch the step with new parameters each time. (The current system
 * time will be used)
 * <li><b>launchStep(Step step, JobParameters jobParameters)</b>: Launch the specified step with the provided
 * JobParameters.  This may be useful if your step requires a certain parameter during runtime.
 * </ul>
 * 
 * It should be noted that any checked exceptions encountered while running the Step will wrapped with
 * RuntimeException.  Any checked exception thrown will be due to a framework error, not the logic of the
 * step, and thus requiring a throws declaration in clients of this class is unnecessary.
 * 
 * @author Dan Garrette
 * @author Lucas Ward
 * @since 2.0
 * @see SimpleJob
 */
public class StepRunner{

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	private JobLauncher launcher;
	private JobRepository jobRepository;

	public StepRunner(JobLauncher launcher, JobRepository jobRepository) {
		this.launcher = launcher;
		this.jobRepository = jobRepository;
	}
	
	/**
	 * Launcher 
	 * 
	 * @param stepName
	 */
	public JobExecution launchStep(Step step) {
		return this.launchStep(step, this.makeUniqueJobParameters());
	}

	/**
	 * Launch just the specified step in the job.
	 * 
	 * @param stepName
	 * @param jobParameters
	 */
	public JobExecution launchStep(Step step, JobParameters jobParameters) {
		//
		// Create a fake job
		//
		SimpleJob job = new SimpleJob();
		job.setName("TestJob");
		job.setJobRepository(this.jobRepository);

		List<Step> stepsToExecute = new ArrayList<Step>();
		stepsToExecute.add(step);
		job.setSteps(stepsToExecute);

		//
		// Launch the job
		//
		return this.launchJob(job, jobParameters);
	}

	/**
	 * Launch the given job
	 * 
	 * @param job
	 * @param jobParameters
	 */
	private JobExecution launchJob(Job job, JobParameters jobParameters) {
		try {
			return this.launcher.run(job, jobParameters);
		} catch (JobExecutionAlreadyRunningException e) {
			throw new RuntimeException(e);
		} catch (JobRestartException e) {
			throw new RuntimeException(e);
		} catch (JobInstanceAlreadyCompleteException e) {
			throw new RuntimeException(e);
		}
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
