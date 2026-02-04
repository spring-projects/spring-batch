/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.batch.core.converter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Stefano Cordio
 */
class DateFormatterTests {

	private final DateFormatter underTest = new DateFormatter();

	@Test
	void testParse() {
		// given
		String date = "1970-01-01T00:00:00Z";

		// when
		Date converted = underTest.parse(date, Locale.getDefault());

		// then
		assertEquals(Date.from(Instant.EPOCH), converted);
	}

	@Test
	void testPrint() {
		// given
		Date date = Date.from(Instant.EPOCH);

		// when
		String converted = underTest.print(date, Locale.getDefault());

		// then
		assertEquals("1970-01-01T00:00:00Z", converted);
	}

}
