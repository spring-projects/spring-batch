/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.batch.samples.jdbc.paging;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditRowMapper;
import org.springframework.batch.samples.jdbc.JdbcReaderBatchWriterSampleJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
public class JdbcPagingReaderBatchWriterSampleJob extends JdbcReaderBatchWriterSampleJob {

	@Bean
	@StepScope
	public JdbcPagingItemReader<CustomerCredit> itemReader(DataSource dataSource,
			@Value("#{jobParameters['credit']}") Double credit) throws Exception {
		Map<String, Object> parameterValues = new HashMap<>();
		parameterValues.put("statusCode", "PE");
		parameterValues.put("credit", credit);
		parameterValues.put("type", "COLLECTION");

		return new JdbcPagingItemReaderBuilder<CustomerCredit>().name("customerReader")
			.dataSource(dataSource)
			.selectClause("select NAME, ID, CREDIT")
			.fromClause("FROM CUSTOMER")
			.whereClause("WHERE CREDIT > :credit")
			.sortKeys(Map.of("ID", Order.ASCENDING))
			.rowMapper(new CustomerCreditRowMapper())
			.pageSize(2)
			.parameterValues(parameterValues)
			.build();
	}

}
