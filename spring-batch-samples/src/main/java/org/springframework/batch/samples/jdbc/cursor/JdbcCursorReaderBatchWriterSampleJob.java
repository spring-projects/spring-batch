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
package org.springframework.batch.samples.jdbc.cursor;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditRowMapper;
import org.springframework.batch.samples.jdbc.JdbcReaderBatchWriterSampleJob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
public class JdbcCursorReaderBatchWriterSampleJob extends JdbcReaderBatchWriterSampleJob {

	@Bean
	public JdbcCursorItemReader<CustomerCredit> itemReader(DataSource dataSource) {
		String sql = "select ID, NAME, CREDIT from CUSTOMER";
		return new JdbcCursorItemReaderBuilder<CustomerCredit>().name("customerReader")
			.dataSource(dataSource)
			.sql(sql)
			.rowMapper(new CustomerCreditRowMapper())
			.build();
	}

}
