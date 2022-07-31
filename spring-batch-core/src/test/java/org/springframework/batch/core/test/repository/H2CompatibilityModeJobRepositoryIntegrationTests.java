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

import java.util.UUID;

import javax.sql.DataSource;

import org.h2.engine.Mode.ModeEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Henning Pöttker
 */
class H2CompatibilityModeJobRepositoryIntegrationTests {

	@EnumSource(ModeEnum.class)
	@ParameterizedTest
	void testJobExecution(ModeEnum compatibilityMode) throws Exception {
		var context = new AnnotationConfigApplicationContext();
		context.register(TestConfiguration.class);
		context.registerBean(DataSource.class, () -> buildDataSource(compatibilityMode));
		context.refresh();
		var jobLauncher = context.getBean(JobLauncher.class);
		var job = context.getBean(Job.class);

		var jobExecution = jobLauncher.run(job, new JobParameters());

		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

		var jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
		jdbcTemplate.execute("SHUTDOWN");
	}

	private static DataSource buildDataSource(ModeEnum compatibilityMode) {
		var connectionUrl = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=%s",
				UUID.randomUUID(), compatibilityMode);
		var dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		var populator = new ResourceDatabasePopulator();
		var resource = new DefaultResourceLoader().getResource("/org/springframework/batch/core/schema-h2.sql");
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
					.start(steps.get("step").tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED).build())
					.build();
		}

	}

}
