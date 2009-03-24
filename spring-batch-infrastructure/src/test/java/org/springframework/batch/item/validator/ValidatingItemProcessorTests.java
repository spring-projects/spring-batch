package org.springframework.batch.item.validator;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Tests for {@link ValidatingItemProcessor}.
 */
public class ValidatingItemProcessorTests {

	@SuppressWarnings("unchecked")
	private Validator<String> validator = createMock(Validator.class);
	
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
