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

package org.springframework.batch.samples.domain.trade.internal;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.samples.domain.trade.CustomerDebit;
import org.springframework.batch.samples.domain.trade.CustomerDebitDao;
import org.springframework.batch.samples.domain.trade.Trade;

/**
 * Transforms Trade to a CustomerDebit and asks DAO delegate to write the result.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class CustomerUpdateWriter implements ItemWriter<Trade> {

	private CustomerDebitDao dao;

	@Override
	public void write(Chunk<? extends Trade> trades) {
		for (Trade trade : trades) {
			CustomerDebit customerDebit = new CustomerDebit();
			customerDebit.setName(trade.getCustomer());
			customerDebit.setDebit(trade.getPrice());
			dao.write(customerDebit);
		}
	}

	public void setDao(CustomerDebitDao outputSource) {
		this.dao = outputSource;
	}

}
