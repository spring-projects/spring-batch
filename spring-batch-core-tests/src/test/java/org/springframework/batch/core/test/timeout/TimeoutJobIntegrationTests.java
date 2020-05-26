/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.test.timeout;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.test.AbstractIntegrationTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/META-INF/batch/timeoutJob.xml" })
public class TimeoutJobIntegrationTests extends AbstractIntegrationTests {

	/** Logger */
	@SuppressWarnings("unused")
	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	@Qualifier("chunkTimeoutJob")
	private Job chunkTimeoutJob;
	
	@Autowired
	@Qualifier("taskletTimeoutJob")
	private Job taskletTimeoutJob;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Test
	public void testChunkTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(chunkTimeoutJob, new JobParametersBuilder().addLong("id", System.currentTimeMillis())
				.toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	public void testTaskletTimeoutShouldFail() throws Exception {
		JobExecution execution = jobLauncher.run(taskletTimeoutJob, new JobParametersBuilder().addLong("id", System.currentTimeMillis())
				.toJobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

}
