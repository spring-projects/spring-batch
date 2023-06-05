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
package org.springframework.batch.test.observability;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static io.micrometer.tracing.test.simple.SpansAssert.assertThat;

@SpringBatchTest
class ObservabilitySampleStepTests extends SampleTestRunner {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private ObservationRegistry observationRegistry;

	ObservabilitySampleStepTests() {
		super(SampleRunnerConfig.builder().build());
	}

	@Override
	protected MeterRegistry createMeterRegistry() {
		return Metrics.globalRegistry;
	}

	@Override
	protected ObservationRegistry createObservationRegistry() {
		return this.observationRegistry;
	}

	@AfterEach
	@Override
	protected void closeMeterRegistry() {
		Metrics.globalRegistry.clear();
	}

	@Override
	public SampleTestRunnerConsumer yourCode() {
		return (bb, meterRegistry) -> {
			// given
			JobParameters jobParameters = this.jobLauncherTestUtils.getUniqueJobParameters();

			// when
			JobExecution jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);

			// then
			Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

			// and
			assertThat(bb.getFinishedSpans()).haveSameTraceId().hasASpanWithName("job").hasASpanWithName("step");

			// and
			MeterRegistryAssert.assertThat(meterRegistry)
				.hasTimerWithName("spring.batch.job")
				.hasTimerWithName("spring.batch.step");
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class TestConfig {

		@Bean
		public ObservationRegistry observationRegistry() {
			ObservationRegistry observationRegistry = ObservationRegistry.create();
			observationRegistry.observationConfig()
				.observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));
			return observationRegistry;
		}

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			return new StepBuilder("step", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
