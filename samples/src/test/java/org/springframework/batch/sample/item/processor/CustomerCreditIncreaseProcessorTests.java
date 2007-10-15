package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import org.easymock.MockControl;
import org.springframework.batch.io.OutputSource;
import org.springframework.batch.sample.domain.CustomerCredit;

import junit.framework.TestCase;

/**
 * Tests for {@link CustomerCreditIncreaseProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessorTests extends TestCase{

	private CustomerCreditIncreaseProcessor processor = new CustomerCreditIncreaseProcessor();
	
	private OutputSource outputSource;
	private MockControl outputSourceControl = MockControl.createStrictControl(OutputSource.class);
	
	private CustomerCredit customerCredit = new CustomerCredit();
	
	
	protected void setUp() throws Exception {
		customerCredit.setId(1);
		customerCredit.setName("testCustomer");
		
		outputSource = (OutputSource) outputSourceControl.getMock();
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
