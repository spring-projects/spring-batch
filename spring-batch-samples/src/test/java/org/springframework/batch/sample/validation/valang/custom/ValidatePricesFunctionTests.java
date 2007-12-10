package org.springframework.batch.sample.validation.valang.custom;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;

import org.springframework.batch.sample.domain.LineItem;

import org.springmodules.validation.valang.functions.Function;
import junit.framework.TestCase;

public class ValidatePricesFunctionTests extends TestCase {

	private ValidatePricesFunction function;
	private MockControl argumentControl;
	private Function argument;

	public void setUp() {
		argumentControl = MockControl.createControl(Function.class);
		argument = (Function) argumentControl.getMock();

		//create function
		function = new ValidatePricesFunction(new Function[] {argument}, 0, 0);
	}
	
	public void testItemPriceMin() throws Exception {
	
		//create line item with correct item price
		LineItem item = new LineItem();
		item.setPrice(new BigDecimal(1.0));
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all item prices are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with negative item price
		item = new LineItem();
		item.setPrice(new BigDecimal(-1.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid item price
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
	
	public void testItemPriceMax() throws Exception {

		//create line item with correct item price
		LineItem item = new LineItem();
		item.setPrice(new BigDecimal(99999999.0));
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all item prices are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with item price above allowed max 
		item = new LineItem();
		item.setPrice(new BigDecimal(100000000.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid item price
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}

}
