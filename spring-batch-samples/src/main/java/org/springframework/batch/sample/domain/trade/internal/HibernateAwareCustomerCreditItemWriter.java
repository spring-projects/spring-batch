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

package org.springframework.batch.sample.domain.trade.internal;

import org.hibernate.SessionFactory;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Delegates writing to a custom DAO and flushes + clears hibernate session to fulfill the
 * {@link ItemWriter} contract.
 *
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class HibernateAwareCustomerCreditItemWriter implements ItemWriter<CustomerCredit>, InitializingBean {

	private CustomerCreditDao dao;

	private SessionFactory sessionFactory;

	@Override
	public void write(Chunk<? extends CustomerCredit> items) throws Exception {
		for (CustomerCredit credit : items) {
			dao.writeCredit(credit);
		}
		try {
			sessionFactory.getCurrentSession().flush();
		}
		finally {
			// this should happen automatically on commit, but to be on the safe
			// side...
			sessionFactory.getCurrentSession().clear();
		}

	}

	public void setDao(CustomerCreditDao dao) {
		this.dao = dao;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(sessionFactory != null, "Hibernate SessionFactory is required");
		Assert.state(dao != null, "Delegate DAO must be set");
	}

}
