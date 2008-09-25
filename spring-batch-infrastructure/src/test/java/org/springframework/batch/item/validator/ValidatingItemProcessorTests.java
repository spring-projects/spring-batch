package org.springframework.batch.item.validator;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

/**
 * Tests for {@link ValidatingItemProcessor}.
 */
public class ValidatingItemProcessorTests {

	private Validator validator = createMock(Validator.class);
	
	private ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<String>(validator);
	
	private String item = "item";
	
	@Test
	public void testSuccessfulValidation() throws Exception {
		
		validator.validate(item);
		expectLastCall();
		replay(validator);
		
		assertSame(item, tested.process(item));
		
		verify(validator);
	}
	
	@Test(expected=ValidationException.class)
	public void testFailedValidation() throws Exception {
		
		validator.validate(item);
		expectLastCall().andThrow(new ValidationException("invalid item"));
		replay(validator);
		
		tested.process(item);
	}
}
