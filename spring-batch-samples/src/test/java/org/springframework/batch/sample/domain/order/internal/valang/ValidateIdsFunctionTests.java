package org.springframework.batch.sample.domain.order.internal.valang;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.Function;

public class ValidateIdsFunctionTests {

	private ValidateIdsFunction function;
	private Function argument;

	@Before
	public void setUp() {
		argument = createMock(Function.class);

		//create function
		function = new ValidateIdsFunction(new Function[] {argument}, 0, 0);
	}
	
	@Test
	public void testIdMin() throws Exception {
	
		//create line item with correct item id
		LineItem item = new LineItem();
		item.setItemId(1);
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);
		
		//verify result - should be true - all ids are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative id
		item = new LineItem();
		item.setItemId(-1);
		items.add(item);
		
		//verify result - should be false - second item has invalid id
		assertFalse((Boolean) function.doGetResult(null));
		
	}
	
	@Test
	public void testIdMax() throws Exception {

		//create line item with correct item id
		LineItem item = new LineItem();
		item.setItemId(9999999999L);
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);
		
		//verify result - should be true - all item ids are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with item id above allowed max 
		item = new LineItem();
		item.setItemId(10000000000L);
		items.add(item);
		
		//verify result - should be false - second item has invalid item id
		assertFalse((Boolean) function.doGetResult(null));

	}

}
