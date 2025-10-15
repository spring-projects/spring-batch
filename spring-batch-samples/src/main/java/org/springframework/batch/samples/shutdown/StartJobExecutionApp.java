/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.samples.shutdown;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobExecutionShutdownHook;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Application to start a job execution.
 *
 * @author Mahmoud Ben Hassine
 */
public class StartJobExecutionApp {

	public static void main(String[] args) throws Exception {
		System.out.println("Process id = " + ProcessHandle.current().pid());
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder().addLong("minId", 1L)
			.addLong("maxId", 10L)
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);
		Thread springBatchHook = new JobExecutionShutdownHook(jobExecution, jobOperator);
		Runtime.getRuntime().addShutdownHook(springBatchHook);
	}

}
