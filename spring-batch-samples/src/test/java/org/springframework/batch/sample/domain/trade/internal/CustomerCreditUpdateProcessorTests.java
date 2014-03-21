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
