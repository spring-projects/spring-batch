package org.springframework.batch.sample.validation.valang.custom;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springmodules.validation.valang.functions.Function;

import java.util.Date;

public class FutureDateFunctionTests {

	private FutureDateFunction function;
	private Function argument;
	
	@Before
	public void setUp() {
		argument = createMock(Function.class);

		//create function
		function = new FutureDateFunction(new Function[] {argument}, 0, 0);

	}
	
	@Test
	public void testFunctionWithNonDateValue() {
		
		//set-up mock argument - set return value to non Date value
		expect(argument.getResult(null)).andReturn(this);
		replay(argument);
				
		//call tested method - exception is expected because non date value
		try {
			function.doGetResult(null);
			fail("Exception was expected.");
		} catch (Exception e) {
			assertTrue(true);
		}

	}
	
	@Test
	public void testFunctionWithFutureDate() throws Exception {

		//set-up mock argument - set return value to future Date
		expect(argument.getResult(null)).andReturn(new Date(Long.MAX_VALUE));
		replay(argument);

		//vefify result - should be true because of future date
		assertTrue((Boolean) function.doGetResult(null));

	}

	@Test
	public void testFunctionWithPastDate() throws Exception {

		//set-up mock argument - set return value to future Date
		expect(argument.getResult(null)).andReturn(new Date(0));
		replay(argument);

		//vefify result - should be false because of past date
		assertFalse((Boolean) function.doGetResult(null));
		
	}

}
