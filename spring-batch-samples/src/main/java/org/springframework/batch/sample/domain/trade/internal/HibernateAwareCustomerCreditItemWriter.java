package org.springframework.batch.sample.domain.trade.internal;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateOperations;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Assert;

/**
 * Delegates writing to a custom DAO and flushes + clears hibernate session to
 * fulfill the {@link ItemWriter} contract.
 * 
 * @author Robert Kasanicky
 */
public class HibernateAwareCustomerCreditItemWriter implements ItemWriter<CustomerCredit>, InitializingBean {

	private CustomerCreditDao dao;

	private HibernateOperations hibernateTemplate;

	public void write(List<? extends CustomerCredit> items) throws Exception {
		for (CustomerCredit credit : items) {
			dao.writeCredit(credit);
		}
		try {
			hibernateTemplate.flush();
		}
		finally {
			// this should happen automatically on commit, but to be on the safe
			// side...
			hibernateTemplate.clear();
		}

	}

	public void setDao(CustomerCreditDao dao) {
		this.dao = dao;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(hibernateTemplate, "Hibernate session factory must be set");
		Assert.notNull(dao, "Delegate DAO must be set");
	}

}
