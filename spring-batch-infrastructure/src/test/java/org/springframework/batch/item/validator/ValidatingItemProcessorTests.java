package org.springframework.batch.item.validator;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Tests for {@link ValidatingItemProcessor}.
 */
public class ValidatingItemProcessorTests {

	@SuppressWarnings("unchecked")
	private Validator<String> validator = createMock(Validator.class);

	private static final String ITEM = "item";

	@Test
	public void testSuccessfulValidation() throws Exception {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<String>(validator);

		validator.validate(ITEM);
		expectLastCall();
		replay(validator);

		assertSame(ITEM, tested.process(ITEM));

		verify(validator);
	}

	@Test(expected = ValidationException.class)
	public void testFailedValidation() throws Exception {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<String>(validator);

		processFailedValidation(tested);
	}

	@Test
	public void testFailedValidation_Filter() throws Exception {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<String>(validator);
		tested.setFilter(true);

		assertNull(processFailedValidation(tested));
	}

	private String processFailedValidation(ValidatingItemProcessor<String> tested) {
		validator.validate(ITEM);
		expectLastCall().andThrow(new ValidationException("invalid item"));
		replay(validator);

		return tested.process(ITEM);
	}
}
