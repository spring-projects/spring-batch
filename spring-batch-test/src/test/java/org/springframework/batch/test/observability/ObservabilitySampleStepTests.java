/*
 * Copyright 2008-2021 the original author or authors.
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

import java.util.function.BiConsumer;

import io.micrometer.api.instrument.MeterRegistry;
import io.micrometer.api.instrument.Metrics;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.SampleTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.SpringBatchTestJUnit5Tests;
import org.springframework.batch.test.StepRunner;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.Assert.assertEquals;

@SpringBatchTest
public class ObservabilitySampleStepTests extends SampleTestRunner {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Override
	protected MeterRegistry getMeterRegistry() {
		return Metrics.globalRegistry;
	}

	@AfterEach
	void clean() {
		Metrics.globalRegistry.clear();
	}

	@Override
	public BiConsumer<Tracer, MeterRegistry> yourCode() throws Exception {
		return (tracer, meterRegistry) -> {
			// given
			JobParameters jobParameters = this.jobLauncherTestUtils.getUniqueJobParameters();

			// when
			JobExecution jobExecution = null;
			try {
				jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}

			// then
			Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		};
	}

	@Configuration(proxyBeanMethods = false)
	@Import(SpringBatchTestJUnit5Tests.JobConfiguration.class)
	static class TestConfig {

	}
}
