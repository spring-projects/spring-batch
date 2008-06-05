package org.springframework.batch.sample.item.reader;

import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.LineItem;
import org.springframework.batch.sample.domain.Order;
import org.springframework.batch.sample.domain.ShippingInfo;

public class OrderItemReaderTests extends TestCase {

	private OrderItemReader provider;
	private MockControl inputControl;
	private ItemReader input;
	private MockControl mapperControl;
	private FieldSetMapper mapper;

	public void setUp() {

		inputControl = MockControl.createControl(ItemReader.class);
		input = (ItemReader)inputControl.getMock();

		provider = new OrderItemReader();
		provider.setItemReader(input);
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
	public void testNext() throws Exception {

		//create fieldsets and set return values for input source
		FieldSet headerFS = new DefaultFieldSet(new String[] {Order.LINE_ID_HEADER});
		FieldSet customerFS = new DefaultFieldSet(new String[] {Customer.LINE_ID_NON_BUSINESS_CUST});
		FieldSet billingFS = new DefaultFieldSet(new String[] {Address.LINE_ID_BILLING_ADDR});
		FieldSet shippingFS = new DefaultFieldSet(new String[] {Address.LINE_ID_SHIPPING_ADDR});
		FieldSet billingInfoFS = new DefaultFieldSet(new String[] {BillingInfo.LINE_ID_BILLING_INFO});
		FieldSet shippingInfoFS = new DefaultFieldSet(new String[] {ShippingInfo.LINE_ID_SHIPPING_INFO});
		FieldSet itemFS = new DefaultFieldSet(new String[] {LineItem.LINE_ID_ITEM});
		FieldSet footerFS = new DefaultFieldSet(new String[] {Order.LINE_ID_FOOTER, "100","3","3"},
										 new String[] {"ID","TOTAL_PRICE","TOTAL_LINE_ITEMS","TOTAL_ITEMS"});

		input.read();
		inputControl.setReturnValue(headerFS);
		input.read();
		inputControl.setReturnValue(customerFS);
		input.read();
		inputControl.setReturnValue(billingFS);
		input.read();
		inputControl.setReturnValue(shippingFS);
		input.read();
		inputControl.setReturnValue(billingInfoFS);
		input.read();
		inputControl.setReturnValue(shippingInfoFS);
		input.read();
		inputControl.setReturnValue(itemFS,3);
		input.read();
		inputControl.setReturnValue(footerFS);
		input.read();
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
		mapper.mapLine(headerFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(order);
		mapper.mapLine(customerFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(customer);
		mapper.mapLine(billingFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(billing);
		mapper.mapLine(shippingFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(shipping);
		mapper.mapLine(billingInfoFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(billingInfo);
		mapper.mapLine(shippingInfoFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(shippingInfo);
		mapper.mapLine(itemFS, FieldSetMapper.ROW_NUMBER_UNKNOWN);
		mapperControl.setReturnValue(item,3);
		mapperControl.replay();


		//set-up provider: set mappers 
		provider.setAddressMapper(mapper);
		provider.setBillingMapper(mapper);
		provider.setCustomerMapper(mapper);
		provider.setHeaderMapper(mapper);
		provider.setItemMapper(mapper);
		provider.setShippingMapper(mapper);

		//call tested method
		Object result = provider.read();

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
		assertNull(provider.read());

		//verify method calls on input source, mapper and validator
		inputControl.verify();
		mapperControl.verify();
	}

}
