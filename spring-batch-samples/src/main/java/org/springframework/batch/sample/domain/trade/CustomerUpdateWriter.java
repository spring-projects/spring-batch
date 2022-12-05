/*
 * Copyright 2006-2022 the original author or authors.
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

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public class CustomerUpdateWriter implements ItemWriter<CustomerUpdate> {

	private CustomerDao customerDao;

	@Override
	public void write(Chunk<? extends CustomerUpdate> items) throws Exception {
		for (CustomerUpdate customerUpdate : items) {
			if (customerUpdate.getOperation() == CustomerOperation.ADD) {
				customerDao.insertCustomer(customerUpdate.getCustomerName(), customerUpdate.getCredit());
			}
			else if (customerUpdate.getOperation() == CustomerOperation.UPDATE) {
				customerDao.updateCustomer(customerUpdate.getCustomerName(), customerUpdate.getCredit());
			}
		}

		// flush and/or clear resources
	}

	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}

}
