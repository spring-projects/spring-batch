/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml", "/jobs/sample-steps.xml" })
class SampleStepTests implements ApplicationContextAware {

	@Autowired
	private DataSource dataSource;

	private JdbcClient jdbcClient;

	private JobOperatorTestUtils jobOperatorTestUtils;

	private ApplicationContext context;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private JobRepository jobRepository;

	@BeforeEach
	void setUp() {
		jdbcClient = JdbcClient.create(dataSource);
		jdbcClient.sql("create table TESTS (ID integer, NAME varchar(40))").update();
		jobOperatorTestUtils = new JobOperatorTestUtils(jobOperator, jobRepository);
	}

	@AfterEach
	void tearDown() {
		JdbcTestUtils.dropTables(this.jdbcClient, "TESTS");
	}

	@Test
	void testTasklet() {
		Step step = context.getBean("s2", Step.class);
		assertEquals(BatchStatus.COMPLETED, jobOperatorTestUtils.startStep(step).getStatus());
		assertEquals(2,
				jdbcClient.sql("SELECT ID from TESTS where NAME = 'SampleTasklet2'").query(Integer.class).single());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

}
