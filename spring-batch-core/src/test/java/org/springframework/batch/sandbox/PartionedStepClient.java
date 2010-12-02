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
package org.springframework.batch.sandbox;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.UUID;

/**
 * @author Josh Long
 * @see {@link org.springframework.batch.core.partition.support.PartitionStep}
 */
public class PartionedStepClient {
	public static void main (String [] args ) throws Throwable {


		ClassPathXmlApplicationContext cax = new ClassPathXmlApplicationContext ("org/springframework/batch/sandbox/ps1.xml");

		JobLauncher jobLauncher = (JobLauncher) cax.getBean("jobLauncher");
		Job job = (Job) cax.getBean("partitionedJob") ;


		JobParameters parms = new JobParametersBuilder().addString("uid", UUID.randomUUID().toString()).toJobParameters();
		JobExecution execution = jobLauncher.run( job, parms);

	}
}
