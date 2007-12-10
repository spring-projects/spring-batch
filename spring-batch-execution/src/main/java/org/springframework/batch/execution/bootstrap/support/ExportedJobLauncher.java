/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.execution.bootstrap.support;

import java.util.Properties;

import org.springframework.batch.execution.launch.JobLauncher;
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
	
	/**
	 * Query statistics of currently executing jobs.
	 * 
	 * @return properties representing last known state of currently executing jobs
	 */
	public Properties getStatistics();

}
