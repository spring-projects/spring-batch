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

import com.ibm.db2.jcc.DB2SimpleDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.utility.DockerImageName;

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
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class Db2JobRepositoryIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName DB2_IMAGE = DockerImageName.parse("ibmcom/db2:11.5.5.1");

	@ClassRule
	public static Db2Container db2 = new Db2Container(DB2_IMAGE).acceptLicense();
	
	@Autowired
	private DataSource dataSource;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private Job job;

	@Before
	public void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-db2.sql"));
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

		@Bean
		public DataSource dataSource() throws Exception {
			DB2SimpleDataSource dataSource =new DB2SimpleDataSource();
			dataSource.setDatabaseName(db2.getDatabaseName());
			dataSource.setUser(db2.getUsername());
			dataSource.setPassword(db2.getPassword());
			dataSource.setDriverType(4);
			dataSource.setServerName(db2.getContainerIpAddress());
			dataSource.setPortNumber(db2.getMappedPort(Db2Container.DB2_PORT));
			dataSource.setSslConnection(false);
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
