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

import static org.easymock.EasyMock.*;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.AbstractItemReader;

/**
 * @author Lucas Ward
 *
 */
public class ValidatingItemReaderTests extends TestCase {

	ItemReader<Object> inputSource;
	ValidatingItemReader<Object> itemProvider;
	Validator validator;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		inputSource = new MockItemReader(this);
		validator = createMock(Validator.class);
		itemProvider = new ValidatingItemReader<Object>();
		itemProvider.setItemReader(inputSource);
		itemProvider.setValidator(validator);
	}

	/*
	 * Super class' afterPropertieSet should be called to
	 * ensure ItemReader is set.
	 */
	public void testItemReaderPropertiesSet(){
		try{
			itemProvider.setItemReader(null);
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

	public void testValidation() throws Exception{

		validator.validate(this);
		expectLastCall().once();
		replay(validator);
		assertEquals(itemProvider.read(), this);
		verify(validator);
	}

	public void testValidationException() throws Exception{

		validator.validate(this);
		expectLastCall().andThrow(new ValidationException(""));
		replay(validator);
		try{
			itemProvider.read();
			fail();
		}catch(ValidationException ex){
			//expected
		}
	}

	public void testNullInput() throws Exception{
		replay(validator);
		itemProvider.setItemReader(new MockItemReader(null));
		assertNull(itemProvider.read());
		//assert validator wasn't called.
		verify(validator);
	}

	private static class MockItemReader extends AbstractItemReader<Object> {

		Object value;

		public MockItemReader(Object value){
			this.value = value;
		}

		public Object read() {
			return value;
		}
	}
}
