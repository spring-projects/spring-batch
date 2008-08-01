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

public class ValidateDiscountsFunctionTests {

	private ValidateDiscountsFunction function;
	private Function argument;

	@Before
	public void setUp() {

		argument = createMock(Function.class);

		//create function
		function = new ValidateDiscountsFunction(new Function[] {argument}, 0, 0);

	}
	
	@Test
	public void testDiscountPercentageMin() throws Exception {
	
		//create line item with correct discount percentage and zero discount amount
		LineItem item = new LineItem();
		item.setDiscountPerc(new BigDecimal(1.0));
		item.setDiscountAmount(new BigDecimal(0.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);
		
		//verify result - should be true - all discount percentages are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative percentage
		item = new LineItem();
		item.setDiscountPerc(new BigDecimal(-1.0));
		item.setDiscountAmount(new BigDecimal(0.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid discount percentage
		assertFalse((Boolean) function.doGetResult(null));

	}
	
	@Test
	public void testDiscountPercentageMax() throws Exception {

		//create line item with correct discount percentage and zero discount amount
		LineItem item = new LineItem();
		item.setDiscountPerc(new BigDecimal(99.0));
		item.setDiscountAmount(new BigDecimal(0.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all discount percentages are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with discount percentage above 100
		item = new LineItem();
		item.setDiscountPerc(new BigDecimal(101.0));
		item.setDiscountAmount(new BigDecimal(0.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid discount percentage
		assertFalse((Boolean) function.doGetResult(null));

	}
	
	@Test
	public void testDiscountPriceMin() throws Exception {

		//create line item with correct discount amount and zero discount percentage
		LineItem item = new LineItem();
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setDiscountAmount(new BigDecimal(10.0));
		item.setPrice(new BigDecimal(100.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all discount amounts are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with negative discount amount
		item = new LineItem();
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setDiscountAmount(new BigDecimal(-1.0));
		item.setPrice(new BigDecimal(100.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid discount amount
		assertFalse((Boolean) function.doGetResult(null));

	}
	
	@Test
	public void testDiscountPriceMax() throws Exception {

		//create line item with correct discount amount and zero discount percentage
		LineItem item = new LineItem();
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setDiscountAmount(new BigDecimal(99.0));
		item.setPrice(new BigDecimal(100.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items).times(2);
		replay(argument);

		//verify result - should be true - all discount amounts are correct
		assertTrue((Boolean) function.doGetResult(null));
		
		//now add line item with discount amount above item price
		item = new LineItem();
		item.setDiscountPerc(new BigDecimal(0.0));
		item.setDiscountAmount(new BigDecimal(101.0));
		item.setPrice(new BigDecimal(100.0));
		items.add(item);
		
		//verify result - should be false - second item has invalid discount amount
		assertFalse((Boolean) function.doGetResult(null));

	}
	
	@Test
	public void testBothDiscountValuesNonZero() throws Exception {

		//create line item with non-zero discount amount and non-zero discount percentage
		LineItem item = new LineItem();
		item.setDiscountPerc(new BigDecimal(10.0));
		item.setDiscountAmount(new BigDecimal(99.0));
		
		//add it to line items list 
		List<LineItem> items = new ArrayList<LineItem>();
		items.add(item);
		
		//set return value for mock argument
		expect(argument.getResult(null)).andReturn(items);
		replay(argument);

		//verify result - should be false - only one of the discount values is empty
		assertFalse((Boolean) function.doGetResult(null));

	}

}
