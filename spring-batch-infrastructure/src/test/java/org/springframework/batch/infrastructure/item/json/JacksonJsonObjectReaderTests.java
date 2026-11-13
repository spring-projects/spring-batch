/*
 * Copyright 2026 the original author or authors.
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

package org.springframework.batch.infrastructure.item.json;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.batch.infrastructure.item.json.domain.Trade;
import org.springframework.core.io.ByteArrayResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Donghwan Kim
 */
class JacksonJsonObjectReaderTests {

	@Test
	void testReadObjectAfterParseException() throws Exception {
		// given
		String json = """
				[
					{"isin":"AAA","quantity":1,"price":1.0,"customer":"foo"},
					{"isin":"BBB","quantity":"not a number","price":2.0,"customer":"bar"},
					{"isin":"CCC","quantity":3,"price":3.0,"customer":"baz"}
				]
				""";
		JacksonJsonObjectReader<Trade> objectReader = new JacksonJsonObjectReader<>(Trade.class);
		objectReader.open(new ByteArrayResource(json.getBytes()));

		// when
		Trade firstItem = objectReader.read();
		assertThrows(ParseException.class, objectReader::read);
		Trade thirdItem = objectReader.read();

		// then
		assertNotNull(firstItem);
		assertEquals("AAA", firstItem.getIsin());
		assertNotNull(thirdItem);
		assertEquals("CCC", thirdItem.getIsin());

		objectReader.close();
	}

	@Test
	void testReadObjectAfterParseExceptionInObjectWithNestedObject() throws Exception {
		// given
		String json = """
				[
					{"isin":"AAA","quantity":1,"price":1.0,"customer":"foo"},
					{"isin":"BBB","quantity":"not a number","price":2.0,"customer":"bar",
						"details":{"origin":{"country":"FR"}}},
					{"isin":"CCC","quantity":3,"price":3.0,"customer":"baz"}
				]
				""";
		JacksonJsonObjectReader<Trade> objectReader = new JacksonJsonObjectReader<>(Trade.class);
		objectReader.open(new ByteArrayResource(json.getBytes()));

		// when
		Trade firstItem = objectReader.read();
		assertThrows(ParseException.class, objectReader::read);
		Trade thirdItem = objectReader.read();

		// then
		assertNotNull(firstItem);
		assertEquals("AAA", firstItem.getIsin());
		assertNotNull(thirdItem);
		assertEquals("CCC", thirdItem.getIsin());

		objectReader.close();
	}

	@Test
	void testReadAfterParseExceptionInLastObject() throws Exception {
		// given
		String json = """
				[
					{"isin":"AAA","quantity":1,"price":1.0,"customer":"foo"},
					{"isin":"BBB","quantity":"not a number","price":2.0,"customer":"bar"}
				]
				""";
		JacksonJsonObjectReader<Trade> objectReader = new JacksonJsonObjectReader<>(Trade.class);
		objectReader.open(new ByteArrayResource(json.getBytes()));

		// when
		Trade firstItem = objectReader.read();
		assertThrows(ParseException.class, objectReader::read);

		// then
		assertNotNull(firstItem);
		assertEquals("AAA", firstItem.getIsin());
		assertNull(objectReader.read());

		objectReader.close();
	}

}
