/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.test.context;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Henning Pöttker
 * @author Mahmoud Ben Hassine
 */
class BatchTestContextBeanPostProcessorTests {

	private GenericApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		this.applicationContext = new AnnotationConfigApplicationContext(BatchConfiguration.class);
		this.applicationContext.registerBean(JobLauncherTestUtils.class);
	}

	@AfterEach
	void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	void testContextWithoutJobBean() {
		var jobLauncherTestUtils = this.applicationContext.getBean(JobLauncherTestUtils.class);
		assertNotNull(jobLauncherTestUtils);
		assertNull(jobLauncherTestUtils.getJob());
	}

	@Test
	void testContextWithUniqueJobBean() {
		applicationContext.registerBean(StubJob.class);
		var jobLauncherTestUtils = this.applicationContext.getBean(JobLauncherTestUtils.class);
		assertNotNull(jobLauncherTestUtils.getJob());
	}

	@Test
	void testContextWithTwoJobBeans() {
		this.applicationContext.registerBean("jobA", StubJob.class);
		this.applicationContext.registerBean("jobB", StubJob.class);
		var jobLauncherTestUtils = applicationContext.getBean(JobLauncherTestUtils.class);
		assertNotNull(jobLauncherTestUtils);
		assertNull(jobLauncherTestUtils.getJob());
	}

	static class StubJob implements Job {

		@Override
		public String getName() {
			return "name";
		}

		@Override
		public void execute(JobExecution execution) {
		}

	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		BatchTestContextBeanPostProcessor beanPostProcessor() {
			return new BatchTestContextBeanPostProcessor();
		}

	}

}
