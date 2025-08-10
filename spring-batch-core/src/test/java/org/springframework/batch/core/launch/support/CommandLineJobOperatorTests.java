/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.launch.support;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CommandLineJobOperator}.
 *
 * @author Mahmoud Ben Hassine
 * @author Yejeong Ham
 */
class CommandLineJobOperatorTests {

	private final JobOperator jobOperator = mock();

	private final JobRepository jobRepository = mock();

	private final JobRegistry jobRegistry = mock();

	private final JobParametersConverter jobParametersConverter = mock();

	private final ExitCodeMapper exitCodeMapper = mock();

	private CommandLineJobOperator commandLineJobOperator;

	@BeforeEach
	void setUp() {
		commandLineJobOperator = new CommandLineJobOperator(jobOperator, jobRepository, jobRegistry);
		commandLineJobOperator.setJobParametersConverter(jobParametersConverter);
		commandLineJobOperator.setExitCodeMapper(exitCodeMapper);
	}

	@Test
	void start() throws Exception {
		// given
		String jobName = "job";
		Properties parameters = new Properties();
		Job job = mock();
		JobParameters jobParameters = mock();

		// when
		Mockito.when(jobRegistry.getJob(jobName)).thenReturn(job);
		Mockito.when(jobParametersConverter.getJobParameters(parameters)).thenReturn(jobParameters);
		this.commandLineJobOperator.start(jobName, parameters);

		// then
		Mockito.verify(jobRegistry).getJob(jobName);
		Mockito.verify(jobParametersConverter).getJobParameters(parameters);
		Mockito.verify(jobOperator).start(job, jobParameters);
	}

	@Test
	void startNextInstance() throws Exception {
		// given
		String jobName = "job";
		Job job = mock();

		// when
		Mockito.when(jobRegistry.getJob(jobName)).thenReturn(job);
		this.commandLineJobOperator.startNextInstance(jobName);

		// then
		Mockito.verify(jobRegistry).getJob(jobName);
		Mockito.verify(jobOperator).startNextInstance(job);
	}

	@Test
	void stop() throws Exception {
		// given
		long jobExecutionId = 1;
		JobExecution jobExecution = mock();

		// when
		Mockito.when(jobRepository.getJobExecution(jobExecutionId)).thenReturn(jobExecution);
		this.commandLineJobOperator.stop(jobExecutionId);

		// then
		Mockito.verify(jobOperator).stop(jobExecution);
	}

	@Test
	void restart() throws Exception {
		// given
		long jobExecutionId = 1;
		JobExecution jobExecution = mock();

		// when
		Mockito.when(jobRepository.getJobExecution(jobExecutionId)).thenReturn(jobExecution);
		this.commandLineJobOperator.restart(jobExecutionId);

		// then
		Mockito.verify(jobOperator).restart(jobExecution);
	}

	@Test
	void abandon() throws Exception {
		// given
		long jobExecutionId = 1;
		JobExecution jobExecution = mock();

		// when
		Mockito.when(jobRepository.getJobExecution(jobExecutionId)).thenReturn(jobExecution);
		this.commandLineJobOperator.abandon(jobExecutionId);

		// then
		Mockito.verify(jobOperator).abandon(jobExecution);
	}

	@Test
	void recover() {
		// given
		long jobExecutionId = 1;
		JobExecution jobExecution = mock();

		// when
		Mockito.when(jobRepository.getJobExecution(jobExecutionId)).thenReturn(jobExecution);
		this.commandLineJobOperator.recover(jobExecutionId);

		// then
		Mockito.verify(jobOperator).recover(jobExecution);
	}

}