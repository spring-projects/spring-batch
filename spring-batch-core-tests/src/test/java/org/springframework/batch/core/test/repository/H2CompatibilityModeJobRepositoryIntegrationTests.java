/*
 * Copyright 2022 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * @author Henning Pöttker
 * @author Mahmoud Ben Hassine
 */
@RunWith(Parameterized.class)
public class H2CompatibilityModeJobRepositoryIntegrationTests {

	private final String compatibilityMode;

	public H2CompatibilityModeJobRepositoryIntegrationTests(String compatibilityMode) {
		this.compatibilityMode = compatibilityMode;
	}

	@Test
	public void testJobExecution() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfiguration.class);
		context.registerBean(DataSource.class, this::buildDataSource);
		context.refresh();
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		Assert.assertNotNull(jobExecution);
		Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
		jdbcTemplate.execute("SHUTDOWN");
	}

	private DataSource buildDataSource() {
		String connectionUrl = String.format(
				"jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=%s",
				UUID.randomUUID(),
				this.compatibilityMode
		);
		DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		Resource resource = new DefaultResourceLoader()
				.getResource("/org/springframework/batch/core/schema-h2.sql");
		populator.addScript(resource);
		DatabasePopulatorUtils.execute(populator, dataSource);
		return dataSource;
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {
		@Bean
		Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get("job")
					.start(steps.get("step")
							.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
							.build())
					.build();
		}
	}

	@Parameters
	public static List<Object[]> data() throws Exception {
		return Arrays.stream(org.h2.engine.Mode.ModeEnum.values())
				.map(mode -> new Object[]{mode.toString()})
				.collect(Collectors.toList());
	}
}
