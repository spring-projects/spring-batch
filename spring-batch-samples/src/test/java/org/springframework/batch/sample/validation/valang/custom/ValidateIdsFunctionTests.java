package org.springframework.batch.sample.validation.valang.custom;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.batch.sample.domain.LineItem;

import org.springmodules.validation.valang.functions.Function;

public class ValidateIdsFunctionTests extends TestCase {

	private ValidateIdsFunction function;
	private MockControl argumentControl;
	private Function argument;

	public void setUp() {
		argumentControl = MockControl.createControl(Function.class);
		argument = (Function) argumentControl.getMock();

		//create function
		function = new ValidateIdsFunction(new Function[] {argument}, 0, 0);
	}
	
	public void testIdMin() throws Exception {
	
		//create line item with correct item id
		LineItem item = new LineItem();
		item.setItemId(1);
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all ids are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with negative id
		item = new LineItem();
		item.setItemId(-1);
		items.add(item);
		
		//verify result - should be false - second item has invalid id
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
	
	public void testIdMax() throws Exception {

		//create line item with correct item id
		LineItem item = new LineItem();
		item.setItemId(9999999999L);
		
		//add it to line items list 
		List items = new ArrayList();
		items.add(item);
		
		//set return value for mock argument
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all item ids are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with item id above allowed max 
		item = new LineItem();
		item.setItemId(10000000000L);
		items.add(item);
		
		//verify result - should be false - second item has invalid item id
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
}
