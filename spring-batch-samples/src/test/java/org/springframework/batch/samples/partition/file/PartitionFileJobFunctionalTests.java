/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.samples.partition.file;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/partition/file/job/partitionFileJob.xml",
		"/simple-job-launcher-context.xml" })
class PartitionFileJobFunctionalTests implements ApplicationContextAware {

	@Autowired
	@Qualifier("inputTestReader")
	private ItemReader<CustomerCredit> inputReader;

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Check the resulting credits correspond to inputs increased by fixed amount.
	 */
	@Test
	void testUpdateCredit() throws Exception {
		assertTrue(applicationContext.containsBeanDefinition("outputTestReader"),
				"Define a prototype bean called 'outputTestReader' to check the output");

		open(inputReader);
		List<CustomerCredit> inputs = new ArrayList<>(getCredits(inputReader));
		close(inputReader);

		JobExecution jobExecution = jobOperatorTestUtils.startJob();
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		@SuppressWarnings("unchecked")
		ItemReader<CustomerCredit> outputReader = (ItemReader<CustomerCredit>) applicationContext
			.getBean("outputTestReader");
		open(outputReader);
		List<CustomerCredit> outputs = new ArrayList<>(getCredits(outputReader));
		close(outputReader);

		assertEquals(inputs.size(), outputs.size());
		int itemCount = inputs.size();
		assertTrue(itemCount > 0, "No entries were available in the input");

		for (int i = 0; i < itemCount; i++) {
			assertEquals(inputs.get(i).getCredit().add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT).intValue(),
					outputs.get(i).getCredit().intValue());
		}
	}

	/**
	 * Read all credits using the provided reader.
	 */
	private Set<CustomerCredit> getCredits(ItemReader<CustomerCredit> reader) throws Exception {
		CustomerCredit credit;
		Set<CustomerCredit> result = new LinkedHashSet<>();

		while ((credit = reader.read()) != null) {
			result.add(credit);
		}

		return result;
	}

	/**
	 * Open the reader if applicable.
	 */
	private void open(ItemReader<?> reader) {
		if (reader instanceof ItemStream itemStream) {
			itemStream.open(new ExecutionContext());
		}
	}

	/**
	 * Close the reader if applicable.
	 */
	private void close(ItemReader<?> reader) {
		if (reader instanceof ItemStream itemStream) {
			itemStream.close();
		}
	}

}
