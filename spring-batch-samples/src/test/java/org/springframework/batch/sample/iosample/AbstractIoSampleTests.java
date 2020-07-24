/*
 * Copyright 2008-2020 the original author or authors.
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
package org.springframework.batch.sample.iosample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * Base class for IoSample tests that increase input customer credit by fixed
 * amount. Assumes inputs and outputs are in the same format and uses the job's
 * {@link ItemReader} to parse the outputs.
 * 
 * @author Robert Kasanicky
 */
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/job-runner-context.xml",
		"/jobs/ioSampleJob.xml" })
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class })
public abstract class AbstractIoSampleTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private ItemReader<CustomerCredit> reader;

	/**
	 * Check the resulting credits correspond to inputs increased by fixed
	 * amount.
	 */
	@Test
	public void testUpdateCredit() throws Exception {

		open(reader);
		List<CustomerCredit> inputs = getCredits(reader);
		close(reader);

		JobExecution jobExecution = jobLauncherTestUtils.launchJob(getUniqueJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		pointReaderToOutput(reader);
		open(reader);
		List<CustomerCredit> outputs = getCredits(reader);
		close(reader);

		assertEquals(inputs.size(), outputs.size());
		int itemCount = inputs.size();
		assertTrue(itemCount > 0);

		for (int i = 0; i < itemCount; i++) {
			assertEquals(inputs.get(i).getCredit().add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT).intValue(),
					outputs.get(i).getCredit().intValue());
		}

	}

	protected JobParameters getUniqueJobParameters() {
		return jobLauncherTestUtils.getUniqueJobParameters();
	}

	protected JobParametersBuilder getUniqueJobParametersBuilder() {
		return jobLauncherTestUtils.getUniqueJobParametersBuilder();
	}

	/**
	 * Configure the reader to read outputs (if necessary). Required for
	 * file-to-file jobs jobs, usually no-op for database jobs where inputs are
	 * updated (rather than outputs created).
	 */
	protected abstract void pointReaderToOutput(ItemReader<CustomerCredit> reader);

	/**
	 * Read all credits using the provided reader.
	 */
	private List<CustomerCredit> getCredits(ItemReader<CustomerCredit> reader) throws Exception {
		CustomerCredit credit;
		List<CustomerCredit> result = new ArrayList<>();
		while ((credit = reader.read()) != null) {
			result.add(credit);
		}
		return result;

	}

	/**
	 * Open the reader if applicable.
	 */
	private void open(ItemReader<?> reader) {
		if (reader instanceof ItemStream) {
			((ItemStream) reader).open(new ExecutionContext());
		}
	}

	/**
	 * Close the reader if applicable.
	 */
	private void close(ItemReader<?> reader) {
		if (reader instanceof ItemStream) {
			((ItemStream) reader).close();
		}
	}

	/**
	 * Create a {@link StepExecution} that can be used to satisfy step scoped
	 * dependencies in the test itself (not in the job it launches).
	 * 
	 * @return a {@link StepExecution}
	 */
	protected StepExecution getStepExecution() {
		return MetaDataInstanceFactory.createStepExecution(getUniqueJobParameters());
	}

}
