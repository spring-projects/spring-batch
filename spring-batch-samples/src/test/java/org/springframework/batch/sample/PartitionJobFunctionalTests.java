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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.batch.test.AbstractJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class PartitionJobFunctionalTests extends AbstractJobTests {

	@Autowired
	@Qualifier("inputTestReader")
	private ItemReader<CustomerCredit> inputReader;

	/**
	 * Check the resulting credits correspond to inputs increased by fixed
	 * amount.
	 */
	@Test
	public void testUpdateCredit() throws Exception {

		assertTrue("Define a prototype bean called 'outputTestReader' to check the output", getApplicationContext()
				.containsBeanDefinition("outputTestReader"));

		open(inputReader);
		List<CustomerCredit> inputs = getCredits(inputReader);
		close(inputReader);

		JobExecution jobExecution = this.launchJob();
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		@SuppressWarnings("unchecked")
		ItemReader<CustomerCredit> outputReader = (ItemReader<CustomerCredit>) getApplicationContext().getBean(
				"outputTestReader");
		open(outputReader);
		List<CustomerCredit> outputs = getCredits(outputReader);
		close(outputReader);

		assertEquals(inputs.size(), outputs.size());
		int itemCount = inputs.size();
		assertTrue(itemCount > 0);

		for (int i = 0; i < itemCount; i++) {
			assertEquals(inputs.get(i).getCredit().add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT).intValue(),
					outputs.get(i).getCredit().intValue());
		}

	}

	/**
	 * Read all credits using the provided reader.
	 */
	private List<CustomerCredit> getCredits(ItemReader<CustomerCredit> reader) throws Exception {
		CustomerCredit credit;
		List<CustomerCredit> result = new ArrayList<CustomerCredit>();
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

}
