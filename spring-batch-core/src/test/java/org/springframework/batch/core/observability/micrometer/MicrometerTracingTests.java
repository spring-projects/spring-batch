/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.observability.micrometer;

import java.util.UUID;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class MicrometerTracingTests extends SampleTestRunner {

	@Autowired
	private Job job;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private ObservationRegistry observationRegistry;

	MicrometerTracingTests() {
		super(SampleRunnerConfig.builder().build());
	}

	@Override
	protected MeterRegistry createMeterRegistry() {
		return this.meterRegistry;
	}

	@Override
	protected ObservationRegistry createObservationRegistry() {
		return this.observationRegistry;
	}

	@AfterEach
	@Override
	protected void closeMeterRegistry() {
		this.meterRegistry.clear();
	}

	@Override
	public SampleTestRunnerConsumer yourCode() {
		return (bb, meterRegistry) -> {
			// given
			JobParameters jobParameters = new JobParametersBuilder().addString("uuid", UUID.randomUUID().toString())
				.toJobParameters();

			// when
			JobExecution jobExecution = this.jobOperator.start(this.job, jobParameters);

			// then
			Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

			// and
			SpansAssert.assertThat(bb.getFinishedSpans())
				.haveSameTraceId()
				.hasASpanWithName(BatchMetrics.METRICS_PREFIX + "job")
				.hasASpanWithName(BatchMetrics.METRICS_PREFIX + "step");

			// and
			MeterRegistryAssert.assertThat(meterRegistry)
				.hasMeterWithName(BatchMetrics.METRICS_PREFIX + "job")
				.hasMeterWithName(BatchMetrics.METRICS_PREFIX + "step");
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class TestConfig {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		public ObservationRegistry observationRegistry(MeterRegistry meterRegistry) {
			ObservationRegistry observationRegistry = ObservationRegistry.create();
			observationRegistry.observationConfig()
				.observationHandler(new DefaultMeterObservationHandler(meterRegistry));
			return observationRegistry;
		}

		@Bean
		public Step step(JobRepository jobRepository) {
			return new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
