package org.springframework.batch.sample.validation.valang.custom;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.sample.domain.LineItem;

import org.easymock.MockControl;
import org.springmodules.validation.valang.functions.Function;
import junit.framework.TestCase;

public class TotalOrderItemsFunctionTests extends TestCase {

	private TotalOrderItemsFunction function;
	private MockControl argument1Control;
	private Function argument1;
	private MockControl argument2Control;
	private Function argument2;
	
	public void setUp() {
		//create mock for first argument - set count to 3
		argument1Control = MockControl.createControl(Function.class);
		argument1 = (Function) argument1Control.getMock();
		argument1.getResult(null);
		argument1Control.setReturnValue(new Integer(3));
		argument1Control.replay();
		
		argument2Control = MockControl.createControl(Function.class);
		argument2 = (Function) argument2Control.getMock();

		//create function
		function = new TotalOrderItemsFunction(new Function[] {argument1, argument2}, 0, 0);
	}
	
	public void testFunctionWithNonListValue() {
		
		argument2.getResult(null);
		argument2Control.setReturnValue(this);
		argument2Control.replay();
		
		//call tested method - exception is expected because non list value
		try {
			function.doGetResult(null);
			fail("Exception was expected.");
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	
	public void testFunctionWithCorrectItemCount() throws Exception {

		//create list with correct item count
		LineItem item = new LineItem();
		item.setQuantity(3);
		List<LineItem> list = new ArrayList<LineItem>();
		list.add(item);
		
		argument2.getResult(null);
		argument2Control.setReturnValue(list);
		argument2Control.replay();

		//vefify result
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
	}

	public void testFunctionWithIncorrectItemCount() throws Exception {

		//create list with incorrect item count
		LineItem item = new LineItem();
		item.setQuantity(99);
		List<LineItem> list = new ArrayList<LineItem>();
		list.add(item);
		
		argument2.getResult(null);
		argument2Control.setReturnValue(list);
		argument2Control.replay();

		//vefify result
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}

}
