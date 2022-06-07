/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Tests for {@link SpringValidator}.
 */
public class SpringValidatorTests {

	private SpringValidator<Object> validator = new SpringValidator<>();

	private Validator mockValidator;

	@BeforeEach
	public void setUp() throws Exception {
		mockValidator = new MockSpringValidator();
		validator.setValidator(mockValidator);
	}

	/**
	 * Validator property is not set
	 */
	@Test
	public void testNullValidator() {
		validator.setValidator(null);
		assertThrows(IllegalArgumentException.class, validator::afterPropertiesSet);
	}

	/**
	 * Validator does not know how to validate object of the given class
	 */
	@Test
	public void testValidateUnsupportedType() {
		assertThrows(ValidationException.class, () -> validator.validate(1));
		// only strings are supported
	}

	/**
	 * Typical successful validation - no exception is thrown.
	 */
	@Test
	public void testValidateSuccessfully() {
		validator.validate(MockSpringValidator.ACCEPT_VALUE);
	}

	/**
	 * Typical failed validation - {@link ValidationException} is thrown
	 */
	@Test
	public void testValidateFailure() {
		assertThrows(ValidationException.class, () -> validator.validate(MockSpringValidator.REJECT_VALUE));
	}

	/**
	 * Typical failed validation - {@link ValidationException} is thrown
	 */
	@Test
	public void testValidateFailureWithErrors() {
		ValidationException e = assertThrows(ValidationException.class,
				() -> validator.validate(MockSpringValidator.REJECT_VALUE));
		assertTrue(e.getCause() instanceof BindException);
	}

	/**
	 * Typical failed validation - message contains the item and names of invalid fields.
	 */
	@Test
	public void testValidateFailureWithFields() {
		try {
			validator.validate(MockSpringValidator.REJECT_MULTI_VALUE);
			fail("exception should have been thrown on invalid value");
		}
		catch (ValidationException expected) {
			assertTrue(expected.getMessage().contains("TestBeanToString"),
					"message should contain the item#toString() value");
			assertTrue(expected.getMessage().contains("foo"), "message should contain names of the invalid fields");
			assertTrue(expected.getMessage().contains("bar"), "message should contain names of the invalid fields");
		}
	}

	private static class MockSpringValidator implements Validator {

		public static final TestBean ACCEPT_VALUE = new TestBean();

		public static final TestBean REJECT_VALUE = new TestBean();

		public static final TestBean REJECT_MULTI_VALUE = new TestBean("foo", "bar");

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.isAssignableFrom(TestBean.class);
		}

		@Override
		public void validate(Object value, Errors errors) {
			if (value.equals(ACCEPT_VALUE)) {
				return; // return without adding errors
			}

			if (value.equals(REJECT_VALUE)) {
				errors.reject("bad.value");
				return;
			}
			if (value.equals(REJECT_MULTI_VALUE)) {
				errors.rejectValue("foo", "bad.value");
				errors.rejectValue("bar", "bad.value");
				return;
			}
		}

	}

	@SuppressWarnings("unused")
	private static class TestBean {

		private String foo;

		private String bar;

		public String getFoo() {
			return foo;
		}

		public String getBar() {
			return bar;
		}

		public TestBean() {
			super();
		}

		public TestBean(String foo, String bar) {
			this();
			this.foo = foo;
			this.bar = bar;
		}

		@Override
		public String toString() {
			return "TestBeanToString";
		}

	}

}
