package org.springframework.batch.item.validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Tests for {@link ValidatingItemProcessor}.
 */
public class ValidatingItemProcessorTests {

	@SuppressWarnings("unchecked")
	private Validator<String> validator = mock(Validator.class);

	private static final String ITEM = "item";

	@Test
	public void testSuccessfulValidation() throws Exception {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<String>(validator);

		validator.validate(ITEM);

		assertSame(ITEM, tested.process(ITEM));
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
		when(validator).thenThrow(new ValidationException("invalid item"));

		return tested.process(ITEM);
	}
}
