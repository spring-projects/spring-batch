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

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/ioSampleJob.xml",
		"/jobs/iosample/delimited.xml" })
public class TwoJobInstancesDelimitedFunctionalTests {

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private Job job;

	@Autowired
	private ItemReader<CustomerCredit> reader;

	@Autowired
	@Qualifier("itemReader")
	private ItemStream readerStream;

	@Test
	public void testLaunchJobTwice() throws Exception {
		JobExecution jobExecution = launcher.run(this.job, getJobParameters("data/iosample/input/delimited.csv"));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		verifyOutput(6);
		jobExecution = launcher.run(this.job, getJobParameters("data/iosample/input/delimited2.csv"));
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		verifyOutput(2);
	}

	private void verifyOutput(int expected) throws Exception {

		JobParameters jobParameters = new JobParametersBuilder().addString("inputFile",
				"file:./target/test-outputs/delimitedOutput.csv").toJobParameters();
		StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);

		int count = StepScopeTestUtils.doInStepScope(stepExecution, new Callable<Integer>() {

			public Integer call() throws Exception {

				int count = 0;

				readerStream.open(new ExecutionContext());

				try {
					while (reader.read() != null) {
						count++;
					}
				}
				finally {
					readerStream.close();
				}
				return count;

			}

		});

		assertEquals(expected, count);

	}

	protected JobParameters getJobParameters(String fileName) {
		return new JobParametersBuilder().addLong("timestamp", new Date().getTime()).addString("inputFile", fileName)
				.addString("outputFile", "file:./target/test-outputs/delimitedOutput.csv").toJobParameters();
	}

}