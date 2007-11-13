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
package org.springframework.batch.sample.dao;

import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * @author Lucas Ward
 * 
 */
public class HibernateCreditWriter extends HibernateDaoSupport implements
		CustomerCreditWriter {

	private boolean failOnFlush = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.sample.dao.CustomerCreditWriter#write(org.springframework.batch.sample.domain.CustomerCredit)
	 */
	public void write(CustomerCredit customerCredit) {
		if (!failOnFlush ) {
			getHibernateTemplate().update(customerCredit);
		} else {
			// try to insert one with a duplicate ID
			CustomerCredit newCredit = new CustomerCredit();
			newCredit.setId(customerCredit.getId());
			newCredit.setName(customerCredit.getName());
			newCredit.setCredit(customerCredit.getCredit());
			getHibernateTemplate().save(newCredit);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) {
		write((CustomerCredit) output);
	}

	/**
	 * Public setter for the {@link boolean} property.
	 *
	 * @param failOnFlush true if you want to fail on flush (for testing)
	 */
	public void setFailOnFlush(boolean failOnFlush) {
		this.failOnFlush = failOnFlush;
	}

}
