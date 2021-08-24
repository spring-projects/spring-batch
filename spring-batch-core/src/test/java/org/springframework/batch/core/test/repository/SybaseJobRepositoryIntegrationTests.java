/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.batch.core.test.repository;

import javax.sql.DataSource;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * The Sybase official jdbc driver is not freely available. This test uses the non-official jTDS driver.
 * There is no official public Docker image for Sybase neither. This test uses the non-official Docker image by Jetbrains.
 * Sybase in not supported in testcontainers. Sysbase support is tested manually for the moment:
 *  1. Run `docker run -d -t -p 5000:5000 -eSYBASE_USER=sa -eSYBASE_PASSWORD=sa -eSYBASE_DB=test datagrip/sybase:16.0`
 *  2. Update the datasource configuration with the IP of the container
 *  3. Run the test `testJobExecution`
 *
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@Ignore("No support for Sybase in testcontainers")
public class SybaseJobRepositoryIntegrationTests {

	@Autowired
	private DataSource dataSource;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private Job job;
		
	@Before
	public void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-sybase.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	public void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
		
		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);

		// then
		Assert.assertNotNull(jobExecution);
		Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		// FIXME Configuration parameters are hard-coded for the moment, to update once testcontainers support is available
		@Bean
		public DataSource dataSource() throws Exception {
			JtdsDataSource dataSource = new JtdsDataSource();
			dataSource.setUser("sa");
			dataSource.setPassword("sa");
			dataSource.setServerName("172.17.0.2");
			dataSource.setPortNumber(5000);
			dataSource.setDatabaseName("test");
			return dataSource;
		}

		@Bean
		public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get("job")
					.start(steps.get("step")
							.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
							.build())
					.build();
		}

	}
}
