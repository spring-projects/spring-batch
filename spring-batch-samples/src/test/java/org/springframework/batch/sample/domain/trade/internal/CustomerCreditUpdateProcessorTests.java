/*
 * Copyright 2008-2014 the original author or authors.
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
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;

public class CustomerCreditUpdateProcessorTests {
	private CustomerCreditDao dao;
	private CustomerCreditUpdateWriter writer;
	private static final double CREDIT_FILTER = 355.0;
	
	@Before
	public void setUp() {
		dao = mock(CustomerCreditDao.class);

		writer = new CustomerCreditUpdateWriter();
		writer.setDao(dao);
		writer.setCreditFilter(CREDIT_FILTER);
	}
	
	@Test
	public void testProcess() throws Exception {
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(CREDIT_FILTER));

		writer.write(Collections.singletonList(credit));

		credit.setCredit(new BigDecimal(CREDIT_FILTER + 1));

		dao.writeCredit(credit);
		
		writer.write(Collections.singletonList(credit));
	}
}
