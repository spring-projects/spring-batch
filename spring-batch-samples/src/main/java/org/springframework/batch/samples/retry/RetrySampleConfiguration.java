/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.samples.retry;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.domain.trade.Trade;
import org.springframework.batch.samples.domain.trade.internal.GeneratingTradeItemReader;
import org.springframework.batch.samples.support.RetrySampleItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class RetrySampleConfiguration {

	@Bean
	public Job retrySample(JobRepository jobRepository, Step step) {
		return new JobBuilder("retrySample", jobRepository).start(step).build();
	}

	@Bean
	protected Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
		return new StepBuilder("step", jobRepository).<Trade, Object>chunk(1)
			.transactionManager(transactionManager)
			.reader(reader())
			.writer(writer())
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(3)
			.build();
	}

	@Bean
	protected GeneratingTradeItemReader reader() {
		GeneratingTradeItemReader reader = new GeneratingTradeItemReader();
		reader.setLimit(10);
		return reader;
	}

	@Bean
	protected RetrySampleItemWriter<Object> writer() {
		return new RetrySampleItemWriter<>();
	}

}
