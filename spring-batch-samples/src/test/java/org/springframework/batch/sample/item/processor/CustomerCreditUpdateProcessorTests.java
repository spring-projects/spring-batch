package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.dao.CustomerCreditDao;
import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.batch.sample.item.writer.CustomerCreditUpdateWriter;

public class CustomerCreditUpdateProcessorTests extends TestCase {

	private MockControl daoControl;
	private CustomerCreditDao dao;
	private CustomerCreditUpdateWriter writer;
	private static final double CREDIT_FILTER = 355.0;
	
	public void setUp() {
		//create mock writer
		daoControl = MockControl.createControl(CustomerCreditDao.class);
		dao = (CustomerCreditDao) daoControl.getMock();
		//create processor, set writer and credit filter
		writer = new CustomerCreditUpdateWriter();
		writer.setWriter(dao);
		writer.setCreditFilter(CREDIT_FILTER);
	}
	
	public void testProcess() throws Exception {
		
		//set-up mock writer - no writer's method should be called 
		daoControl.replay();
		
		//create credit and set it to same value as credit filter
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(CREDIT_FILTER));
		//call tested method
		writer.write(credit);
		//verify method calls - no method should be called 
		//because credit is not greater then credit filter
		daoControl.verify();
		
		//change credit to be greater than credit filter
		credit.setCredit(new BigDecimal(CREDIT_FILTER + 1));
		//reset and set-up writer - write method is expected to be called
		daoControl.reset();
		dao.writeCredit(credit);
		daoControl.replay();
		
		//call tested method
		writer.write(credit);
		
		//verify method calls
		daoControl.verify();
	}
	
}
