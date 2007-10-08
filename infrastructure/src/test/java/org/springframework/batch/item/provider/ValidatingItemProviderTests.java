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
package org.springframework.batch.item.provider;

import org.easymock.MockControl;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.exception.ValidationException;
import org.springframework.batch.item.validator.Validator;

import junit.framework.TestCase;

/**
 * @author Lucas Ward
 *
 */
public class ValidatingItemProviderTests extends TestCase {

	InputSource inputSource;
	ValidatingItemProvider itemProvider;
	Validator validator;
	MockControl validatorControl = MockControl.createControl(Validator.class);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		inputSource = new MockInputSource(this);
		validator = (Validator)validatorControl.getMock();
		itemProvider = new ValidatingItemProvider();
		itemProvider.setInputSource(inputSource);
		itemProvider.setValidator(validator);
	}

	/*
	 * Super class' afterPropertieSet should be called to
	 * ensure InputSource is set.
	 */
	public void testInputSourcePropertiesSet(){
		try{
			itemProvider.setInputSource(null);
			itemProvider.afterPropertiesSet();
			fail();
		}catch(Exception ex){
			assertTrue(ex instanceof IllegalArgumentException);
		}
	}

	public void testValidatorPropertesSet(){
		try{
			itemProvider.setValidator(null);
			itemProvider.afterPropertiesSet();
			fail();
		}catch(Exception ex){
			assertTrue(ex instanceof IllegalArgumentException);
		}
	}

	public void testValidation(){

		validator.validate(this);
		validatorControl.replay();
		assertEquals(itemProvider.next(), this);
		validatorControl.verify();
	}

	public void testValidationException(){

		validator.validate(this);
		validatorControl.setThrowable(new ValidationException(""));
		validatorControl.replay();
		try{
			itemProvider.next();
			fail();
		}catch(ValidationException ex){
			//expected
		}
	}

	public void testNullInput(){
		validatorControl.replay();
		itemProvider.setInputSource(new MockInputSource(null));
		assertNull(itemProvider.next());
		//assert validator wasn't called.
		validatorControl.verify();
	}

	private class MockInputSource implements InputSource{

		Object value;

		public MockInputSource(Object value){
			this.value = value;
		}

		public Object read() {
			return value;
		}
	}
}
