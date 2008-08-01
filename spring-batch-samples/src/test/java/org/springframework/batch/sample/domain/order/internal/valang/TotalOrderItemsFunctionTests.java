package org.springframework.batch.sample.domain.order.internal.valang;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.Function;

public class TotalOrderItemsFunctionTests {

	private TotalOrderItemsFunction function;
	private Function argument2;
	
	@Before
	public void setUp() {
		//create mock for first argument - set count to 3
		Function argument1 = createMock(Function.class);
		expect(argument1.getResult(null)).andReturn(3);
		replay(argument1);
		
		argument2 = createMock(Function.class);

		//create function
		function = new TotalOrderItemsFunction(new Function[] {argument1, argument2}, 0, 0);

	}
	
	@Test
	public void testFunctionWithNonListValue() {
		
		expect(argument2.getResult(null)).andReturn(this);
		replay(argument2);
		
		//call tested method - exception is expected because non list value
		try {
			function.doGetResult(null);
			fail("Exception was expected.");
		} catch (Exception e) {
			assertTrue(true);
		}

	}
	
	@Test
	public void testFunctionWithCorrectItemCount() throws Exception {

		//create list with correct item count
		LineItem item = new LineItem();
		item.setQuantity(3);
		List<LineItem> list = new ArrayList<LineItem>();
		list.add(item);
		
		expect(argument2.getResult(null)).andReturn(list);
		replay(argument2);

		//vefify result
		assertTrue((Boolean) function.doGetResult(null));

	}

	@Test
	public void testFunctionWithIncorrectItemCount() throws Exception {

		//create list with incorrect item count
		LineItem item = new LineItem();
		item.setQuantity(99);
		List<LineItem> list = new ArrayList<LineItem>();
		list.add(item);
		
		expect(argument2.getResult(null)).andReturn(list);
		replay(argument2);

		//vefify result
		assertFalse((Boolean) function.doGetResult(null));

	}

}
