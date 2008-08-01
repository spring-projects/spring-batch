package org.springframework.batch.sample.domain.order.internal.valang;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.Function;

public class ValidateShippingPricesFunctionTests {

	private ValidateShippingPricesFunction function;
	private Function argument;

	@Before
	public void setUp() {

		argument = createMock(Function.class);

		//create function
		function = new ValidateShippingPricesFunction(new Function[] {argument}, 0, 0);

	}
	
	@Test
	public void testShippingPriceMin() throws Exception {
	
		//create line item with correct shipping price
		LineItem item = new LineItem();
		item.setShippingPrice(new BigDecimal(1.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all shipping prices are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative shipping price
		item = new LineItem();
		item.setShippingPrice(new BigDecimal(-1.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid shipping price
		assertFalse((Boolean) function.doGetResult(null));
		
	}
	
	@Test
	public void testShippingPriceMax() throws Exception {

		//create line item with correct shipping price
		LineItem item = new LineItem();
		item.setShippingPrice(new BigDecimal(99999999.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all shipping prices are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with shipping price above allowed max 
		item = new LineItem();
		item.setShippingPrice(new BigDecimal(100000000.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid shipping price
		assertFalse((Boolean) function.doGetResult(null));

	}
}
