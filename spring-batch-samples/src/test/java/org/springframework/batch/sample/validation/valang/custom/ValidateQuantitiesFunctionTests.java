package org.springframework.batch.sample.validation.valang.custom;

import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;

import org.springframework.batch.sample.domain.LineItem;

import org.springmodules.validation.valang.functions.Function;
import junit.framework.TestCase;

public class ValidateQuantitiesFunctionTests extends TestCase {

	private ValidateQuantitiesFunction function;
	private MockControl argumentControl;
	private Function argument;

	public void setUp() {
		argumentControl = MockControl.createControl(Function.class);
		argument = (Function) argumentControl.getMock();

		//create function
		function = new ValidateQuantitiesFunction(new Function[] {argument}, 0, 0);
	}
	
	public void testQuantityMin() throws Exception {
	
		//create line item with correct item quantity
		LineItem item = new LineItem();
		item.setQuantity(1);
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all quantities are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with negative quantity
		item = new LineItem();
		item.setQuantity(-1);
		items.add(item);
		
		//verify result - should be false - second item has invalid quantity
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
	
	public void testQuantityMax() throws Exception {

		//create line item with correct item quantity
		LineItem item = new LineItem();
		item.setQuantity(9999);
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all item quantities are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with item quantity above allowed max 
		item = new LineItem();
		item.setQuantity(10000);
		items.add(item);
		
		//verify result - should be false - second item has invalid item quantity
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
}
