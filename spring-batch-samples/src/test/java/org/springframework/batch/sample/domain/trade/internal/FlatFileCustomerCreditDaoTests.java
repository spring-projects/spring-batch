/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

public class FlatFileCustomerCreditDaoTests {
	private ResourceLifecycleItemWriter output;
	private FlatFileCustomerCreditDao writer;
	
	@Before
	public void setUp() throws Exception {
		output = mock(ResourceLifecycleItemWriter.class);

		writer = new FlatFileCustomerCreditDao();
		writer.setItemWriter(output);
	}

	@Test
	public void testOpen() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();

		output.open(executionContext);

		writer.open(executionContext);
	}
	
	@Test
	public void testClose() throws Exception{
		output.close();

		writer.close();
	}
	
	@Test
	public void testWrite() throws Exception {
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(1));
		credit.setName("testName");
		
		writer.setSeparator(";");
		
		output.write(Collections.singletonList("testName;1"));
		output.open(new ExecutionContext());

		writer.writeCredit(credit);
	}
	
	private interface ResourceLifecycleItemWriter extends ItemWriter<String>, ItemStream {

	}
}
