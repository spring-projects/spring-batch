/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import static org.easymock.EasyMock.*;
import static org.springframework.batch.sample.domain.trade.CustomerOperation.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Lucas Ward
 *
 */
public class CustomerUpdateProcessorTests {

	CustomerDao customerDao;
	InvalidCustomerLogger logger;
	CustomerUpdateProcessor processor;
	
	@Before
	public void init(){
		customerDao = createMock(CustomerDao.class);
		logger = createMock(InvalidCustomerLogger.class);
		processor = new CustomerUpdateProcessor();
		processor.setCustomerDao(customerDao);
		processor.setInvalidCustomerLogger(logger);
	}
	
	@Test
	public void testSuccessfulAdd() throws Exception{
		
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal(232.2));
		expect(customerDao.getCustomerByName("test customer")).andReturn(null);
		replay(customerDao);
		assertEquals(customerUpdate, processor.process(customerUpdate));
		verify(customerDao);
	}
	
	@Test
	public void testInvalidAdd() throws Exception{
		
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal(232.2));
		expect(customerDao.getCustomerByName("test customer")).andReturn(new CustomerCredit());
		logger.log(customerUpdate);
		replay(customerDao, logger);
		assertNull("Processor should return null", processor.process(customerUpdate));
		verify(customerDao, logger);
	}
	
	@Test
	public void testDelete() throws Exception{
		//delete should never work, therefore, ensure it fails fast.
		CustomerUpdate customerUpdate = new CustomerUpdate(DELETE, "test customer", new BigDecimal(232.2));
		logger.log(customerUpdate);
		replay(customerDao, logger);
		assertNull("Processor should return null", processor.process(customerUpdate));
		verify(customerDao, logger);
	}
	
	@Test
	public void testSuccessfulUpdate() throws Exception{
		
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal(232.2));
		expect(customerDao.getCustomerByName("test customer")).andReturn(new CustomerCredit());
		replay(customerDao, logger);
		assertEquals(customerUpdate, processor.process(customerUpdate));
		verify(customerDao, logger);
	}
	
	@Test
	public void testInvalidUpdate() throws Exception{
		
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal(232.2));
		expect(customerDao.getCustomerByName("test customer")).andReturn(null);
		logger.log(customerUpdate);
		replay(customerDao, logger);
		assertNull("Processor should return null", processor.process(customerUpdate));
		verify(customerDao, logger);
	}
	
}
