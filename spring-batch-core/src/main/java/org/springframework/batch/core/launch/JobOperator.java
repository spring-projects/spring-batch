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

import java.util.Collection;
import java.util.Map;

import org.springframework.batch.core.repository.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.core.repository.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.NoSuchJobInstanceException;

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

	String getParameters(Long instanceId) throws NoSuchJobInstanceException;

	Long getLastInstance(String jobName) throws NoSuchJobException;

	Long start(String jobName, String parameters) throws NoSuchJobException, JobInstanceAlreadyExistsException,
			JobRestartException;

	Long resume(Long instanceId) throws LastExecutionNotFailedException, NoSuchJobInstanceException;

	Long startNextInstance(String jobName) throws NoSuchJobException, JobParametersIncrementerNotFoundException;

	boolean stop(Long executionId) throws NoSuchJobExecutionException;

	Map<Long, String> status(Long executionId) throws NoSuchJobExecutionException;

	Collection<Long> getRunningExecutions(String jobName) throws NoSuchJobException;

	Collection<String> getJobNames();

}
