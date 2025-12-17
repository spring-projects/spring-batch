/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.chunking.local;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalChunkingJobFunctionalTests {

	@Test
	public void testLaunchJobWithJavaConfig() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(LocalChunkingJobConfiguration.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFile", "org/springframework/batch/samples/chunking/local/data/vets.csv")
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		int vetsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "vets");
		assertEquals(6, vetsCount);
	}

	@Test
	public void testLaunchJobWithJavaConfigFailure() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(LocalChunkingJobConfiguration.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("inputFile", "org/springframework/batch/samples/chunking/local/data/vets-bad-data.csv")
			.toJobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertTrue(jobExecution.getExitStatus().getExitDescription().contains("size limit: 30"));
		int vetsCount = JdbcTestUtils.countRowsInTable(jdbcTemplate, "vets");
		assertEquals(4, vetsCount);
	}

}
