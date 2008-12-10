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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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

		//create mock for OutputSource
		output = createMock(ResourceLifecycleItemWriter.class);

		//create new writer
		writer = new FlatFileCustomerCreditDao();
		writer.setItemWriter(output);
	}

	@Test
	public void testOpen() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		//set-up outputSource mock
		output.open(executionContext);
		replay(output);

		//call tested method
		writer.open(executionContext);
		
		//verify method calls
		verify(output);
	}
	
	@Test
	public void testClose() throws Exception{
		
		//set-up outputSource mock
		output.close();
		replay(output);

		//call tested method
		writer.close();
		
		//verify method calls
		verify(output);
	}
	
	@Test
	public void testWrite() throws Exception {
		
		//Create and set-up CustomerCredit
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(1));
		credit.setName("testName");
		
		//set separator
		writer.setSeparator(";");
		
		//set-up OutputSource mock
		output.write(Collections.singletonList("testName;1"));
		output.open(new ExecutionContext());
		replay(output);

		//call tested method
		writer.writeCredit(credit);
		
		//verify method calls
		verify(output);
	}
	
	private interface ResourceLifecycleItemWriter extends ItemWriter<String>, ItemStream{
		
	}
}
