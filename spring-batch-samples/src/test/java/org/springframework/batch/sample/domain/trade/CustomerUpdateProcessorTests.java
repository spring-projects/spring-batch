/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.sample.domain.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.batch.sample.domain.trade.CustomerOperation.*;

/**
 * @author Lucas Ward
 * @author Glenn Renfro
 *
 */
class CustomerUpdateProcessorTests {

	private CustomerDao customerDao;

	private InvalidCustomerLogger logger;

	private CustomerUpdateProcessor processor;

	@BeforeEach
	void init() {
		customerDao = mock(CustomerDao.class);
		logger = mock(InvalidCustomerLogger.class);
		processor = new CustomerUpdateProcessor();
		processor.setCustomerDao(customerDao);
		processor.setInvalidCustomerLogger(logger);
	}

	@Test
	void testSuccessfulAdd() throws Exception {
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(null);
		assertEquals(customerUpdate, processor.process(customerUpdate));
	}

	@Test
	void testInvalidAdd() throws Exception {
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(new CustomerCredit());
		logger.log(customerUpdate);
		assertNull(processor.process(customerUpdate), "Processor should return null");
	}

	@Test
	void testDelete() throws Exception {
		CustomerUpdate customerUpdate = new CustomerUpdate(DELETE, "test customer", new BigDecimal("232.2"));
		logger.log(customerUpdate);
		assertNull(processor.process(customerUpdate), "Processor should return null");
	}

	@Test
	void testSuccessfulUpdate() throws Exception {
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(new CustomerCredit());
		assertEquals(customerUpdate, processor.process(customerUpdate));
	}

	@Test
	void testInvalidUpdate() throws Exception {
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(null);
		logger.log(customerUpdate);
		assertNull(processor.process(customerUpdate), "Processor should return null");
	}

}
