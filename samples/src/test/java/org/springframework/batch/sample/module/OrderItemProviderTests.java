package org.springframework.batch.sample.module;

import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.LineItem;
import org.springframework.batch.sample.domain.Order;
import org.springframework.batch.sample.domain.ShippingInfo;

public class OrderItemProviderTests extends TestCase {

	private OrderItemProvider provider;
	private MockControl inputControl;
	private FieldSetInputSource input;
	private MockControl mapperControl;
	private FieldSetMapper mapper;
	private MockControl validatorControl;
	private Validator validator;
	
	public void setUp() {
		
		inputControl = MockControl.createControl(FieldSetInputSource.class);
		input = (FieldSetInputSource)inputControl.getMock();
				
		provider = new OrderItemProvider();
		provider.setInputSource(input);
	}
	
	/*
	 * OrderItemProvider is resposible for retrieving validated value object from input source.
	 * OrderItemProvider.next():
	 * - reads lines from the input source - returned as fieldsets 
	 * - pass fieldsets to the mapper - mapper will create value object
	 * - pass value object to validator
	 * - returns validated object
	 * 
	 * In testNext method we are going to test these responsibilities. So we need create mock
	 * objects for input source, mapper and validator.
	 */
	public void testNext() {

		//create fieldsets and set return values for input source
		FieldSet headerFS = new FieldSet(new String[] {Order.LINE_ID_HEADER});
		FieldSet customerFS = new FieldSet(new String[] {Customer.LINE_ID_NON_BUSINESS_CUST});
		FieldSet billingFS = new FieldSet(new String[] {Address.LINE_ID_BILLING_ADDR});
		FieldSet shippingFS = new FieldSet(new String[] {Address.LINE_ID_SHIPPING_ADDR});
		FieldSet billingInfoFS = new FieldSet(new String[] {BillingInfo.LINE_ID_BILLING_INFO});
		FieldSet shippingInfoFS = new FieldSet(new String[] {ShippingInfo.LINE_ID_SHIPPING_INFO});
		FieldSet itemFS = new FieldSet(new String[] {LineItem.LINE_ID_ITEM});
		FieldSet footerFS = new FieldSet(new String[] {Order.LINE_ID_FOOTER, "100","3","3"},
										 new String[] {"ID","TOTAL_PRICE","TOTAL_LINE_ITEMS","TOTAL_ITEMS"});

		input.readFieldSet();
		inputControl.setReturnValue(headerFS);
		input.readFieldSet();
		inputControl.setReturnValue(customerFS);
		input.readFieldSet();
		inputControl.setReturnValue(billingFS);
		input.readFieldSet();
		inputControl.setReturnValue(shippingFS);
		input.readFieldSet();
		inputControl.setReturnValue(billingInfoFS);
		input.readFieldSet();
		inputControl.setReturnValue(shippingInfoFS);
		input.readFieldSet();
		inputControl.setReturnValue(itemFS,3);
		input.readFieldSet();
		inputControl.setReturnValue(footerFS);
		input.readFieldSet();
		inputControl.setReturnValue(null);
		inputControl.replay();
		
		//create value objects
		Order order = new Order();
		Customer customer = new Customer();
		Address billing = new Address();
		Address shipping = new Address();
		BillingInfo billingInfo = new BillingInfo();
		ShippingInfo shippingInfo = new ShippingInfo();
		LineItem item = new LineItem();
		
		//create mock mapper
		mapperControl = MockControl.createControl(FieldSetMapper.class);
		mapper = (FieldSetMapper)mapperControl.getMock();
		//set how mapper should respond - set return values for mapper 		
		mapper.mapLine(headerFS);
		mapperControl.setReturnValue(order);
		mapper.mapLine(customerFS);
		mapperControl.setReturnValue(customer);
		mapper.mapLine(billingFS);
		mapperControl.setReturnValue(billing);
		mapper.mapLine(shippingFS);
		mapperControl.setReturnValue(shipping);
		mapper.mapLine(billingInfoFS);
		mapperControl.setReturnValue(billingInfo);
		mapper.mapLine(shippingInfoFS);
		mapperControl.setReturnValue(shippingInfo);
		mapper.mapLine(itemFS);
		mapperControl.setReturnValue(item,3);
		mapperControl.replay();
		
		//create mock validator
		validatorControl = MockControl.createControl(Validator.class);
		validator = (Validator)validatorControl.getMock();
		validator.validate(null);
		validatorControl.setMatcher(MockControl.ALWAYS_MATCHER);
		validatorControl.setVoidCallable(1);
		validatorControl.replay();

		//set-up provider: set mappers and validator
		provider.setValidator(validator);
		provider.setAddressMapper(mapper);
		provider.setBillingMapper(mapper);
		provider.setCustomerMapper(mapper);
		provider.setHeaderMapper(mapper);
		provider.setItemMapper(mapper);
		provider.setShippingMapper(mapper);
	
		//call tested method
		Object result = provider.next();
		
		//verify result
		assertNotNull(result);
		//result should be Order
		assertTrue(result instanceof Order);
		
		//verify whether order is constructed correctly
		//Order object should contain same instances as returned by mapper
		Order o = (Order) result;
		assertEquals(o,order);
		assertEquals(o.getCustomer(),customer);
		//is it non-bussines customer
		assertFalse(o.getCustomer().isBusinessCustomer());
		assertEquals(o.getBillingAddress(),billing);
		assertEquals(o.getShippingAddress(),shipping);
		assertEquals(o.getBilling(),billingInfo);
		assertEquals(o.getShipping(), shippingInfo);
		//there should be 3 line items
		assertEquals(3, o.getLineItems().size());
		for (Iterator i = o.getLineItems().iterator(); i.hasNext();) {
			assertEquals(i.next(),item);
		}
		
		//try to retrieve next object - nothing should be returned
		assertNull(provider.next());
		
		//verify method calls on input source, mapper and validator
		inputControl.verify();
		mapperControl.verify();
		validatorControl.verify();
	}
	
}
