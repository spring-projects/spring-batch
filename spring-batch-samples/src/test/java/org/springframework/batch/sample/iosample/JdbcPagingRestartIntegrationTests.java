/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.sample.iosample;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

/**
 * @author Dave Syer
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class })
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/ioSampleJob.xml",
		"/jobs/iosample/jdbcPaging.xml" })
public class JdbcPagingRestartIntegrationTests {

	@Autowired
	private ItemReader<CustomerCredit> reader;

	private SimpleJdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public StepExecution getStepExecution() {
		return MetaDataInstanceFactory.createStepExecution(new JobParametersBuilder().addDouble("credit", 10000.)
				.toJobParameters());
	}

	@Test
	public void testReader() throws Exception {

		int total = SimpleJdbcTestUtils.countRowsInTable(jdbcTemplate, "CUSTOMER");
		int pageSize = 2; // same as configured in reader
		int count = (total / pageSize) * pageSize;
		if (count >= pageSize) {
			count -= pageSize;
		}

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt("JdbcPagingItemReader.read.count", count);
		// Assume the primary keys are in order

		List<Map<String, Object>> ids = jdbcTemplate
				.queryForList("SELECT ID, CREDIT FROM CUSTOMER WHERE CREDIT > 10000 ORDER BY ID ASC");
		// System.err.println(ids);
		int startAfterValue = ((Long) ids.get(count - 1).get("ID")).intValue();
		// System.err.println("Start after: " + startAfterValue);
		executionContext.putInt("JdbcPagingItemReader.start.after", startAfterValue);
		((ItemStream) reader).open(executionContext);

		for (int i = count; i < total; i++) {
			CustomerCredit item = reader.read();
			// System.err.println("Item: " + item);
			assertNotNull(item);
		}

		CustomerCredit item = reader.read();
		// System.err.println("Item: " + item);
		assertNull(item);

	}

}
