package org.springframework.batch.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for testing batch jobs using the SimpleJob implementation. It
 * provides methods for launching a Job, or individual Steps within a Job on
 * their own, allowing for end to end testing of individual steps, without
 * having to run every step in the job. Any test classes inheriting from this
 * class should make sure they are part of an ApplicationContext, which is
 * generally expected to be done as part of the Spring test framework.
 * Furthermore, the ApplicationContext in which it is a part of is expected to
 * have one {@link JobLauncher}, {@link JobRepository}, and a single Job
 * implementation. It should be noted that using any of the methods that don't
 * conain {@link JobParameters} in their signature, will result in one being
 * created with the current system time as a parameter.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractFlowJobTests {

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	private JobLauncher launcher;
	private FlowJob job;

	@Autowired
	public void setLauncher(JobLauncher bootstrap) {
		this.launcher = bootstrap;
	}

	@Autowired
	public void setJob(FlowJob job) {
		this.job = job;
	}

	public FlowJob getJob() {
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
			return getLauncher().run(this.job, jobParameters);
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
