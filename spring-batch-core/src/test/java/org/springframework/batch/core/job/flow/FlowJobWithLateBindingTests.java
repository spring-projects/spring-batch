/*
 * Copyright 2006-present the original author or authors.
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
package org.springframework.batch.core.job.flow;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yanming Zhou
 * @author Tino Desjardins
 */
@SpringJUnitConfig
class FlowJobWithLateBindingTests {

	// GH-5680
	@Test
	void executionContextShouldBeVisibleForLaterScopedStepCreation(@Autowired JobOperator jobOperator,
			@Autowired Job job) throws Exception {
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
				() -> "Failures: " + jobExecution.getAllFailureExceptions());
	}

	@Configuration
	@EnableBatchProcessing
	static class JobConfiguration {

		@Bean
		Job job(JobRepository jobRepository, Step producerStep, Step consumerStep) {
			return new JobBuilder("job", jobRepository).flow(producerStep).next(consumerStep).end().build();
		}

		@Bean
		Step producerStep(JobRepository jobRepository) {
			return new StepBuilder(jobRepository).tasklet((contribution, chunkContext) -> {
				contribution.getStepExecution().getJobExecution().getExecutionContext().put("value", "hello");
				return RepeatStatus.FINISHED;
			}).build();
		}

		@Bean
		@JobScope
		Step consumerStep(@Value("#{jobExecutionContext['value']}") String value, JobRepository jobRepository) {
			Assert.state("hello".equals(value), "value must be set by producerStep");
			return new StepBuilder(jobRepository).tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
				.build();
		}

	}

}