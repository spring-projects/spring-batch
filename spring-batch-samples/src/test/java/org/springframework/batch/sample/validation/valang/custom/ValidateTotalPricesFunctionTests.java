package org.springframework.batch.sample.validation.valang.custom;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.easymock.MockControl;

import org.springframework.batch.sample.domain.LineItem;

import org.springmodules.validation.valang.functions.Function;
import junit.framework.TestCase;

public class ValidateTotalPricesFunctionTests extends TestCase {

	private ValidateTotalPricesFunction function;
	private MockControl argumentControl;
	private Function argument;

	public void setUp() {
		argumentControl = MockControl.createControl(Function.class);
		argument = (Function) argumentControl.getMock();

		//create function
		function = new ValidateTotalPricesFunction(new Function[] {argument}, 0, 0);
	}
	
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
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all total prices are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with negative item price
		item = new LineItem();
		item.setTotalPrice(new BigDecimal(-1.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid total price
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
	
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
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all total prices are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());
		
		//now add line item with total price above allowed max 
		item = new LineItem();
		item.setTotalPrice(new BigDecimal(100000000.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid total price
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
	
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
		argument.getResult(null);
		argumentControl.setReturnValue(items,2);
		argumentControl.replay();
		
		//verify result - should be true - all total prices are correct
		assertTrue(((Boolean)function.doGetResult(null)).booleanValue());		

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
		assertFalse(((Boolean)function.doGetResult(null)).booleanValue());
	}
}
