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
package org.springframework.batch.sample.jsr352;

import org.junit.Before;
import org.junit.Test;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Test cases to run JSR-352 configuration samples.
 * </p>
 *
 * @since 3.0
 * @author Chris Schaefer
 */
public class JsrConfigSampleTests {
	private Properties properties = new Properties();

	@Before
	public void setup() {
		properties.setProperty("remoteServiceURL", "https://api.example.com");
	}

	/**
	 * <p>
	 * Use inline class names as batch artifact references.
	 * </p>
	 */
	@Test
	public void inlineConfigSampleTest() {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		Long executionId = jobOperator.start("inlineConfigSample", properties);

		BatchStatus batchStatus = waitForJobComplete(jobOperator, executionId);
		assertTrue(BatchStatus.COMPLETED.equals(batchStatus));
	}

	/**
	 * <p>
	 * Use batch artifact references defined in batch.xml.
	 * </p>
	 */
	@Test
	public void batchXmlConfigSampleTest() {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		Long executionId = jobOperator.start("batchXmlConfigSample", properties);

		BatchStatus batchStatus = waitForJobComplete(jobOperator, executionId);
		assertTrue(BatchStatus.COMPLETED.equals(batchStatus));
	}

	/**
	 * <p>
	 * Use batch artifact references defined via Spring beans.
	 * </p>
	 */
	@Test
	public void springConfigSampleTest() {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		Long executionId = jobOperator.start("springConfigSampleContext", properties);

		BatchStatus batchStatus = waitForJobComplete(jobOperator, executionId);
		assertTrue(BatchStatus.COMPLETED.equals(batchStatus));
	}

	// JSR jobs run async
	private BatchStatus waitForJobComplete(JobOperator jobOperator, long executionId) {
		JobExecution execution = jobOperator.getJobExecution(executionId);

		BatchStatus curBatchStatus = execution.getBatchStatus();

		while(true) {
			if(curBatchStatus == BatchStatus.STOPPED || curBatchStatus == BatchStatus.COMPLETED || curBatchStatus == BatchStatus.FAILED) {
				break;
			}

			execution = jobOperator.getJobExecution(executionId);
			curBatchStatus = execution.getBatchStatus();
		}

		return curBatchStatus;
	}
}
