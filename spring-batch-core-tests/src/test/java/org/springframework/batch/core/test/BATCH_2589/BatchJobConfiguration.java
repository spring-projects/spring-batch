/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.BATCH_2589;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

/**
 * @author Jannik Hell
 */
@Configuration
@EnableBatchProcessing
public class BatchJobConfiguration extends DefaultBatchConfigurer {
	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Job fooJob() throws Exception {
		return jobBuilderFactory.get("fooJob")
				.incrementer(new RunIdIncrementer())
				.flow(fooStep())
				.end()
				.build();
	}

	@Bean
	public Step fooStep() throws Exception {
		PagingAndSortingRepository<Foo, Integer> repository = new FooRepository();

		Map<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("id", Sort.Direction.ASC);

		RepositoryItemReader<Foo> fooRepositoryReader = new RepositoryItemReader<>();
		fooRepositoryReader.setRepository(repository);
		fooRepositoryReader.setPageSize(10);
		fooRepositoryReader.setSort(sorts);
		fooRepositoryReader.setMethodName("findAll");
		fooRepositoryReader.afterPropertiesSet();

		ItemWriter<Foo> logWriter = new ItemWriter<Foo>() {
			@Override
			public void write(List<? extends Foo> items) throws Exception {
				logger.info(items);
			}
		};

		return stepBuilderFactory.get("fooStep")
				.<Foo, Foo>chunk(10)
				.reader(fooRepositoryReader)
				.writer(logWriter)
				.faultTolerant()
				.retry(Exception.class)
				.retryLimit(3)
				.skip(Exception.class)
				.skipLimit(3)
				.backOffPolicy(new ExponentialBackOffPolicy())
				.build();
	}
}
