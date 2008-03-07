/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.validator;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.ValidationException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class SpringValidatorTests extends TestCase {
	private SpringValidator validator = new SpringValidator();

	private Validator mockValidator;

	protected void setUp() throws Exception {
		mockValidator = new MockSpringValidator();
		validator.setValidator(mockValidator);
	}

	/**
	 * Validator property is not set
	 */
	public void testValidateNullValidator() {
		validator.setValidator(null);

		try {
			validator.validate(MockSpringValidator.ACCEPT_VALUE);
			fail("must not validate with null validator");
		}
		catch (ValidationException expected) {
			assertTrue(true);
		}
	}

	/**
	 * Validator does not know how to validate object of the given class
	 */
	public void testValidateUnsupportedType() {
		try {
			validator.validate(new Integer(1)); // only strings are supported
			fail("must not validate unsupported classes");
		}
		catch (ValidationException expected) {
			assertTrue(true);
		}
	}

	/**
	 * Typical successful validation
	 */
	public void testValidateSuccessfully() {
		try {
			validator.validate(MockSpringValidator.ACCEPT_VALUE);
			assertTrue(true);
		}
		catch (ValidationException unexpected) {
			throw unexpected;
		}
	}

	/**
	 * Typical failed validation
	 */
	public void testValidateFailure() {
		try {
			validator.validate(MockSpringValidator.REJECT_VALUE);
			fail("exception should have been thrown on invalid value");
		}
		catch (ValidationException expected) {
			assertTrue(true);
		}
	}
	
	/**
	 * Typical failed validation
	 */
	public void testValidateFailureWithFields() {
		try {
			validator.validate(MockSpringValidator.REJECT_MULTI_VALUE);
			fail("exception should have been thrown on invalid value");
		}
		catch (ValidationException expected) {
			assertTrue("Wonrg message: "+expected.getMessage(), expected.getMessage().indexOf("foo, bar")>=0);
		}
	}
	
	static class MockSpringValidator implements Validator {
		public static final TestBean ACCEPT_VALUE = new TestBean();

		public static final TestBean REJECT_VALUE = new TestBean();

		public static final TestBean REJECT_MULTI_VALUE = new TestBean("foo", "bar");

		public boolean supports(Class clazz) {
			return clazz.isAssignableFrom(TestBean.class);
		}

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
	
	static class TestBean {
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
		
	}
}
