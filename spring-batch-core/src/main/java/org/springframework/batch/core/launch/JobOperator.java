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
package org.springframework.batch.core.launch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * A really low level interface for inspecting and controlling jobs with access
 * only to primitive and collection types. Suitable for a command-line client
 * (e.g. that launches a new process for each operation), or a remote launcher
 * like a JMX console.
 * 
 * @author Dave Syer
 * 
 */
public interface JobOperator {

	List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException;

	List<Long> getLastInstances(String jobName, int count) throws NoSuchJobException;

	Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException;

	String getParameters(long executionId) throws NoSuchJobExecutionException;

	Long start(String jobName, String parameters) throws NoSuchJobException, JobInstanceAlreadyExistsException,
			JobRestartException;

	Long resume(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
			NoSuchJobException, JobRestartException;

	/**
	 * Launch the next in a sequence of {@link JobInstance} determined by the
	 * {@link JobParametersIncrementer} attached to the specified job. If the
	 * previous instance is still in a failed state, this method should still
	 * create a new instance and run it with different parameters (as long as
	 * the {@link JobParametersIncrementer} is working).<br/><br/>
	 * 
	 * The last three exception described below should be extremely unlikely,
	 * but cannot be ruled out entirely. It points to some other thread or
	 * process trying to use this method (or a similar one) at the same time.
	 * 
	 * @param jobName the name of the job to launch
	 * @return the {@link JobExecution} id of the execution created when the job
	 * is launched
	 * @throws NoSuchJobException if there is no such job definition available
	 * @throws JobParametersNotFoundException if the parameters cannot be found
	 * @throws UnexpectedJobExecutionException if an unexpected condition arises
	 */
	Long startNextInstance(String jobName) throws NoSuchJobException, JobParametersNotFoundException,
			JobRestartException, JobExecutionAlreadyRunningException, JobInstanceAlreadyCompleteException;

	boolean stop(long executionId) throws NoSuchJobExecutionException;

	String getSummary(long executionId) throws NoSuchJobExecutionException;

	Map<Long, String> getStepExecutionSummaries(long executionId) throws NoSuchJobExecutionException;

	Set<String> getJobNames();

}
