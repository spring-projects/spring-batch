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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.SpringBatchTestJUnit5Tests;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static io.micrometer.tracing.test.simple.SpansAssert.assertThat;

@SpringBatchTest
class ObservabilitySampleStepTests extends SampleTestRunner {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	ObservabilitySampleStepTests() {
		super(SampleRunnerConfig.builder().build());
	}

	@Override
	protected MeterRegistry createMeterRegistry() {
		return Metrics.globalRegistry;
	}

	@Override
	protected ObservationRegistry createObservationRegistry() {
		return BatchMetrics.observationRegistry;
	}

	@BeforeEach
	void setup(@Autowired Job job) {
		this.jobLauncherTestUtils.setJob(job);
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
			MeterRegistryAssert.assertThat(meterRegistry).hasTimerWithName("spring.batch.job")
					.hasTimerWithName("spring.batch.step");
		};
	}

	@Configuration(proxyBeanMethods = false)
	@Import(SpringBatchTestJUnit5Tests.JobConfiguration.class)
	static class TestConfig {

	}

}
