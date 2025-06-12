/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.batch.core.observability;

import java.util.UUID;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ObservabilitySampleStepTests extends SampleTestRunner {

	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

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
			JobParameters jobParameters = new JobParametersBuilder().addString("uuid", UUID.randomUUID().toString())
				.toJobParameters();

			// when
			JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);

			// then
			Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

			// and
			SpansAssert.assertThat(bb.getFinishedSpans())
				.haveSameTraceId()
				.hasASpanWithName("job")
				.hasASpanWithName("step");

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
