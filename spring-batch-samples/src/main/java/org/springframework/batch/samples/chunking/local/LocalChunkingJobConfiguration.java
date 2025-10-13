/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.samples.chunking.local;

import javax.sql.DataSource;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.integration.chunk.ChunkTaskExecutorItemWriter;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableBatchProcessing
@Import(DataSourceConfiguration.class)
public class LocalChunkingJobConfiguration {

	public record Vet(String firstname, String lastname) {
	}

	@Bean
	public Job job(JobRepository jobRepository, Step chunkingStep) {
		return new JobBuilder("job", jobRepository).start(chunkingStep).build();
	}

	@Bean
	public Step chunkingStep(JobRepository jobRepository, JdbcTransactionManager transactionManager,
			FlatFileItemReader<Vet> itemReader, ChunkTaskExecutorItemWriter<Vet> itemWriter) {
		return new StepBuilder("chunkingStep", jobRepository).<Vet, Vet>chunk(2)
			.transactionManager(transactionManager)
			.reader(itemReader)
			.writer(itemWriter)
			.build();
	}

	@Bean
	@StepScope
	public FlatFileItemReader<Vet> itemReader(@Value("#{jobParameters['inputFile']}") Resource resource) {
		return new FlatFileItemReaderBuilder<Vet>().name("vetItemReader")
			.resource(resource)
			.delimited()
			.names("firstname", "lastname")
			.targetType(Vet.class)
			.build();
	}

	@Bean
	public ChunkTaskExecutorItemWriter<Vet> itemWriter(ChunkProcessor<Vet> chunkProcessor) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(4);
		taskExecutor.setThreadNamePrefix("worker-thread-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.afterPropertiesSet();
		return new ChunkTaskExecutorItemWriter<>(chunkProcessor, taskExecutor);
	}

	@Bean
	public ChunkProcessor<Vet> chunkProcessor(DataSource dataSource, TransactionTemplate transactionTemplate) {
		String sql = "insert into vets (firstname, lastname) values (?, ?)";
		JdbcBatchItemWriter<Vet> itemWriter = new JdbcBatchItemWriterBuilder<Vet>().dataSource(dataSource)
			.sql(sql)
			.itemPreparedStatementSetter((item, ps) -> {
				ps.setString(1, item.firstname());
				ps.setString(2, item.lastname());
			})
			.build();

		return (chunk, contribution) -> transactionTemplate.executeWithoutResult(transactionStatus -> {
			try {
				itemWriter.write(chunk);
				contribution.incrementWriteCount(chunk.size());
				contribution.setExitStatus(ExitStatus.COMPLETED);
			}
			catch (Exception e) {
				transactionStatus.setRollbackOnly();
				contribution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
		});
	}

}
