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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class HibernateCreditDao extends HibernateDaoSupport implements
		CustomerCreditDao, RepeatListener {

	private int failOnFlush = -1;
	private List<Throwable> errors = new ArrayList<Throwable>();

	/**
	 * Public accessor for the errors property.
	 * 
	 * @return the errors - a list of Throwable instances
	 */
	public List<Throwable> getErrors() {
		return errors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.sample.domain.trade.internal.CustomerCreditWriter#write(org.springframework.batch.sample.domain.CustomerCredit)
	 */
	public void writeCredit(CustomerCredit customerCredit) {
		if (customerCredit.getId() == failOnFlush) {
			// try to insert one with a duplicate ID
			CustomerCredit newCredit = new CustomerCredit();
			newCredit.setId(customerCredit.getId());
			newCredit.setName(customerCredit.getName());
			newCredit.setCredit(customerCredit.getCredit());
			getHibernateTemplate().save(newCredit);
		} else {
			getHibernateTemplate().update(customerCredit);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) {
		writeCredit((CustomerCredit) output);
	}

	/**
	 * Public setter for the failOnFlush property.
	 * 
	 * @param failOnFlush
	 *            the ID of the record you want to fail on flush (for testing)
	 */
	public void setFailOnFlush(int failOnFlush) {
		this.failOnFlush = failOnFlush;
	}

	public void onError(RepeatContext context, Throwable e) {
		errors.add(e);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#after(org.springframework.batch.repeat.RepeatContext, org.springframework.batch.repeat.ExitStatus)
	 */
	public void after(RepeatContext context, RepeatStatus result) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#before(org.springframework.batch.repeat.RepeatContext)
	 */
	public void before(RepeatContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#close(org.springframework.batch.repeat.RepeatContext)
	 */
	public void close(RepeatContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatInterceptor#open(org.springframework.batch.repeat.RepeatContext)
	 */
	public void open(RepeatContext context) {
	}

}
