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
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CustomWiredJsrJobOperatorTests {

	@Autowired
	JobOperator jobOperator;

	@Test
	public void testRunningJobWithManuallyWiredJsrJobOperator() throws Exception {
		Date startTime = new Date();
		long jobExecutionId = jobOperator.start("jsrJobOperatorTestJob", new Properties());

		JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);

		long timeout = startTime.getTime() + 10000;

		while(!jobExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
			Thread.sleep(500);
			jobExecution = jobOperator.getJobExecution(jobExecutionId);

			if(new Date().getTime() > timeout) {
				throw new TimeoutException("Job didn't finish within 10 seconds");
			}
		}
	}
}
