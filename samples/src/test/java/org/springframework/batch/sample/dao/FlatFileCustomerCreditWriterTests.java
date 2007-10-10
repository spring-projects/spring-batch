package org.springframework.batch.sample.dao;

import java.math.BigDecimal;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.sample.domain.CustomerCredit;

import org.easymock.MockControl;
import junit.framework.TestCase;

public class FlatFileCustomerCreditWriterTests extends TestCase {

	private MockControl outputControl;
	private ResourceLifecycleOutputSource output;
	private FlatFileCustomerCreditWriter writer;
	
	public void setUp() throws Exception {
		super.setUp();
		
		//create mock for OutputSource
		outputControl = MockControl.createControl(ResourceLifecycleOutputSource.class);
		output = (ResourceLifecycleOutputSource)outputControl.getMock();
		
		//create new writer
		writer = new FlatFileCustomerCreditWriter();
		writer.setOutputSource(output);
	}

	public void testOpen() {
		
		//set-up outputSource mock
		output.open();
		outputControl.replay();
		
		//call tested method
		writer.open();
		
		//verify method calls
		outputControl.verify();
	}
	
	public void testClose() {
		
		//set-up outputSource mock
		output.close();
		outputControl.replay();
		
		//call tested method
		writer.close();
		
		//verify method calls
		outputControl.verify();
	}
	
	public void testWrite() {
		
		//Create and set-up CustomerCredit
		CustomerCredit credit = new CustomerCredit();
		credit.setCredit(new BigDecimal(1));
		credit.setName("testName");
		
		//set separator
		writer.setSeparator(";");
		
		//set-up OutputSource mock
		output.write("testName;1");
		output.open();
		outputControl.replay();
		
		//call tested method
		writer.write(credit);
		
		//verify method calls
		outputControl.verify();
	}
	
	private interface ResourceLifecycleOutputSource extends OutputSource, ResourceLifecycle {
		
	}
}
