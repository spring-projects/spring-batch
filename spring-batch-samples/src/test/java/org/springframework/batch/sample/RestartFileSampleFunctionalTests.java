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

package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.AssertFile;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Garrette
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/restartFileSampleJob.xml",
		"/job-runner-context.xml" })
public class RestartFileSampleFunctionalTests {

	@Autowired
	private Resource outputResource;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void runTest() throws Exception {
		JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParameters();

		JobExecution je1 = jobLauncherTestUtils.launchJob(jobParameters);
		assertEquals(BatchStatus.FAILED, je1.getStatus());
		AssertFile.assertLineCount(10, outputResource);

		JobExecution je2 = jobLauncherTestUtils.launchJob(jobParameters);
		assertEquals(BatchStatus.COMPLETED, je2.getStatus());
		AssertFile.assertLineCount(20, outputResource);
	}

	public static class CustomerCreditFlatFileItemWriter extends FlatFileItemWriter<CustomerCredit> {

		private boolean failed = false;

		@Override
		public void write(List<? extends CustomerCredit> arg0) throws Exception {
			for (CustomerCredit cc : arg0) {
				if (!failed && cc.getName().equals("customer13")) {
					failed = true;
					throw new RuntimeException();
				}
			}
			super.write(arg0);
		}

	}

}