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

	List<Long> getExecutions(Long instanceId) throws NoSuchJobException;

	List<Long> getLastInstances(String jobName, int count) throws NoSuchJobException;

	Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException;

	String getParameters(Long executionId) throws NoSuchJobExecutionException;

	Long start(String jobName, String parameters) throws NoSuchJobException, JobInstanceAlreadyExistsException,
			JobRestartException;

	Long resume(Long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
			NoSuchJobException;

	Long startNextInstance(String jobName) throws NoSuchJobException, JobParametersIncrementerNotFoundException;

	boolean stop(Long executionId) throws NoSuchJobExecutionException;

	String getSummary(Long executionId) throws NoSuchJobExecutionException;

	Map<Long, String> getStepExecutionSummaries(Long executionId) throws NoSuchJobExecutionException;

	Set<String> getJobNames();

}
