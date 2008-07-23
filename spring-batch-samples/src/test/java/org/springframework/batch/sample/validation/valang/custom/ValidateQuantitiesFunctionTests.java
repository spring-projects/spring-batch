package org.springframework.batch.sample.validation.valang.custom;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.LineItem;
import org.springmodules.validation.valang.functions.Function;

import java.util.ArrayList;
import java.util.List;

public class ValidateQuantitiesFunctionTests {

	private ValidateQuantitiesFunction function;
	private Function argument;

	@Before
	public void setUp() {

		argument = createMock(Function.class);

		//create function
		function = new ValidateQuantitiesFunction(new Function[] {argument}, 0, 0);

	}
	
	@Test
	public void testQuantityMin() throws Exception {
	
		//create line item with correct item quantity
		LineItem item = new LineItem();
		item.setQuantity(1);
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all quantities are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative quantity
		item = new LineItem();
		item.setQuantity(-1);
		items.add(item);
		
		//verify result - should be false - second item has invalid quantity
		assertFalse((Boolean) function.doGetResult(null));
	}
	
	@Test
	public void testQuantityMax() throws Exception {

		//create line item with correct item quantity
		LineItem item = new LineItem();
		item.setQuantity(9999);
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);
		
		//verify result - should be true - all item quantities are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with item quantity above allowed max 
		item = new LineItem();
		item.setQuantity(10000);
		items.add(item);
		
		//verify result - should be false - second item has invalid item quantity
		assertFalse((Boolean) function.doGetResult(null));
	}
}
