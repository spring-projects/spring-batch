package org.springframework.batch.execution.bootstrap.support;

import org.springframework.batch.execution.bootstrap.JobLauncher;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Interface to expose for remote management of jobs. Similar to
 * {@link JobLauncher}, but replaces {@link ExitStatus} with String in return
 * types, so it can be inspected by remote clients like the jconsole from the
 * JRE without any links to Spring Batch.
 * 
 * @author Dave Syer
 * 
 */
public interface ExportedJobLauncher {

	/**
	 * Launch a job and get back a representation of the {@link ExitStatus}
	 * returned by a {@link JobLauncher}. Normally the launch will be
	 * asynchronous, so the possible values of the return type are constrained
	 * (it will never be {@link ExitStatus#CONTINUABLE}).
	 * 
	 * @return a representation of the {@link ExitStatus} returned by a
	 *         {@link JobLauncher}.
	 */
	String run();

	/**
	 * Launch a job configuration with the given name.
	 * 
	 * @param name the name of the job to launch
	 * @return a representation of the {@link ExitStatus} returned by a
	 *         {@link JobLauncher}.
	 *         
	 * @see #run()
	 */
	String run(String name);

	/**
	 * Stop all running jobs.
	 * 
	 * @see JobLauncher#stop()
	 */
	void stop();

	/**
	 * Enquire if any jobs are still running.
	 * 
	 * @return true if any jobs are running.
	 * 
	 * @see JobLauncher#isRunning()
	 */
	boolean isRunning();

}
