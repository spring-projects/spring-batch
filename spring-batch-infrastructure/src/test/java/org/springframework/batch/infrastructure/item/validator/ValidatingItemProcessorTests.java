/*
 * Copyright 2008-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.validator.ValidatingItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.batch.infrastructure.item.validator.Validator;

/**
 * Tests for {@link ValidatingItemProcessor}.
 */
class ValidatingItemProcessorTests {

	@SuppressWarnings("unchecked")
	private final Validator<String> validator = mock();

	private static final String ITEM = "item";

	@Test
	void testSuccessfulValidation() {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<>(validator);

		validator.validate(ITEM);

		assertSame(ITEM, tested.process(ITEM));
	}

	@Test
	void testFailedValidation() {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<>(validator);

		assertThrows(ValidationException.class, () -> processFailedValidation(tested));
	}

	@Test
	void testFailedValidation_Filter() {

		ValidatingItemProcessor<String> tested = new ValidatingItemProcessor<>(validator);
		tested.setFilter(true);

		assertNull(processFailedValidation(tested));
	}

	private String processFailedValidation(ValidatingItemProcessor<String> tested) {
		validator.validate(ITEM);
		when(validator).thenThrow(new ValidationException("invalid item"));

		return tested.process(ITEM);
	}

}
