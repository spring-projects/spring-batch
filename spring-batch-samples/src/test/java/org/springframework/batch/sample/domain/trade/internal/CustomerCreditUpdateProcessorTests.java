package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		//create mock writer
		dao = mock(CustomerCreditDao.class);
		//create processor, set writer and credit filter
		writer = new CustomerCreditUpdateWriter();
		writer.setDao(dao);
		writer.setCreditFilter(CREDIT_FILTER);
	}
	
	@Test
	public void testProcess() throws Exception {
		
		//set-up mock writer - no writer's method should be called 
		
		//create credit and set it to same value as credit filter
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(CREDIT_FILTER));
		//call tested method
		writer.write(Collections.singletonList(credit));
		//verify method calls - no method should be called 
		//because credit is not greater then credit filter
		
		//change credit to be greater than credit filter
		credit.setCredit(new BigDecimal(CREDIT_FILTER + 1));
		//reset and set-up writer - write method is expected to be called
		dao.writeCredit(credit);
		
		//call tested method
		writer.write(Collections.singletonList(credit));
		
	}
	
}
