package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.dao.CustomerCreditWriter;
import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.batch.sample.item.processor.CustomerCreditUpdateProcessor;

public class CustomerCreditUpdateProcessorTests extends TestCase {

	private MockControl writerControl;
	private CustomerCreditWriter writer;
	private CustomerCreditUpdateProcessor processor;
	private static final double CREDIT_FILTER = 355.0;
	
	public void setUp() {
		//create mock writer
		writerControl = MockControl.createControl(CustomerCreditWriter.class);
		writer = (CustomerCreditWriter) writerControl.getMock();
		//create processor, set writer and credit filter
		processor = new CustomerCreditUpdateProcessor();
		processor.setWriter(writer);
		processor.setCreditFilter(CREDIT_FILTER);
	}
	
	public void testProcess() {
		
		//set-up mock writer - no writer's method should be called 
		writerControl.replay();
		
		//create credit and set it to same value as credit filter
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(CREDIT_FILTER));
		//call tested method
		processor.process(credit);
		//verify method calls - no method should be called 
		//because credit is not greater then credit filter
		writerControl.verify();
		
		//change credit to be greater than credit filter
		credit.setCredit(new BigDecimal(CREDIT_FILTER + 1));
		//reset and set-up writer - write method is expected to be called
		writerControl.reset();
		writer.writeCredit(credit);
		writerControl.replay();
		
		//call tested method
		processor.process(credit);
		
		//verify method calls
		writerControl.verify();
	}
	
}
