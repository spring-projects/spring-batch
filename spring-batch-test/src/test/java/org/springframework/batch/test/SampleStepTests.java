/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/sample-steps.xml" })
public class SampleStepTests implements ApplicationContextAware {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private StepRunner stepRunner;

	private ApplicationContext context;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobRepository jobRepository;

	@Before
	public void setUp() {
		jdbcTemplate.update("create table TESTS (ID integer, NAME varchar(40))");
		stepRunner = new StepRunner(jobLauncher, jobRepository);
	}

	@After
	public void tearDown() {
		this.jdbcTemplate.update("drop table TESTS");
	}

	@Test
	public void testTasklet() {
		Step step = (Step) context.getBean("s2");
		assertEquals(BatchStatus.COMPLETED, stepRunner.launchStep(step).getStatus());
		assertEquals(2, jdbcTemplate.queryForObject("SELECT ID from TESTS where NAME = 'SampleTasklet2'", Integer.class).intValue());
	}

    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

}
