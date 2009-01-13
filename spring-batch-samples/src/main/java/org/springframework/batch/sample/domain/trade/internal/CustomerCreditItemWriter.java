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

package org.springframework.batch.sample.domain.trade.internal;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;

/**
 * Delegates actual writing to a custom DAO. 
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditItemWriter implements ItemWriter<CustomerCredit> {

	private CustomerCreditDao customerCreditDao;

	/**
	 * Public setter for the {@link CustomerCreditDao}.
	 * @param customerCreditDao the {@link CustomerCreditDao} to set
	 */
	public void setCustomerCreditDao(CustomerCreditDao customerCreditDao) {
		this.customerCreditDao = customerCreditDao;
	}

	public void write(List<? extends CustomerCredit> customerCredits) throws Exception {
		for (CustomerCredit customerCredit : customerCredits) {
			customerCreditDao.writeCredit(customerCredit);
		}
	}

}
