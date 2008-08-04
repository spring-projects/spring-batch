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

package org.springframework.batch.core.launch.support;

import java.util.Properties;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * Interface to expose for remote management of jobs. Similar to
 * {@link JobLauncher}, but replaces {@link JobExecution} and
 * {@link JobParameters} with Strings in return types and method parameters, so
 * it can be inspected by remote clients like the jconsole from the JRE without
 * any links to Spring Batch.
 * 
 * @author Dave Syer
 * 
 */
public interface ExportedJobLauncher {

	/**
	 * Launch a job with the given name.
	 * 
	 * @param name the name of the job to launch
	 * @return a representation of the {@link JobExecution} returned by a
	 * {@link JobLauncher}.
	 */
	String run(String name);

	/**
	 * Launch a job with the given name and parameters.
	 * 
	 * @param name the name of the job to launch
	 * @return a representation of the {@link JobExecution} returned by a
	 * {@link JobLauncher}.
	 */
	String run(String name, String params);

	/**
	 * Stop all running jobs.
	 */
	void stop();

	/**
	 * Stop running jobs with the supplied name.
	 */
	void stop(String name);

	/**
	 * Clear volatile memory of any jobs that are not running (and are therefore
	 * completed or failed).
	 */
	void clear();

	/**
	 * Enquire if any jobs launched here are still running.
	 * 
	 * @return true if any jobs are running.
	 */
	boolean isRunning();

	/**
	 * Query statistics of jobs.
	 * 
	 * @return properties representing last known state of jobs with this name
	 * (including those that may have finished)
	 */
	public Properties getStatistics(String name);
}
