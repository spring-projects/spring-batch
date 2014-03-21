package org.springframework.batch.sample.domain.trade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.batch.sample.domain.trade.CustomerOperation.ADD;
import static org.springframework.batch.sample.domain.trade.CustomerOperation.DELETE;
import static org.springframework.batch.sample.domain.trade.CustomerOperation.UPDATE;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Lucas Ward
 *
 */
public class CustomerUpdateProcessorTests {
	private CustomerDao customerDao;
	private InvalidCustomerLogger logger;
	private CustomerUpdateProcessor processor;
	
	@Before
	public void init(){
		customerDao = mock(CustomerDao.class);
		logger = mock(InvalidCustomerLogger.class);
		processor = new CustomerUpdateProcessor();
		processor.setCustomerDao(customerDao);
		processor.setInvalidCustomerLogger(logger);
	}
	
	@Test
	public void testSuccessfulAdd() throws Exception{
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(null);
		assertEquals(customerUpdate, processor.process(customerUpdate));
	}
	
	@Test
	public void testInvalidAdd() throws Exception{
		CustomerUpdate customerUpdate = new CustomerUpdate(ADD, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(new CustomerCredit());
		logger.log(customerUpdate);
		assertNull("Processor should return null", processor.process(customerUpdate));
	}
	
	@Test
	public void testDelete() throws Exception{
		CustomerUpdate customerUpdate = new CustomerUpdate(DELETE, "test customer", new BigDecimal("232.2"));
		logger.log(customerUpdate);
		assertNull("Processor should return null", processor.process(customerUpdate));
	}
	
	@Test
	public void testSuccessfulUpdate() throws Exception{
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(new CustomerCredit());
		assertEquals(customerUpdate, processor.process(customerUpdate));
	}
	
	@Test
	public void testInvalidUpdate() throws Exception{
		CustomerUpdate customerUpdate = new CustomerUpdate(UPDATE, "test customer", new BigDecimal("232.2"));
		when(customerDao.getCustomerByName("test customer")).thenReturn(null);
		logger.log(customerUpdate);
		assertNull("Processor should return null", processor.process(customerUpdate));
	}
}
