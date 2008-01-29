package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.dao.CustomerCreditDao;
import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.batch.sample.item.writer.CustomerCreditIncreaseWriter;

/**
 * Tests for {@link CustomerCreditIncreaseWriter}.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessorTests extends TestCase{

	private CustomerCreditIncreaseWriter writer = new CustomerCreditIncreaseWriter();
	
	private CustomerCreditDao outputSource;
	private MockControl outputSourceControl = MockControl.createStrictControl(CustomerCreditDao.class);
	
	private CustomerCredit customerCredit = new CustomerCredit();
	
	
	protected void setUp() throws Exception {
		customerCredit.setId(1);
		customerCredit.setName("testCustomer");
		
		outputSource = (CustomerCreditDao) outputSourceControl.getMock();
		writer.setCustomerCreditDao(outputSource);
	}

	/**
	 * Increases customer's credit by fixed value
	 */
	public void testProcess() throws Exception {
		BigDecimal oldCredit = new BigDecimal(10.54);
		customerCredit.setCredit(oldCredit);
		
		outputSource.writeCredit(customerCredit);
		outputSourceControl.setVoidCallable();
		outputSourceControl.replay();
		
		writer.write(customerCredit);
		
		BigDecimal newCredit = customerCredit.getCredit();
		BigDecimal expectedCredit = oldCredit.add(CustomerCreditIncreaseWriter.FIXED_AMOUNT);
		assertTrue(newCredit.compareTo(expectedCredit) == 0);
		outputSourceControl.verify();
	}
}
