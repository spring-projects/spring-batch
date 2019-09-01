/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.sample.domain.trade;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.lang.Nullable;

/**
 * @author Lucas Ward
 *
 */
public class CompositeCustomerUpdateLineTokenizerTests {
	private StubLineTokenizer customerTokenizer;
	private FieldSet customerFieldSet = new DefaultFieldSet(null);
	private FieldSet footerFieldSet = new DefaultFieldSet(null);
	private CompositeCustomerUpdateLineTokenizer compositeTokenizer;
	
	@Before
	public void init(){
		customerTokenizer = new StubLineTokenizer(customerFieldSet);
		compositeTokenizer = new CompositeCustomerUpdateLineTokenizer();
		compositeTokenizer.setCustomerTokenizer(customerTokenizer);
		compositeTokenizer.setFooterTokenizer(new StubLineTokenizer(footerFieldSet));
	}
	
	@Test
	public void testCustomerAdd() throws Exception{
		String customerAddLine = "AFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test
	public void testCustomerDelete() throws Exception{
		String customerAddLine = "DFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test
	public void testCustomerUpdate() throws Exception{
		String customerAddLine = "UFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidLine() throws Exception{
		String invalidLine = "INVALID";
		compositeTokenizer.tokenize(invalidLine);
	}

	private static class StubLineTokenizer implements LineTokenizer{
		private final FieldSet fieldSetToReturn;
		private String tokenizedLine;
		
		public StubLineTokenizer(FieldSet fieldSetToReturn) {
			this.fieldSetToReturn = fieldSetToReturn;
		}

		@Override
		public FieldSet tokenize(@Nullable String line) {
			this.tokenizedLine = line;
			return fieldSetToReturn;
		}
		
		public String getTokenizedLine() {
			return tokenizedLine;
		}
	}
}
