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
package org.springframework.batch.sample.domain.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Lucas Ward
 * @author Glenn Renfro
 *
 */
class CompositeCustomerUpdateLineTokenizerTests {

	private StubLineTokenizer customerTokenizer;

	private final FieldSet customerFieldSet = new DefaultFieldSet(null);

	private final FieldSet footerFieldSet = new DefaultFieldSet(null);

	private CompositeCustomerUpdateLineTokenizer compositeTokenizer;

	@BeforeEach
	void init() {
		customerTokenizer = new StubLineTokenizer(customerFieldSet);
		compositeTokenizer = new CompositeCustomerUpdateLineTokenizer();
		compositeTokenizer.setCustomerTokenizer(customerTokenizer);
		compositeTokenizer.setFooterTokenizer(new StubLineTokenizer(footerFieldSet));
	}

	@Test
	void testCustomerAdd() {
		String customerAddLine = "AFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}

	@Test
	void testCustomerDelete() {
		String customerAddLine = "DFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}

	@Test
	void testCustomerUpdate() {
		String customerAddLine = "UFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}

	@Test
	void testInvalidLine() {
		String invalidLine = "INVALID";
		assertThrows(IllegalArgumentException.class, () -> compositeTokenizer.tokenize(invalidLine));
	}

	private static class StubLineTokenizer implements LineTokenizer {

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
