/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.infrastructure.item.validator;

import jakarta.validation.constraints.NotEmpty;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class BeanValidatingItemProcessorTests {

	@Test
	void testValidObjectValidation() throws Exception {
		// given
		BeanValidatingItemProcessor<Foo> validatingItemProcessor = new BeanValidatingItemProcessor<>();
		validatingItemProcessor.afterPropertiesSet();
		Foo foo = new Foo("foo");

		// when
		Foo processed = validatingItemProcessor.process(foo);

		// then
		assertNotNull(processed);
	}

	@Test
	void testInvalidObjectValidation() throws Exception {
		// given
		BeanValidatingItemProcessor<Foo> validatingItemProcessor = new BeanValidatingItemProcessor<>();
		validatingItemProcessor.afterPropertiesSet();
		Foo foo = new Foo("");

		// when/then
		assertThrows(ValidationException.class, () -> validatingItemProcessor.process(foo));
	}

	private static class Foo {

		@NotEmpty
		private String name;

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
