package org.springframework.batch.sample.domain.order.internal.valang;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.Function;

public class ValidateTotalPricesFunctionTests {

	private ValidateTotalPricesFunction function;
	private Function argument;

	@Before
	public void setUp() {

		argument = createMock(Function.class);

		//create function
		function = new ValidateTotalPricesFunction(new Function[] {argument}, 0, 0);

	}
	
	@Test
	public void testTotalPriceMin() throws Exception {
	
		//create line item with correct total price
		LineItem item = new LineItem();
		item.setDiscountAmount(new BigDecimal(0.0));
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setHandlingPrice(new BigDecimal(0.0));
		item.setShippingPrice(new BigDecimal(0.0));
		item.setPrice(new BigDecimal(1.0));
		item.setQuantity(1);
		item.setTotalPrice(new BigDecimal(1.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all total prices are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative item price
		item = new LineItem();
		item.setTotalPrice(new BigDecimal(-1.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid total price
		assertFalse((Boolean) function.doGetResult(null));
	}
	
	@Test
	public void testTotalPriceMax() throws Exception {

		//create line item with correct total price
		LineItem item = new LineItem();
		item.setDiscountAmount(new BigDecimal(0.0));
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setHandlingPrice(new BigDecimal(0.0));
		item.setShippingPrice(new BigDecimal(0.0));
		item.setPrice(new BigDecimal(99999999.0));
		item.setQuantity(1);
		item.setTotalPrice(new BigDecimal(99999999.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all total prices are correct
		assertEquals(true, function.doGetResult(null));
		
		//now add line item with total price above allowed max 
		item = new LineItem();
		item.setTotalPrice(new BigDecimal(100000000.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid total price
		assertFalse((Boolean) function.doGetResult(null));

	}
	
	@Test
	public void testTotalPriceCalculation() throws Exception {
		
		//create line item
		LineItem item = new LineItem();
		item.setDiscountAmount(new BigDecimal(5.0));
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setHandlingPrice(new BigDecimal(1.0));
		item.setShippingPrice(new BigDecimal(2.0));
		item.setPrice(new BigDecimal(250.0));
		item.setQuantity(1);
		item.setTotalPrice(new BigDecimal(248.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all total prices are correct
		assertEquals(true, function.doGetResult(null));		

		//now add line item with incorrect total price 
		item = new LineItem();
		item.setDiscountAmount(new BigDecimal(5.0));
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setHandlingPrice(new BigDecimal(1.0));
		item.setShippingPrice(new BigDecimal(2.0));
		item.setPrice(new BigDecimal(250.0));
		item.setQuantity(1);
		item.setTotalPrice(new BigDecimal(253.0));

		items.add(item);
		
		//verify result - should be false - second item has incorrect total price
		assertFalse((Boolean) function.doGetResult(null));

	}

}
