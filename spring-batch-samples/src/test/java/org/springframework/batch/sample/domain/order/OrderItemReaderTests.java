package org.springframework.batch.sample.domain.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.OrderItemReader;

public class OrderItemReaderTests {

	private OrderItemReader provider;

	private ItemReader<FieldSet> input;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {

		input = (ItemReader<FieldSet>) mock(ItemReader.class);

		provider = new OrderItemReader();
		provider.setFieldSetReader(input);
	}

	/*
	 * OrderItemProvider is resposible for retrieving validated value object
	 * from input source. OrderItemProvider.next(): - reads lines from the input
	 * source - returned as fieldsets - pass fieldsets to the mapper - mapper
	 * will create value object - pass value object to validator - returns
	 * validated object
	 * 
	 * In testNext method we are going to test these responsibilities. So we
	 * need create mock objects for input source, mapper and validator.
	 */
	@Ignore //TODO mockito fix
	@SuppressWarnings("unchecked")
	@Test
	public void testNext() throws Exception {

		// create fieldsets and set return values for input source
		FieldSet headerFS = new DefaultFieldSet(new String[] { Order.LINE_ID_HEADER });
		FieldSet customerFS = new DefaultFieldSet(new String[] { Customer.LINE_ID_NON_BUSINESS_CUST });
		FieldSet billingFS = new DefaultFieldSet(new String[] { Address.LINE_ID_BILLING_ADDR });
		FieldSet shippingFS = new DefaultFieldSet(new String[] { Address.LINE_ID_SHIPPING_ADDR });
		FieldSet billingInfoFS = new DefaultFieldSet(new String[] { BillingInfo.LINE_ID_BILLING_INFO });
		FieldSet shippingInfoFS = new DefaultFieldSet(new String[] { ShippingInfo.LINE_ID_SHIPPING_INFO });
		FieldSet itemFS = new DefaultFieldSet(new String[] { LineItem.LINE_ID_ITEM });
		FieldSet footerFS = new DefaultFieldSet(new String[] { Order.LINE_ID_FOOTER, "100", "3", "3" }, new String[] {
				"ID", "TOTAL_PRICE", "TOTAL_LINE_ITEMS", "TOTAL_ITEMS" });

		when(input.read()).thenReturn(headerFS);
		when(input.read()).thenReturn(customerFS);
		when(input.read()).thenReturn(billingFS);
		when(input.read()).thenReturn(shippingFS);
		when(input.read()).thenReturn(billingInfoFS);
		when(input.read()).thenReturn(shippingInfoFS);
		when(input.read()).thenReturn(itemFS);
		when(input.read()).thenReturn(footerFS);
		when(input.read()).thenReturn(null);
//		replay(input);
//		input.read();

		// create value objects
		Order order = new Order();
		Customer customer = new Customer();
		Address billing = new Address();
		Address shipping = new Address();
		BillingInfo billingInfo = new BillingInfo();
		ShippingInfo shippingInfo = new ShippingInfo();
		LineItem item = new LineItem();

		// create mock mapper
		@SuppressWarnings("rawtypes")
		FieldSetMapper mapper = mock(FieldSetMapper.class);
		// set how mapper should respond - set return values for mapper
		when(mapper.mapFieldSet(headerFS)).thenReturn(order);
		when(mapper.mapFieldSet(customerFS)).thenReturn(customer);
		when(mapper.mapFieldSet(billingFS)).thenReturn(billing);
		when(mapper.mapFieldSet(shippingFS)).thenReturn(shipping);
		when(mapper.mapFieldSet(billingInfoFS)).thenReturn(billingInfo);
		when(mapper.mapFieldSet(shippingInfoFS)).thenReturn(shippingInfo);
		when(mapper.mapFieldSet(itemFS)).thenReturn(item);

		// set-up provider: set mappers
		provider.setAddressMapper(mapper);
		provider.setBillingMapper(mapper);
		provider.setCustomerMapper(mapper);
		provider.setHeaderMapper(mapper);
		provider.setItemMapper(mapper);
		provider.setShippingMapper(mapper);

		// call tested method
		Object result = provider.read();

		// verify result
		assertNotNull(result);

		// verify whether order is constructed correctly
		// Order object should contain same instances as returned by mapper
		Order o = (Order) result;
		assertEquals(o, order);
		assertEquals(o.getCustomer(), customer);
		// is it non-bussines customer
		assertFalse(o.getCustomer().isBusinessCustomer());
		assertEquals(o.getBillingAddress(), billing);
		assertEquals(o.getShippingAddress(), shipping);
		assertEquals(o.getBilling(), billingInfo);
		assertEquals(o.getShipping(), shippingInfo);
		// there should be 3 line items
		assertEquals(3, o.getLineItems().size());
		for (Iterator<?> i = o.getLineItems().iterator(); i.hasNext();) {
			assertEquals(i.next(), item);
		}

		// try to retrieve next object - nothing should be returned
		assertNull(provider.read());
	}

}
