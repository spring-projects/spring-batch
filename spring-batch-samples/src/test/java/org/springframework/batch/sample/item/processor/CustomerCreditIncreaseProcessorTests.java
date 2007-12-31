package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * Tests for {@link CustomerCreditIncreaseProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessorTests extends TestCase{

	private CustomerCreditIncreaseProcessor processor = new CustomerCreditIncreaseProcessor();
	
	private ItemWriter outputSource;
	private MockControl outputSourceControl = MockControl.createStrictControl(ItemWriter.class);
	
	private CustomerCredit customerCredit = new CustomerCredit();
	
	
	protected void setUp() throws Exception {
		customerCredit.setId(1);
		customerCredit.setName("testCustomer");
		
		outputSource = (ItemWriter) outputSourceControl.getMock();
		processor.setOutputSource(outputSource);
	}

	/**
	 * Increases customer's credit by fixed value
	 */
	public void testProcess() throws Exception {
		BigDecimal oldCredit = new BigDecimal(10.54);
		customerCredit.setCredit(oldCredit);
		
		outputSource.write(customerCredit);
		outputSourceControl.setVoidCallable();
		outputSourceControl.replay();
		
		processor.process(customerCredit);
		
		BigDecimal newCredit = customerCredit.getCredit();
		BigDecimal expectedCredit = oldCredit.add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT);
		assertTrue(newCredit.compareTo(expectedCredit) == 0);
		outputSourceControl.verify();
	}
}
